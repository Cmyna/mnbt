package net.myna.mnbt.tag

import net.myna.mnbt.AnyTag
import net.myna.mnbt.IdTagCompound
import net.myna.mnbt.Tag
import net.myna.mnbt.utils.SnbtTools
import java.util.*

typealias AnyCompound = MutableMap<String, Tag<out Any>>
typealias UnknownCompound = MutableMap<*,*>

@Suppress("EqualsOrHashCode")
class CompoundTag(
        override val name: String?,
        override val value: AnyCompound,
        override val id: Byte = IdTagCompound
        ): Tag.NestTag<AnyCompound>() {

    constructor(name:String?=null):this(name, mutableMapOf())

    @Suppress("UNCHECKED_CAST")
    override fun equals(other: Any?): Boolean {
        if (!isTagAndEqName(this, other)) return false
        other as Tag<AnyCompound>
        //if (value.size != other.value.size) return false
        return value == other.value
    }

    fun add(value: Tag<out Any>):Tag<out Any>? {
        return this.value.put(value.name!!, value)
    }

    operator fun get(name: String):Tag<out Any>? {
        return this.value[name]
    }

    override fun valueToString(parents: Deque<Tag<*>>): String {
        val cirRef = parents.any { this === it }
        if (cirRef) {
            return "CircularReference: ${this::class.java} @${this.hashCode()}"
        }
        parents.push(this)
        val hasNestTag = value.any { it.value is NestTag }
        val builder = StringBuilder("{")
        value.entries.forEachIndexed { i, e ->
            if (hasNestTag) {
                builder.append("\n")
                repeat(parents.size) {builder.append("\t")}
            }
            elementToString(builder, e.value, parents)
            if (i<value.size-1) {
                builder.append(", ")
            }
        }
        parents.pop()
        if (hasNestTag) {
            builder.append("\n")
            repeat(parents.size) {builder.append("\t")}
        }
        builder.append("}")
        return builder.toString()
    }

    private fun elementToString(builder: StringBuilder, element:AnyTag, parents: Deque<Tag<*>>) {
        val realName = element.name ?: "null"
        builder.append("\"${SnbtTools.escape(realName)}\": ")

        builder.append(element.valueToString(parents))
    }

    override fun toString(): String {
        val deque: Deque<Tag<*>> = ArrayDeque()
        return if (this.name==null) valueToString(deque)
        else "{\"${SnbtTools.escape(this.name)}\":${valueToString(deque)}}"
    }

    @Suppress("UNCHECKED_CAST")
    override fun <V> getElementByPath(pathSegment: String): Tag<out V>? {
        val res = value[pathSegment]
        return if (res != null) res as Tag<out V> else null
    }
}
