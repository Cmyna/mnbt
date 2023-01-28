package com.myna.mnbt.experiment

import com.myna.mnbt.Tag
import com.myna.mnbt.codec.*
import com.myna.mnbt.core.CodecTool
import java.lang.NullPointerException

abstract class OnByteFlatCodec<NbtRelatedType:Any>(override val id: Byte, override val valueTypeToken: Class<NbtRelatedType>)
    : Codec<NbtRelatedType> {

    abstract fun encodeValue(value:NbtRelatedType, intent: CodecCallerIntent): CodecFeedback
    abstract fun decodeToValue(intent: CodecCallerIntent):NbtRelatedType
    abstract fun createTag(name:String?, value:NbtRelatedType): Tag<NbtRelatedType>

    override fun encode(tag: Tag<out NbtRelatedType>, intent: CodecCallerIntent): CodecFeedback {
        intent as EncodeHead
        val hasHead = intent.encodeHead
        val name = if (hasHead) tag.name?: throw NullPointerException("want serialize tag with tag head, but name was null!") else null
        val feedback = encodeValue(tag.value, intent) as EncodedBytesFeedback
        val bits = if (name!=null) CodecTool.getTagBits(tag.id, tag.name!!, feedback.bytes) else feedback.bytes
        return object: EncodedBytesFeedback {
            override val bytes: ByteArray = bits
        }
    }

    override fun decode(intent: CodecCallerIntent): TagFeedback<NbtRelatedType> {
        val name = TagHeadDecoder.decodeHead(id, intent)
        return object: TagFeedback<NbtRelatedType> {
            override val tag: Tag<NbtRelatedType> = createTag(name, decodeToValue(intent))
        }
    }
}