package com.myna.mnbt.converter

import com.myna.mnbt.Tag
import com.myna.mnbt.reflect.MTypeToken
import java.util.*

// NbtRelatedType: a type directly related to an actual Nbt bin value type
// eg. Tag_Int value type related to Int, Tag_IntArray value type related to IntArray
/**
 * a interface that declare a converter to specific tag
 *
 * generic type T: class extends Tag
 *
 * generic type V: another value type want to convert to Tag
 */
// to fix Tag generic type problem, the caller no necessary to know what actual generic type of created Tag it is
// they just need to know one Converter accept one kind of sub-class of Tag
interface TagConverter<out NbtRelatedType:Any, I: ConverterCallerIntent> {
    // I had considered add isCompatible function to check value can be used to TagConverter or not
    // but it causes duplicate code that creatTag and toValue also need check,
    // so I decided to change return type nullable, which means it will return null if value not compat
    // TODO: createTag parameter typeToken seems useless?
    fun defaultIntent():I
    fun <V:Any> createTag(name: String?, value:V, typeToken: MTypeToken<out V>, intent:I = defaultIntent()): Tag<out NbtRelatedType>?
    fun <V:Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent:I = defaultIntent()): Pair<String?, V>?
}


/**
 * the Converter used to handle java object with hierarchical structure
 */
abstract class HierarchicalTagConverter<NbtRelatedType:Any>: TagConverter<NbtRelatedType, ConverterCallerIntent> {
    // Hierarchical Tag Converter is the one handle Tag with hierarchical structure
    // because of this, it acts like a delegator and may need other Converters' help to handle element tags in top level tag passed in
    // hence, it needs a proxy, which is also regard as an HierarchicalTagConverter too (has same functionality)
    // in general, the delegator should not care about what actual type token in, just let proxy handle it
    abstract var proxy: TagConverter<Any, ConverterCallerIntent>
    override fun defaultIntent(): ConverterCallerIntent {
        return converterCallerIntent(false)
    }
}

