package com.myna.mnbt.annotations

import java.lang.Exception
import java.lang.RuntimeException
import java.lang.reflect.Field

/**
 * an abstract class that can provide default value for a field in Tag->Object conversion.
 * it is used with annotation [Ignore], when we try to convert a compound tag to an java object.
 * The converter will ignore read sub tag with name mapped to field, and use value from this provider instead.
 *
 * it is suggested to let class implement this interface has empty constructor, so the annotation handler can create it safely
 *
 * [provide] function should not rely any runtime context, or [StateException] may be threw from caller
 */
interface FieldValueProvider {
    /**
     * provide a default value for a field
     * @param field: the field type containing the value
     */
    fun provide(field: Field):Any?

    /**
     * for catching any Exceptions from FieldValueProvider.provide(), and throws this exception out
     */
    class StateException(exception: Exception):RuntimeException() {
        override val cause: Throwable = exception
        override val message: String = "Exception throws when invoking FieldValueProvider.provide() method"
    }

    companion object {

        /**
         * try provide value by field. Capturing any Exception and throw as [StateException]
         * @throws [StateException] if any Exceptions caught in method
         *
         */
        fun FieldValueProvider.tryProvide(field: Field):Any? {
            return try {
                this.provide(field)
            } catch (exception:Exception) {
                throw StateException(exception)
            }
        }
    }

}