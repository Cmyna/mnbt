package net.myna.mnbt.utils

import net.myna.mnbt.Tag
import net.myna.mnbt.converter.*
import net.myna.mnbt.reflect.MTypeToken

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

    fun applyWhenCreateTag(injectCodeName:String, injectFun:(name:String?, value:Any, typeToken: MTypeToken<out Any>, intent: CreateTagIntent)->Unit) {
        createMockTagSupplier[injectCodeName] = { _,_,_,_->
            CreateTagMockFeedback(false, null)
        }
    }

}

typealias MockTagSupplier
        = (name: String?, value: Any, typeToken: MTypeToken<out Any>,
         intent: CreateTagIntent)-> MockConverterProxy.CreateTagMockFeedback