package com.myna.mnbt.codec

import com.myna.mnbt.IdTagEnd
import com.myna.mnbt.Tag
import com.myna.mnbt.defaultTreeDepthLimit
import com.myna.mnbt.exceptions.*
import com.myna.mnbt.tag.NullTag
import com.myna.mnbt.utils.CodecIntentExentions.tryGetId
import java.io.*
import java.util.*

class DefaultCodecProxy(): Codec<Any> {

    private val invalidFlag:Byte = -1

    override val id: Byte = invalidFlag
    override val valueTypeToken = Any::class.java
    private val codecMap:MutableMap<Byte, Codec<out Any>> = HashMap<Byte, Codec<out Any>>()

    fun registerCodec(codec: Codec<out Any>):Boolean {
        this.codecMap[codec.id] = codec
        return true
    }

    constructor(codecInstances:List<Codec<Any>>):this() {
        for (Codec in codecInstances) codecMap[Codec.id] = Codec
    }

    constructor(vararg codecInstances:Codec<out Any>):this() {
        for (Codec in codecInstances) {
            codecMap[Codec.id] = Codec
        }
    }

    override fun encode(tag: Tag<out Any>, intent: CodecCallerIntent):CodecFeedback {
        intent as OnStreamToDelegatorEncodeIntent
        val parents = intent.parents
        // check null tag
        val tag2 = tag as Tag<*>
        if (tag2 is NullTag || tag2.value is Nothing || tag2.value==null) return codecMap[IdTagEnd]!!.encode(tag as Tag<Nothing>, intent)
        val Codec = codecMap[tag.id]?: throw NullPointerException("can not find Codec be tag id:${tag.id} with value ${tag.value}).")

        // if parents.size reach tree depth limit (consider the tag passed in, data structure will over tree depth limit)
        // throw Exception
        if (parents.size >= defaultTreeDepthLimit) throw MaxNbtTreeDepthException(parents.size)
        // circular check
        // TODO: may can change ways to handle circular reference, like return empty ByteArray when found circular ref
        parents.onEach { if (tag === it) throw CircularReferenceException(tag) }
        // need to push tagValue in stack before pass to delegate, then pop it
        parents.push(tag)
        // run time cast
        val castedCodec = Codec as Codec<Any>
        val feedback = castedCodec.encode(tag, intent)
        parents.pop()
        return feedback
    }

    override fun decode(intent: CodecCallerIntent): TagFeedback<Any> {
        intent as DecodeOnStream; intent as DecodeHead
        val parents = (intent as CodecRecordParents).parents
        var intentId = if (intent is SpecifyIdWhenDecoding) intent.id else null

        if (!intent.decodeHead && intentId==null) throw IllegalArgumentException("specify no tag head, but no tag id passed in!")
        var ignoreId = intent is SpecifyIdWhenDecoding
        if (intentId == null) {
            intentId = intent.tryGetId()
            if (intentId == invalidFlag) throw NullPointerException("Input Stream has reached the end, while deserialization is keep going!")
            ignoreId = true
        }
        // find Codec by id
        val codec = codecMap[intentId]?: throw IllegalArgumentException("can not find Codec with related id $intentId")
        codec as Codec<Any> // runtime cast
        // decoding
        val newIntent = object: CodecRecordParents,DecodeOnStream,DecodeHead {
            override val inputStream: InputStream = (intent as DecodeOnStream).inputStream
            override val parents = parents
            override val decodeHead: Boolean = intent.decodeHead
            override val ignoreIdWhenDecoding: Boolean = ignoreId
        }
        return codec.decode(newIntent)
    }

}