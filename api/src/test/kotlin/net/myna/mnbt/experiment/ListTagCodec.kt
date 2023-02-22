package net.myna.mnbt.experiment

import net.myna.mnbt.*
import net.myna.mnbt.codec.*
import net.myna.mnbt.codec.binary.*
import net.myna.mnbt.tag.AnyTagList
import net.myna.mnbt.tag.ListTag
import net.myna.mnbt.utils.CodecIntentExentions.getByte
import net.myna.mnbt.utils.CodecIntentExentions.getInt
import net.myna.utils.Extensions.toBytes
import java.lang.IllegalArgumentException
import java.lang.NullPointerException
import java.util.ArrayList

class ListTagCodec(override var proxy: Codec<Any>): HierarchicalCodec<AnyTagList> {

    override val id: Byte = IdTagList
    override val valueTypeToken = MutableList::class.java as Class<AnyTagList>

    override fun encode(tag: Tag<out AnyTagList>, intent: EncodeIntent): CodecFeedback {
        intent as EncodeHead
        val hasHead = intent.encodeHead
        if (tag !is ListTag<*>) throw IllegalArgumentException("List Tag Codec can only handle tag type that is ListTag, but ${tag::class.java} is passed")
        val name = if (hasHead) tag.name?: throw NullPointerException("want serialize tag with tag head, but name was null!") else null
        var bitsLen = if (hasHead) TagIdPayload else 0
        val nameBits = name?.toByteArray(Charsets.UTF_8)
        val nameBitsLen = nameBits?.let {nameBits.size}
        nameBitsLen?.let {bitsLen += it+ ShortSizePayload } // add tag.name payload if has head
        bitsLen += TagIdPayload + ArraySizePayload // add element id payload and element list size payload
        val proxyIntent = toProxyIntent(false, intent)
        val bitsCache = ArrayList<ByteArray>()
        for (tags in tag.value) {
            val feedback = proxy.encode(tags, proxyIntent) as EncodedBytesFeedback
            bitsCache.add(feedback.bytes)
            bitsLen += feedback.bytes.size
        }
        val bits = ByteArray(bitsLen)
        var pointer = 0
        if (hasHead) {
            bits[0] = id // set id
            pointer += TagIdPayload
        }

        nameBitsLen?.let { // set name len if has head
            System.arraycopy(it.toShort().toBytes(), 0, bits, pointer, StringSizePayload)
            pointer += StringSizePayload
        }
        nameBits?.let { // set name if has head
            System.arraycopy(it, 0, bits, pointer, nameBitsLen!!)
            pointer += nameBitsLen
        }

        // set element id
        bits[pointer] = tag.elementId
        pointer += TagIdPayload

        // set element size
        System.arraycopy(tag.value.size.toBytes(), 0, bits, pointer, ArraySizePayload)
        pointer += ArraySizePayload

        // copy each element data
        bitsCache.onEach {
            System.arraycopy(it, 0, bits, pointer, it.size)
            pointer += it.size
        }

        return object: EncodedBytesFeedback {
            override val bytes: ByteArray = bits
        }
    }

    override fun decode(intent: DecodeIntent): TagFeedback<AnyTagList> {
        intent as DecodeFromBytes
        // read tag head if intent wants
        val name = TagHeadDecoder.decodeHead(id, intent)
        // read element id
        val elementId = intent.getByte()
        // read list size
        val size = intent.getInt()
        val nbtlist = ListTag<Tag<out Any>>(elementId, name)
        val proxyIntent = proxyDecodeFromBytesIntent(false, elementId, intent)
        for (i in 0 until size) {
            val feedback = proxy.decode(proxyIntent)
            nbtlist.add(feedback.tag)
        }
        return object: TagFeedback<AnyTagList> {
            override val tag: Tag<AnyTagList> = nbtlist
        }
    }


}