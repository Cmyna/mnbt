package mnbt.experiment

import net.myna.mnbt.IdTagCompound
import net.myna.mnbt.IdTagEnd
import net.myna.mnbt.Tag
import net.myna.mnbt.TagIdPayload
import net.myna.mnbt.codec.*
import net.myna.mnbt.tag.AnyCompound
import net.myna.mnbt.tag.CompoundTag
import java.util.ArrayList

class CompoundTagCodec(override var proxy: Codec<Any>):
        OnByteFlatCodec<AnyCompound>(IdTagCompound, Collection::class.java as Class<AnyCompound>), HierarchicalCodec<AnyCompound> {

    override fun createTag(name: String?, value: AnyCompound): Tag<AnyCompound> {
        return CompoundTag(name, value)
    }

    override fun encodeValue(value: AnyCompound, intent: CodecCallerIntent): CodecFeedback {
        intent as EncodeToBytes
        val proxyIntent = toProxyIntent(true, intent)
        val bitsCache = ArrayList<ByteArray>()
        var bitsLen = TagIdPayload // payload for TagEnd
        value.onEach {
            checkNotNull(it.value.name) // name should not be null
            val feedback = proxy.encode(it.value, proxyIntent) as EncodedBytesFeedback
            bitsCache.add(feedback.bytes)
            bitsLen += feedback.bytes.size
        }
        val bits = ByteArray(bitsLen)
        var pointer = 0
        bitsCache.onEach {
            System.arraycopy(it, 0, bits, pointer, it.size)
            pointer += it.size
        }
        bits[pointer] = IdTagEnd
        return object: EncodedBytesFeedback {
            override val bytes: ByteArray = bits
        }
    }

    override fun decodeToValue(intent: CodecCallerIntent): AnyCompound {
        intent as DecodeFromBytes
        var subTagId = intent.data[intent.pointer]
        val compound = mutableMapOf<String, Tag<out Any>>()
        while (subTagId != IdTagEnd) {
            val proxyIntent = proxyDecodeFromBytesIntent(true, subTagId, intent)
            val feedback = proxy.decode(proxyIntent)
            compound[feedback.tag.name!!] = feedback.tag
            subTagId = intent.data[intent.pointer]
        }
        intent.pointer += TagIdPayload // skip tag End
        return compound
    }
}