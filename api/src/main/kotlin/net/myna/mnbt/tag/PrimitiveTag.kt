package net.myna.mnbt.tag

import net.myna.mnbt.*
import net.myna.mnbt.Tag
import net.myna.mnbt.utils.SnbtTools.escape
import java.util.*

class IntTag(override val name: String?,override val value: Int) : Tag<Int>() {
    override val id: Byte = IdTagInt
    override fun equals(other: Any?): Boolean = TagTools.primitiveEqFun(this, other)
    override fun hashCode(): Int = TagTools.primitiveHashCode(this)
    override fun valueToString(parents: Deque<Tag<*>>): String {
        return value.toString()
    }
}
class ByteTag(override val name: String?,override val value: Byte) : Tag<Byte>() {
    override val id: Byte = IdTagByte
    override fun equals(other: Any?): Boolean = TagTools.primitiveEqFun(this, other)
    override fun hashCode(): Int = TagTools.primitiveHashCode(this)
    override fun valueToString(parents: Deque<Tag<*>>): String {
        return value.toString()
    }
}
class ShortTag(override val name: String?,override val value: Short) : Tag<Short>() {
    override val id: Byte = IdTagShort
    override fun equals(other: Any?): Boolean = TagTools.primitiveEqFun(this, other)
    override fun hashCode(): Int = TagTools.primitiveHashCode(this)
    override fun valueToString(parents: Deque<Tag<*>>): String {
        return value.toString()
    }
}
class LongTag(override val name: String?,override val value: Long) : Tag<Long>() {
    override val id: Byte = IdTagLong
    override fun equals(other: Any?): Boolean = TagTools.primitiveEqFun(this, other)
    override fun hashCode(): Int = TagTools.primitiveHashCode(this)
    override fun valueToString(parents: Deque<Tag<*>>): String {
        return value.toString()
    }
}
class FloatTag(override val name: String?, override val value: Float) : Tag<Float>() {
    override val id: Byte = IdTagFloat
    override fun equals(other: Any?): Boolean = TagTools.primitiveEqFun(this, other)
    override fun hashCode(): Int = TagTools.primitiveHashCode(this)
    override fun valueToString(parents: Deque<Tag<*>>): String {
        return value.toString()
    }
}
class DoubleTag(override val name: String?, override val value: Double) : Tag<Double>() {
    override val id: Byte = IdTagDouble
    override fun equals(other: Any?): Boolean = TagTools.primitiveEqFun(this, other)
    override fun hashCode(): Int = TagTools.primitiveHashCode(this)
    override fun valueToString(parents: Deque<Tag<*>>): String {
        return value.toString()
    }
}
class StringTag(override val name: String?,override val value: String) : Tag<String>() {
    override val id: Byte = IdTagString
    override fun equals(other: Any?): Boolean = TagTools.primitiveEqFun(this, other)
    override fun hashCode(): Int = TagTools.primitiveHashCode(this)
    override fun valueToString(parents: Deque<Tag<*>>): String {
        return "\"${escape(value)}\""
    }
}