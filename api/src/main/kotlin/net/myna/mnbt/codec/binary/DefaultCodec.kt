package net.myna.mnbt.codec.binary

import net.myna.mnbt.Tag
import net.myna.mnbt.codec.*
import net.myna.mnbt.utils.CodecIntentExentions.decodeHead
import java.lang.NullPointerException

/**
 * Default Codec implement usual part
 */
abstract class DefaultCodec<NbtRelatedType:Any>(override val id: Byte, override val valueTypeToken: Class<NbtRelatedType>)
    : Codec<NbtRelatedType> {

    abstract fun encodeValue(value:NbtRelatedType, intent: EncodeOnStream): CodecFeedback
    abstract fun decodeToValue(intent: DecodeOnStream):NbtRelatedType
    abstract fun createTag(name:String?, value:NbtRelatedType): Tag<NbtRelatedType>

    override fun encode(tag: Tag<out NbtRelatedType>, intent: EncodeIntent): CodecFeedback {
        intent as EncodeOnStream; intent as EncodeHead
        val hasHead = intent.encodeHead
        val name = if (hasHead) tag.name?: throw NullPointerException("want serialize tag with tag head, but name was null!") else null
        if (hasHead) CodecTool.writeTagHead(id, name!!, intent.outputStream)
        encodeValue(tag.value, intent)
        return object: OutputStreamFeedback {
            override val outputStream = intent.outputStream
        }
    }

    override fun decode(intent: DecodeIntent): TagFeedback<NbtRelatedType> {
        intent as DecodeOnStream
        val name = if (intent is DecodeHead) intent.decodeHead(id) else null
        return object: TagFeedback<NbtRelatedType> {
            override val tag: Tag<NbtRelatedType> = createTag(name, decodeToValue(intent))
        }
    }
}