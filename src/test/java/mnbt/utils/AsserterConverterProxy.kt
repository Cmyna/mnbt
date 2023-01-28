package mnbt.utils

import com.myna.mnbt.Tag
import com.myna.mnbt.converter.ConverterCallerIntent
import com.myna.mnbt.converter.HierarchicalTagConverter
import com.myna.mnbt.converter.TagConverter
import com.myna.mnbt.reflect.MTypeToken

@Suppress("UnstableApiUsage")
/**
 * a mock proxy that accept and check delegator passed parameters
 *
 * main object for this proxy is check parameter passed from other converters
 */
class AsserterConverterProxy(realProxy:TagConverter<Any, ConverterCallerIntent>):HierarchicalTagConverter<Any>() {

    override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: ConverterCallerIntent): Tag<out Any>? {
        return proxy.createTag(name, value, typeToken, intent)
    }

    override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ConverterCallerIntent): Pair<String?, V>? {
        return proxy.toValue(tag, typeToken, intent)
    }

    override var proxy: TagConverter<Any, ConverterCallerIntent> = realProxy

}