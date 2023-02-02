package com.myna.mnbt.converter.meta

import com.myna.mnbt.IdTagCompound
import com.myna.mnbt.Tag
import com.myna.mnbt.converter.TagLocator
import com.myna.mnbt.converter.meta.NbtPath.Companion.scheme
import com.myna.mnbt.converter.meta.NbtPath.Companion.tagNameRegex
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag
import java.lang.IllegalArgumentException

class TagLocatorInstance
    (private val root:Tag<out Any>) :TagLocator {


    override fun findAt(absolutePath: String, id:Byte): Tag<out Any>? {
        checkIsAbsolutePath(absolutePath)
        val accessSeq = NbtPath.toAccessQueue(absolutePath)
        if (accessSeq.first() != root.name) {
            throw IllegalArgumentException("path root tag name ${accessSeq.first()} is not equals to locator root (${root.name})!")
        }
        return NbtPath.findTag(root, accessSeq.drop(1), id)
    }

    override fun linkTagAt(absolutePath: String, tag: Tag<out Any>): Boolean {
        val parent = findAt(absolutePath, IdTagCompound)?: return false
        val parentContainer = parent.value
        if (parentContainer !is MutableMap<*,*>) return false
        parentContainer as MutableMap<String, Tag<out Any>>
        if (tag.name == null) return false
        parentContainer[tag.name!!] = tag
        return true
    }

    override fun buildPath(absolutePath: String): Tag<out Any> {
        checkIsAbsolutePath(absolutePath)
        val pathValue = absolutePath.substring(scheme.length, absolutePath.length)
        val accessSeq = tagNameRegex.findAll(pathValue)
        val pathRoot = accessSeq.first().value
        if (pathRoot != root.name && pathRoot!="null" && root.name!=null) {
            throw IllegalArgumentException("path root tag name (path url: $absolutePath) is not equals to locator root (${root.name})!")
        }
        var current:Tag<out Any>? = null
        accessSeq.forEach { match ->
            if (current == null) {
                current = root
                return@forEach
            }
            val pathSeg = match.value
            val value = current!!.value
            if (value !is MutableMap<*,*>) {
                // TODO: more clear exception throws
                throw IllegalArgumentException("the path segment $pathSeg " +
                        "related Tag type (tag with value type${value::class.java}) found in whole exists structure is not as expected")
            }
            value as AnyCompound
            if (value[pathSeg] == null) {
                val newSubTag = CompoundTag(pathSeg)
                value[pathSeg] = newSubTag
                current = newSubTag
            } else {
                current = value[pathSeg] as Tag<out Any>
            }
        }
        return current!!
    }

    private fun checkIsAbsolutePath(path:String) {
        if (!NbtPath.isAbsolutePath(path)) throw IllegalArgumentException("the path URL ($path) passed in is not an absolute path!")
    }
}