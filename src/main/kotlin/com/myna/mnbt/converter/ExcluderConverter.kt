package com.myna.mnbt.converter

import com.myna.mnbt.Tag
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.NullTag

/**
 * this class is for filter some value in, it will be higher priority than ReflectiveTypeConverter.
 *
 * It will filter some value that is from local/anonymous class
 */
class ExcluderConverter: TagConverter<Nothing, ConverterCallerIntent> {
    override fun defaultIntent(): ConverterCallerIntent {
        return converterCallerIntent()
    }

    override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: ConverterCallerIntent): Tag<Nothing>? {
        // if it is local/anonymous class, return nullTag
        if (value==null || value::class.java.isAnonymousClass || value::class.java.isLocalClass) return NullTag(null, null) as Tag<Nothing>
        return null
    }

    override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ConverterCallerIntent): Pair<String?, V>? {
        return null
    }

    companion object {
        @JvmStatic
        val instance = ExcluderConverter()
    }


}