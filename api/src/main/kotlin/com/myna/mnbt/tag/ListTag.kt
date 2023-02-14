package com.myna.mnbt.tag

import com.myna.mnbt.IdTagList
import com.myna.mnbt.Tag
import com.myna.mnbt.converter.meta.NbtPathTool.indexFormatRegex
import java.lang.StringBuilder

typealias AnyTagList = MutableList<Tag<out Any>>
typealias UnknownList = MutableList<*>

/**
 *
 */
// so the tag declaration require it is a generic type VT that there is sub-class extends Tag<VT>
// FIXME: better way to design generic type
class ListTag<NbtRelatedType:Any>(
        override val name: String?,
        override val value: MutableList<Tag<out NbtRelatedType>>,
        /**
         * the element tag type id allowed in list
         */
        var elementId:Byte
        ) : Tag.NestTag<MutableList<Tag<out NbtRelatedType>>>() {

    override val id: Byte = IdTagList

    constructor(elementId: Byte, name:String?=null):this(name, mutableListOf(), elementId)

    fun <T: Tag<NbtRelatedType>> add(tag:T):Boolean {
        if (elementId == unknownElementId) elementId = tag.id
        if (tag.id != elementId) return false
        return value.add(tag)
    }

    operator fun get(i:Int):Tag<NbtRelatedType>? {
        return if (value.size <= i) null
        else value[i] as Tag<NbtRelatedType>
    }

    override fun equals(other: Any?): Boolean {
        // because NbtRelatedType is from other object
        // the equals function may not perform as expected
        // we need to get the related encapsulated Tag instance and use its equals function
        if (!isTagAndEqName(this, other)) return false
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

    override fun <V> getElementByPath(pathSegment: String): Tag<out V>? {
        if (indexFormatRegex.matchEntire(pathSegment)==null) return null
        val index = pathSegment.substring(1, pathSegment.length).toInt()
        if (value.size <= index) return null
        return value[index] as Tag<out V>
    }

    companion object {

        /**
         * specifies that list tag don't know element tag type id in list, this happens when list tag created without any sub tags in list
         */
        const val unknownElementId:Byte = -1

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