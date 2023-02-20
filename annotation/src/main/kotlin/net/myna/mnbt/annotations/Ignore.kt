package net.myna.mnbt.annotations

import meta.AnnotationAlias
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.jvm.isAccessible

/**
 * specify ignore a field when convert to/from tag
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@AnnotationAlias<Ignore, IgnoreToTag>(IgnoreToTag::class, Ignore.EncodeWrapper::class)
@AnnotationAlias<Ignore, IgnoreFromTag>(IgnoreFromTag::class, Ignore.DecodeWrapper::class)
annotation class Ignore(
        val ignoreToTag:Boolean,
        val ignoreFromTag:Boolean,
        val fieldValueProvider: KClass<out FieldValueProvider>) {

    class EncodeWrapper: AnnotationAlias.AnnotationWrapper<Ignore, IgnoreToTag> {
        override fun tryWrap(annotationInstance: Ignore): IgnoreToTag? {
            return if (annotationInstance.ignoreToTag) IgnoreToTag()
            else null
        }
    }

    class DecodeWrapper: AnnotationAlias.AnnotationWrapper<Ignore, IgnoreFromTag> {
        override fun tryWrap(annotationInstance: Ignore): IgnoreFromTag? {
            return if (annotationInstance.ignoreFromTag) IgnoreFromTag(annotationInstance.fieldValueProvider)
            else null
        }
    }

    /**
     * only provide null value
     */
    class NullProvider: FieldValueProvider {
        override fun provide(field: Field): Any? = null
    }

    companion object {
        fun ignoreToTag(field:Field):Boolean {
            val ignoreAnn = field.getAnnotation(Ignore::class.java)
            val ignoreToTag = field.getAnnotation(IgnoreToTag::class.java)?: ignoreAnn?.let {AnnotationAlias.toAliasTarget(it, IgnoreToTag::class)}
            return ignoreToTag != null
        }

        fun tryGetIgnoreFromTag(field: Field): IgnoreFromTag? {
            val ignoreAnn = field.getAnnotation(Ignore::class.java)
            return field.getAnnotation(IgnoreFromTag::class.java)
                    ?: ignoreAnn?.let { AnnotationAlias.toAliasTarget(it, IgnoreFromTag::class) }
        }
    }
}


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class IgnoreToTag

// TODO: is it possible to simplify provider in IgnoreDecode?

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class IgnoreFromTag(val fieldValueProvider: KClass<out FieldValueProvider>) {

    companion object {
        fun tryProvide(providerClass: KClass<out FieldValueProvider>, field: Field):Any? {
            val constructor = providerClass.constructors.first()
            constructor.isAccessible = true
            val providerInstance = constructor.call()
            return providerInstance.provide(field)
        }
    }
}

