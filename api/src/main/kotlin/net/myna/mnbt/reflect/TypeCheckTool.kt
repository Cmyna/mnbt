package net.myna.mnbt.reflect

import net.myna.mnbt.reflect.MTypeToken
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Modifier
import java.lang.reflect.Type

object TypeCheckTool {

    /**
     * wrap primitive type to Boxed type (eg. int -> Integer)
     */
    private fun wrap(type:Type):Type {
        return when(type) {
            Int::class.java -> java.lang.Integer::class.java
            Short::class.java -> java.lang.Short::class.java
            Byte::class.java -> java.lang.Byte::class.java
            Long::class.java -> java.lang.Long::class.java
            Float::class.java -> java.lang.Float::class.java
            Double::class.java -> java.lang.Double::class.java
            Boolean::class.java -> java.lang.Boolean::class.java
            else -> type
        }
    }

    /**
     * check can cast source class to target class or not
     * if two classes are primitive class, it will try to convert to related wrapper class
     */
    fun isCastable(source: MTypeToken<*>, target: MTypeToken<*>):Boolean {
        val wrappedSource = source.wrap()
        val wrappedTarget = target.wrap()
        // a shortcut prevent using TypeToken.subTypeOf when input TypeToken has been a rawType
        // because TypeToken.subTypeOf is a heavy function that takes a lot time
        // same as prevent using TypeToken.getRawType function
        val targetType = wrappedTarget.type
        val sourceType = wrappedSource.type
        val actualTarget = if (targetType is Class<*>) targetType else wrappedTarget.rawType
        val actualSource = if (sourceType is Class<*>) sourceType else wrappedSource.rawType
        return (actualTarget.isAssignableFrom(actualSource))
    }

    fun isCastable(source:Class<*>, target:Class<*>):Boolean {
        val wrappedSource = wrap(source) as Class<*>
        val wrappedTarget = wrap(target) as Class<*>
        return (wrappedTarget.isAssignableFrom(wrappedSource))
    }

    /**
     * check type is array or not
     */
    fun isArray(type:Type):Boolean {
        if (type is Class<*>) return type.isArray // if it is actual class
        else return type is GenericArrayType // if it is generic type
    }

    /**
     * get array type component, if it is not array, return null
     */
    fun getArrayTypeComponent(type:Type):Type? {
        if (!isArray(type)) return null
        if (type is Class<*>) {
            return type.componentType
        } else if (type is GenericArrayType) {
            return type.genericComponentType
        }
        return null // unsupported type, return null
    }

    fun isInterfaceOrAbstract(clazz: Class<*>):Boolean {
        if (clazz.isInterface) return true
        if (clazz.isArray) {
            return isInterfaceOrAbstract(clazz.componentType)
        }
        if (Modifier.isAbstract(clazz.modifiers)) return true
        return false
    }

}