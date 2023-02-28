package net.myna.mnbt.annotations

import kotlin.reflect.KClass

/**
 * this annotation is used for declaring an actual field class type
 * if field is declared as an interface
 * The annotation will be used when convert a Tag to a specific object's field value.
 *
 * The class parameter should be an implementation of field type interface, or Exception[TODO] will be thrown
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class InstanceAs(val instanceClass: KClass<out Any>)
