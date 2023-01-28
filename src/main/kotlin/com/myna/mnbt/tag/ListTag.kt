package com.myna.mnbt.tag

import com.myna.mnbt.IdTagList
import com.myna.mnbt.Tag
import java.lang.StringBuilder

typealias AnyTagList = MutableList<Tag<out Any>>

/**
 *
 */
// so the tag declaration require it is a generic type VT that there is sub-class extends Tag<VT>
class ListTag<NbtRelatedType:Any>(
        override val name: String?,
        override val value: MutableList<Tag<out NbtRelatedType>>,
        val elementId:Byte
        ) : Tag<MutableList<Tag<out NbtRelatedType>>>() {

    override val id: Byte = IdTagList

    constructor(elementId: Byte, name:String?=null):this(name, mutableListOf(), elementId)

    fun <T: Tag<NbtRelatedType>> add(tag:T):Boolean {
        if (tag.id != elementId) return false
        return value.add(tag)
    }

    override fun equals(other: Any?): Boolean {
        // because NbtRelatedType is from other object
        // the equals function may not perform as expected
        // we need to get the related encapsulated Tag instance and use its equals function
        if (!PrimitiveTag.isTagAndEqName(this, other)) return false
        if (other !is ListTag<*>) return false
        return listEqFun(value as List<Tag<Any>>, other.value)
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("[List Tag]\n")
        builder.append("\tTag name: $name\n")
        builder.append("\tlist size: ${value.size}\n")
        if (value.size > 0 ) builder.append("\telement type: ${value.get(0)!!::class.java}")
        return builder.toString()
    }

    companion object {

        fun listEqFun(list: List<Tag<Any>>, other: Any?):Boolean {
            if (other !is List<*>) return false
            if (list.size != other.size) return false
            for (i in list.indices) {
                if (list[i] != other[i]) return false
            }
            return true
        }
    }
}