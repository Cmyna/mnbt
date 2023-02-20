package net.myna.mnbt.codec

import net.myna.mnbt.Tag
import java.io.OutputStream

interface CodecFeedback

/**
 * an interface used to contain decode result (a tag)
 */
interface TagFeedback<NbtRelatedType> {
    val tag: Tag<NbtRelatedType>
}

interface OutputStreamFeedback:CodecFeedback{
    val outputStream:OutputStream
}

interface EncodedBytesFeedback:CodecFeedback{
    val bytes:ByteArray
}
