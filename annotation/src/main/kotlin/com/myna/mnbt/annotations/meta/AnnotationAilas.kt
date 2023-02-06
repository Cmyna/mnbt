package com.myna.mnbt.annotations.meta

import kotlin.reflect.KClass


@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class AnnotationAlias<S:Any,T:Annotation>(
        val target: KClass<out T>,
        val annotationWrapperClass: KClass<out AnnotationWrapper<S, T>>) {

    interface AnnotationWrapper<in S:Any, out T:Annotation> {
        fun tryWrap(annotationInstance: S):T?
    }

    companion object {

        /**
         * @return aliasTarget instance from current annotation instance passed in
         */
        fun <T:Annotation> toAliasTarget(annotationInstance:Annotation, aliasTarget: KClass<T>):T? {
            // we can not get real annotation class from annotation::class.java (it will get Proxy class),
            // use annotation.annotationClass.java instead
            val aliasArr = annotationInstance.annotationClass.java.getAnnotationsByType(AnnotationAlias::class.java)
                    ?: return null
            aliasArr as Array<AnnotationAlias<*,*>>
            val annotationAlias = aliasArr.find { it.target == aliasTarget }
            annotationAlias as AnnotationAlias<*, T>
            val annotationWrapper = annotationAlias.annotationWrapperClass.constructors.first().call() as AnnotationWrapper<Any, T>
            return annotationWrapper.tryWrap(annotationInstance)
        }
    }

}



