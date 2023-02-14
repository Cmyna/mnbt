package mnbt.utils

import com.myna.mnbt.Tag
import com.myna.mnbt.codec.*

class MockCodecProxy(var functionalProxy:Codec<Any>): Codec<Any> {
    override val id: Byte
        get() = 0
    override val valueTypeToken: Class<Any>
        get() = Any::class.java

    override fun encode(tag: Tag<out Any>, intent: EncodeIntent): CodecFeedback {
        return functionalProxy.encode(tag, intent)
    }

    override fun decode(intent: DecodeIntent): TagFeedback<Any> {
        return functionalProxy.decode(intent)
    }

}