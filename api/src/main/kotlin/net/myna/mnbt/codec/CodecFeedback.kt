package net.myna.mnbt.codec

import com.google.common.annotations.Beta
import net.myna.mnbt.Tag
import java.io.OutputStream

@Beta
interface CodecFeedback

@Beta
/**
 * an interface used to contain decode result (a tag)
 */
interface TagFeedback<NbtRelatedType> {
    val tag: Tag<NbtRelatedType>
}

@Beta
interface OutputStreamFeedback: CodecFeedback {
    val outputStream:OutputStream
}

@Beta
interface EncodedBytesFeedback: CodecFeedback {
    val bytes:ByteArray
}
