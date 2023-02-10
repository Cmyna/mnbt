package com.myna.mnbt.tag

import com.myna.mnbt.IdTagCompound
import com.myna.mnbt.Tag

typealias AnyCompound = MutableMap<String, Tag<out Any>>

class CompoundTag(
        override val name: String?,
        override val value: AnyCompound,
        override val id: Byte = IdTagCompound
        ): Tag.NestTag<AnyCompound>() {

    constructor(name:String?=null):this(name, mutableMapOf())

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

    override fun <V> getElementByPath(pathSegment: String): Tag<out V>? {
        val res = value[pathSegment]
        return if (res != null) res as Tag<out V> else null
    }
}
