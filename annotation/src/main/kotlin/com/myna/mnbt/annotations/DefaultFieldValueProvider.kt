package com.myna.mnbt.annotations

/**
 * an abstract class that can provide default value for a field in Tag->Object conversion.
 * it is used with annotation [Ignore], when we try to convert a compound tag to an java object.
 * The converter will ignore read sub tag with name mapped to field, and use value from this provider instead.
 *
 * it is suggested to let class implement this interface has empty constructor, so the annotation handler can create it safely
 *
 * [provide] function should not rely any runtime context, or [Exception] may be threw
 */
// TODO: change document [Exception] to a specific Exception
interface DefaultFieldValueProvider<V> {
    /**
     * provide a default value for a field
     */
    fun provide():V
}