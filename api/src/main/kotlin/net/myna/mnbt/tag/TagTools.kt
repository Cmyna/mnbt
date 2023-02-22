package net.myna.mnbt.tag

import net.myna.mnbt.Tag

object TagTools {

    fun primitiveEqFun(tag: Tag<*>, other:Any?):Boolean {
        if (!Tag.isTagAndEqName(tag, other)) return false
        other as Tag<*>
        return tag.value == other.value
    }

    fun primitiveHashCode(tag: Tag<*>):Int {
        return tag.name.hashCode()+tag.value.hashCode()
    }

    fun reflectiveArrayTagEq(tag: Tag<*>, other: Any?):Boolean {
        if(!Tag.isTagAndEqName(tag, other)) return false
        other as Tag<*>
        return reflectArrayTagValueEq(tag.value, other.value)
    }

    fun reflectArrayTagValueEq(tv: Any?, other: Any?):Boolean {
        if (tv==null && other==null) return true
        if (tv==null || other==null) return false
        if (!tv::class.java.isArray) throw IllegalArgumentException("input value type ${tv::class.java} is not Array!")
        if (!other::class.java.isArray) return false
        if (tv::class.java.componentType!=other::class.java.componentType) return false
        // compare size
        val l1 = java.lang.reflect.Array.getLength(tv)
        val l2 = java.lang.reflect.Array.getLength(other)
        if (l1 != l2) return false
        // compare each element in array
        for (i in IntRange(0,l1-1)) {
            val v1 = java.lang.reflect.Array.get(tv, i)
            val v2 = java.lang.reflect.Array.get(other, i)
            if (v1 != v2) return false
        }
        return true
    }
}