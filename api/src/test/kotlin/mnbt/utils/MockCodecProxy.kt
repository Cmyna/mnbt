package mnbt.utils

import com.myna.mnbt.Tag
import com.myna.mnbt.codec.Codec
import com.myna.mnbt.codec.CodecCallerIntent
import com.myna.mnbt.codec.CodecFeedback
import com.myna.mnbt.codec.TagFeedback

class MockCodecProxy(var functionalProxy:Codec<Any>): Codec<Any> {
    override val id: Byte
        get() = 0
    override val valueTypeToken: Class<Any>
        get() = Any::class.java

    override fun encode(tag: Tag<out Any>, intent: CodecCallerIntent): CodecFeedback {
        return functionalProxy.encode(tag, intent)
    }

    override fun decode(intent: CodecCallerIntent): TagFeedback<Any> {
        return functionalProxy.decode(intent)
    }

}