package net.myna.mnbt.tag

import net.myna.mnbt.IdTagList
import net.myna.mnbt.Tag
import net.myna.mnbt.converter.meta.NbtPathTool.indexFormatRegex
import java.lang.StringBuilder

typealias AnyTagList = MutableList<Tag<out Any>>
typealias UnknownList = MutableList<*>

/**
 *
 */
@Suppress("EqualsOrHashCode")
class ListTag<TAG: Tag<out Any>>(
        override val name: String?,
        override val value: MutableList<TAG>,
        /**
         * the element tag type id allowed in list
         */
        var elementId:Byte
        ) : Tag.NestTag<MutableList<TAG>>() {

    override val id: Byte = IdTagList

    constructor(elementId: Byte, name:String?=null):this(name, mutableListOf(), elementId)

    fun add(tag:TAG):Boolean {
        if (elementId == unknownElementId) elementId = tag.id
        if (tag.id != elementId) return false
        return value.add(tag)
    }

    operator fun get(i:Int):TAG? {
        return if (value.size <= i) null
        else value[i]
    }

    override fun valueToString(): String {
        val builder = StringBuilder("[")
        value.fold(true) { isFirst, cur ->
            if (!isFirst) builder.append(",${cur.valueToString()}")
            else builder.append(cur.valueToString())
            false
        }
        return builder.append("]").toString()
    }

    @Suppress("UNCHECKED_CAST")
    override fun equals(other: Any?): Boolean {
        // because NbtRelatedType is from other object
        // the equals function may not perform as expected
        // we need to get the related encapsulated Tag instance and use its equals function
        if (!isTagAndEqName(this, other)) return false
        if (other !is ListTag<*>) return false
        return listEqFun(value as List<Tag<Any>>, other.value)
    }

//    override fun toString(): String {
//        val builder = StringBuilder()
//        builder.append("[List Tag]\n")
//        builder.append("\tTag name: $name\n")
//        builder.append("\tlist size: ${value.size}\n")
//        if (value.size > 0 ) builder.append("\telement type: ${value.get(0)!!::class.java}")
//        return builder.toString()
//    }

    @Suppress("UNCHECKED_CAST")
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