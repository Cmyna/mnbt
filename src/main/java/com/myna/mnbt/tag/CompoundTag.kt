package com.myna.mnbt.tag

import com.myna.mnbt.IdTagCompound
import com.myna.mnbt.Tag

typealias AnyCompound = MutableCollection<Tag<out Any>>

class CompoundTag(
        override val name: String?,
        override val value: AnyCompound,
        override val id: Byte = IdTagCompound
        ): Tag<AnyCompound>() {

    constructor(name:String?=null):this(name, mutableSetOf())

    override fun equals(other: Any?): Boolean {
        if (!PrimitiveTag.isTagAndEqName(this, other)) return false
        other as CompoundTag
        if (value.size != other.value.size) return false
        return value.all { tag-> other.value.find{it==tag}!=null } && other.value.all { tag-> value.find{tag==it}!=null }
    }

    fun add(value: Tag<out Any>) {
        this.value.add(value)
    }
}
