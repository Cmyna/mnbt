package com.myna.mnbt.tag

import com.myna.mnbt.IdTagCompound
import com.myna.mnbt.Tag

typealias AnyCompound = MutableMap<String, Tag<out Any>>

class CompoundTag(
        override val name: String?,
        override val value: AnyCompound,
        override val id: Byte = IdTagCompound
        ): Tag<AnyCompound>() {

    constructor(name:String?=null):this(name, mutableMapOf())

    override fun equals(other: Any?): Boolean {
        if (!PrimitiveTag.isTagAndEqName(this, other)) return false
        other as Tag<AnyCompound>
        //if (value.size != other.value.size) return false
        return value == other.value
    }

    fun add(value: Tag<out Any>):Tag<out Any>? {
        return this.value.put(value.name!!, value)
    }
}
