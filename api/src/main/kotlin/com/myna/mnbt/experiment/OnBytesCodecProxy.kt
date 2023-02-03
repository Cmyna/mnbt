package com.myna.mnbt.experiment

import com.myna.mnbt.IdTagEnd
import com.myna.mnbt.Tag
import com.myna.mnbt.codec.*
import com.myna.mnbt.defaultTreeDepthLimit
import com.myna.mnbt.exceptions.CircularReferenceException
import com.myna.mnbt.exceptions.MaxNbtTreeDepthException
import com.myna.mnbt.tag.NullTag
import java.util.HashMap

class OnBytesCodecProxy:Codec<Any>  {

    private val invalidFlag:Byte = -1

    override val id: Byte = invalidFlag
    override val valueTypeToken = Any::class.java
    private val codecMap:MutableMap<Byte, Codec<out Any>> = HashMap<Byte, Codec<out Any>>()



    constructor(codecInstances:List<Codec<Any>>):this() {
        for (Codec in codecInstances) codecMap[Codec.id] = Codec
    }

    constructor(vararg Codeces: Codec<out Any>) {
        for (codec in Codeces) {
            codecMap[codec.id] = codec
        }
    }

    override fun encode(tag: Tag<out Any>, intent: CodecCallerIntent): CodecFeedback {
        intent as OnBytesToProxyEncodeIntent
        val parents = intent.parents
        // check null tag
        val tag2 = tag as Tag<*>
        if (tag2 is NullTag || tag2.value is Nothing || tag2.value==null) return codecMap[IdTagEnd]!!.encode(tag as Tag<Nothing>, intent)
        val Codec = codecMap[tag.id]?: throw NullPointerException("can not find Codec be tag id:${tag.id} with value ${tag.value}).")
        // do stack depth checking
        // why maximumTreeDepth-1? Ans: because the known depth here is parents number + 1(all parents appears above and the current passed tagValue)
        //      so we check (parents number+1)> maximumTreeDepth
        if (parents.size > defaultTreeDepthLimit - 1) throw MaxNbtTreeDepthException(parents.size)
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
        intent as DecodeFromBytes; intent as RecordParentsWhenEncoding
        val parents = intent.parents
        var intentId = if (intent is SpecifyIdWhenDecoding) intent.id else null
        if (intent !is DecodeHead && intentId==null) throw IllegalArgumentException("specify no tag head, but no tag id passed in!")
        // if id == invalidId(-1), try find id by input bits data
        if (intentId == null) {
            intentId = intent.data[intent.pointer]
            if (intentId == invalidFlag) throw NullPointerException("Input Stream has reached the end, while deserialization is keep going!")
        }
        // find Codec by id
        val Codec = codecMap[intentId]?: throw IllegalArgumentException("can not find Codec with related id $intentId")
        Codec as Codec<Any> // runtime cast
        // deserialization

        val newIntent = proxyDecodeFromBytesIntent(intent is DecodeHead, parents, intentId, intent)
        return Codec.decode(newIntent)
    }

    fun registerCodec(codec: Codec<out Any>):Boolean {
        this.codecMap[codec.id] = codec
        return true
    }

}
