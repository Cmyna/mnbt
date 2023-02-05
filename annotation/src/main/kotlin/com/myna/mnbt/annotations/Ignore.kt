package com.myna.mnbt.annotations

import com.myna.mnbt.annotations.meta.AnnotationAlias
import com.myna.mnbt.annotations.meta.AnnotationWrapper
import kotlin.reflect.KClass

/**
 * specify ignore a field when convert to/from tag
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@AnnotationAlias<Ignore, IgnoreEncode>(IgnoreEncode::class, Ignore.EncodeWrapper::class)
@AnnotationAlias<Ignore, IgnoreDecode>(IgnoreDecode::class, Ignore.DecodeWrapper::class)
annotation class Ignore(
        val ignoreWhenEncode:Boolean,
        val ignoreWhenDecode:Boolean,
        val defaultFieldValueProvider: KClass<out DefaultFieldValueProvider<*>>) {

    class EncodeWrapper: AnnotationWrapper<Ignore, IgnoreEncode> {
        override fun tryWrap(annotationInstance: Ignore):IgnoreEncode? {
            return if (annotationInstance.ignoreWhenEncode) IgnoreEncode()
            else null
        }
    }

    class DecodeWrapper: AnnotationWrapper<Ignore, IgnoreDecode> {
        override fun tryWrap(annotationInstance: Ignore):IgnoreDecode? {
            return if (annotationInstance.ignoreWhenDecode) IgnoreDecode(annotationInstance.defaultFieldValueProvider)
            else null
        }
    }
}


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class IgnoreEncode

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class IgnoreDecode(val defaultFieldValueProvider: KClass<out DefaultFieldValueProvider<*>>)

