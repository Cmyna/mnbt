package com.myna.mnbt.converter

import com.myna.mnbt.Tag
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.NullTag

/**
 * this class is for filter some value in, it will be higher priority than ReflectiveTypeConverter.
 *
 * It will filter some value that is from local/anonymous class
 */
class ExcluderConverter: TagConverter<Unit> {
    override fun defaultToValueIntent(): ToValueIntent {
        return converterCallerIntent()
    }

    override fun defaultCreateTagIntent(): CreateTagIntent {
        return createTagUserIntent()
    }

    override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: CreateTagIntent): Tag<Unit>? {
        // if it is local/anonymous class, return nullTag
        if (value::class.java.isAnonymousClass || value::class.java.isLocalClass) return NullTag()
        return null
    }

    override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ToValueIntent): Pair<String?, V>? {
        return null
    }

    companion object {
        @JvmStatic
        val instance = ExcluderConverter()
    }


}