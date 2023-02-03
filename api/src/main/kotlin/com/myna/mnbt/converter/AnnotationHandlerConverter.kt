package com.myna.mnbt.converter

import com.myna.mnbt.Tag
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.utils.addStartAtIntent

/**
 * a Converter handle Annotation specified in typeToken
 */
class AnnotationHandlerConverter(override var proxy: TagConverter<Any, ConverterCallerIntent>)
    :HierarchicalTagConverter<Any>() {

    override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: ConverterCallerIntent): Tag<out Any>? {
        return proxy.createTag(name, value, typeToken, intent)
    }

    override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ConverterCallerIntent): Pair<String?, V>? {
        intent as RecordParents; intent as ToValueIntent
        val mapToAnn = typeToken.rawType.getAnnotation(com.myna.mnbt.annotations.LinkTo::class.java)
//        if (mapToAnn!=null) {
//            val startedTag = TagSearcher.findTag(tag, mapToAnn.path, mapToAnn.typeId) ?: throw ConversionException(
//                    "Can not find matched Tag with name:${mapToAnn.path} and value type:${mapToAnn.typeId}"
//            )
//            return proxy.toValue(startedTag, typeToken, intent)
//        }
//        return null

        if (intent !is StartAt && mapToAnn!=null) {
            val actualIntent = intent.addStartAtIntent(mapToAnn.path, mapToAnn.typeId)
            return proxy.toValue(tag, typeToken, actualIntent)
        }
        return null
    }

}