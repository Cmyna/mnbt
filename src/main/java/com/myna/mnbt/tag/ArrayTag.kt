package com.myna.mnbt.tag

import com.myna.mnbt.IdTagByteArray
import com.myna.mnbt.IdTagIntArray
import com.myna.mnbt.IdTagLongArray
import com.myna.mnbt.Tag

object ArrayTag {

    fun reflectiveArrayTagEq(tag: Tag<*>, other: Any?):Boolean {
        if(!PrimitiveTag.isTagAndEqName(tag, other)) return false
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

    class IntArrayTag(override val name: String?, override val value: IntArray, override val id: Byte = IdTagIntArray) : Tag<IntArray>() {
        override fun equals(other: Any?): Boolean = reflectiveArrayTagEq(this, other)
    }
    class ByteArrayTag(override val name: String?, override val value: ByteArray, override val id: Byte = IdTagByteArray) : Tag<ByteArray>() {
        override fun equals(other: Any?): Boolean = reflectiveArrayTagEq(this, other)
    }
    class LongArrayTag(override val name: String?, override val value: LongArray, override val id: Byte = IdTagLongArray) : Tag<LongArray>() {
        override fun equals(other: Any?): Boolean = reflectiveArrayTagEq(this, other)
    }
}