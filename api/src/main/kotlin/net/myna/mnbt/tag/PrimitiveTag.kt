package net.myna.mnbt.tag

import net.myna.mnbt.*
import net.myna.mnbt.Tag
import net.myna.mnbt.Tag.Companion.isTagAndEqName

object PrimitiveTag {

    fun primitiveEqFun(tag: Tag<*>, other:Any?):Boolean {
        if (!isTagAndEqName(tag, other)) return false
        other as Tag<*>
        return primitiveTagValueEqFun(tag.value, other.value)
    }

    fun primitiveTagValueEqFun(tv: Any?, other: Any?):Boolean {
        return tv==other
    }

    fun primitiveHashCode(tag: Tag<*>):Int {
        return tag.name.hashCode()+tag.value.hashCode()
    }

    class IntTag(override val name: String?,override val value: Int) : Tag<Int>() {
        override val id: Byte = IdTagInt
        override fun equals(other: Any?): Boolean = primitiveEqFun(this, other)
        override fun hashCode(): Int = primitiveHashCode(this)
    }
    class ByteTag(override val name: String?,override val value: Byte) : Tag<Byte>() {
        override val id: Byte = IdTagByte
        override fun equals(other: Any?): Boolean = primitiveEqFun(this, other)
        override fun hashCode(): Int = primitiveHashCode(this)
    }
    class ShortTag(override val name: String?,override val value: Short) : Tag<Short>() {
        override val id: Byte = IdTagShort
        override fun equals(other: Any?): Boolean = primitiveEqFun(this, other)
        override fun hashCode(): Int = primitiveHashCode(this)
    }
    class LongTag(override val name: String?,override val value: Long) : Tag<Long>() {
        override val id: Byte = IdTagLong
        override fun equals(other: Any?): Boolean = primitiveEqFun(this, other)
        override fun hashCode(): Int = primitiveHashCode(this)
    }
    class FloatTag(override val name: String?, override val value: Float) : Tag<Float>() {
        override val id: Byte = IdTagFloat
        override fun equals(other: Any?): Boolean = primitiveEqFun(this, other)
        override fun hashCode(): Int = primitiveHashCode(this)
    }
    class DoubleTag(override val name: String?, override val value: Double) : Tag<Double>() {
        override val id: Byte = IdTagDouble
        override fun equals(other: Any?): Boolean = primitiveEqFun(this, other)
        override fun hashCode(): Int = primitiveHashCode(this)
    }
    class StringTag(override val name: String?,override val value: String) : Tag<String>() {
        override val id: Byte = IdTagString
        override fun equals(other: Any?): Boolean = primitiveEqFun(this, other)
        override fun hashCode(): Int = primitiveHashCode(this)
    }


}