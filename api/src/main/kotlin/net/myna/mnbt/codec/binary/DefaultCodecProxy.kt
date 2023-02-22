package net.myna.mnbt.codec.binary

import net.myna.mnbt.IdTagEnd
import net.myna.mnbt.Tag
import net.myna.mnbt.codec.*
import net.myna.mnbt.defaultTreeDepthLimit
import net.myna.mnbt.exceptions.*
import net.myna.mnbt.tag.NullTag
import net.myna.mnbt.utils.CodecIntentExentions.tryGetId
import java.lang.reflect.Proxy
import java.util.*
import kotlin.reflect.jvm.javaGetter

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
        for (codec in codecInstances) codecMap[codec.id] = codec
    }

    constructor(vararg codecInstances: Codec<out Any>):this() {
        for (codec in codecInstances) {
            codecMap[codec.id] = codec
        }
    }

    /**
     * @throws CircularReferenceException if circular reference found
     */
    override fun encode(tag: Tag<out Any>, intent: EncodeIntent): CodecFeedback {
        intent as OnStreamToDelegatorEncodeIntent
        val parents = intent.parents
        // check null tag
        val tag2 = tag as Tag<*>
        if (tag2 is NullTag || tag2.value==null) return codecMap[IdTagEnd]!!.encode(tag as Tag<Nothing>, intent)
        val codec = codecMap[tag.id]?: throw NullPointerException("can not find Codec be tag id:${tag.id} with value ${tag.value}).")

        // if parents.size reach tree depth limit (consider the tag passed in, data structure will over tree depth limit)
        // throw Exception
        if (parents.size >= defaultTreeDepthLimit) throw MaxNbtTreeDepthException(parents.size)
        // circular check
        val foundCirRef = parents.any { tag === it }
        if (foundCirRef) throw CircularReferenceException(tag)
        // need to push tagValue in stack before pass to delegate, then pop it
        parents.push(tag)
        // run time cast
        val castedCodec = codec as Codec<Any>
        val feedback = castedCodec.encode(tag, intent)
        parents.pop()
        return feedback
    }

    override fun decode(intent: DecodeIntent): TagFeedback<Any> {
        intent as DecodeOnStream; intent as DecodeTreeDepth
        var intentId = if (intent is SpecifyIdWhenDecoding) intent.id else null

        intent.depth += 1 // increase depth
        // check decode tree depth
        if (intent.depth >= defaultTreeDepthLimit) {
            throw MaxNbtTreeDepthException(intent.depth+1, "tree depth limit reach when decode binary data;")
        }


        if (intent !is DecodeHead && intentId==null) throw IllegalArgumentException("specify no tag head, but no tag id passed in!")
        var ignoreId = intent is SpecifyIdWhenDecoding
        if (intentId == null) {
            intentId = intent.tryGetId()
            if (intentId == invalidFlag) throw NullPointerException("Input Stream has reached the end, while decode is still in progress!")
            ignoreId = true
        }
        // find Codec by id
        val codec = codecMap[intentId]?: throw IllegalArgumentException("can not find Codec with related id $intentId")
        codec as Codec<Any> // runtime cast

        // decoding
        val newIntent = Proxy.newProxyInstance(
                intent::class.java.classLoader,
                intent::class.java.interfaces
        ) { _, method, args->
            when (method) {
                DecodeHead::ignoreIdWhenDecoding.javaGetter -> return@newProxyInstance ignoreId
                else -> return@newProxyInstance method.invoke(intent, *args.orEmpty())
            }
        } as DecodeIntent
        val result = codec.decode(newIntent)

        intent.depth -= 1
        return result
    }

}