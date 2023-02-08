package mnbt.utils

import com.myna.mnbt.Tag
import com.myna.mnbt.converter.*
import com.myna.mnbt.reflect.MTypeToken

@Suppress("UnstableApiUsage")
/**
 * a mock proxy that can inject code in createTag/toValue functions
 */
class MockConverterProxy(realProxy:TagConverter<Any>):HierarchicalTagConverter<Any>() {

    data class CreateTagMockFeedback(val asReturn:Boolean, val result:Tag<out Any>?)

    val createMockTagSupplier:MutableMap<String, MockTagSupplier> = HashMap()

    override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: CreateTagIntent): Tag<out Any>? {
        createMockTagSupplier.onEach {
            val feedback = it.value(name, value, typeToken, intent)
            if (feedback.asReturn) return feedback.result
        }
        return proxy.createTag(name, value, typeToken, intent)
    }

    override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ToValueIntent): Pair<String?, V>? {
        return proxy.toValue(tag, typeToken, intent)
    }

    override var proxy: TagConverter<Any> = realProxy

}

typealias MockTagSupplier
        = (name: String?, value: Any, typeToken: MTypeToken<out Any>,
         intent: ConverterCallerIntent)-> MockConverterProxy.CreateTagMockFeedback