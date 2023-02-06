package com.myna.mnbt.converter.meta

import com.myna.mnbt.IdTagCompound
import com.myna.mnbt.Tag
import com.myna.mnbt.converter.TagLocator
import com.myna.mnbt.converter.meta.NbtPathTool.scheme
import com.myna.mnbt.converter.meta.NbtPathTool.tagNameRegex
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag
import java.lang.IllegalArgumentException

class TagLocatorInstance
    (override val root:Tag<out Any>) :TagLocator {


    override fun findAt(absolutePath: String, id:Byte): Tag<out Any>? {
        val accessSeq = buildSequencesWithoutRoot(absolutePath)
        return NbtPathTool.findTag(root, accessSeq, id)
    }

    override fun linkTagAt(absolutePath: String, tag: Tag<out Any>): Boolean {
        val parent = findAt(absolutePath, IdTagCompound)?: return false
        val parentContainer = parent.value
        if (parentContainer !is MutableMap<*,*>) return false
        parentContainer as MutableMap<String, Tag<out Any>>
        if (tag.name == null) return false
        //if (parentContainer[tag.name!!] != null) throw RuntimeException()
        parentContainer[tag.name!!] = tag
        return true
    }

    override fun buildPath(absolutePath: String): Tag<out Any> {
        val accessSeq = buildSequencesWithoutRoot(absolutePath)
        var current:Tag<out Any>? = root
        // drop root tag
        accessSeq.forEach { pathSeg ->
            val value = current!!.value
            // TODO: make it can handle ListTag
            if (value !is MutableMap<*,*>) {
                throw IllegalArgumentException(
                        "found tag by $pathSeg in $absolutePath ->" +
                        "the tag (with value type ${value::class.java}) found is not an Hierarchical Tag")
            }
            value as AnyCompound
            val subTag = value[pathSeg]?: CompoundTag(pathSeg)
            value[pathSeg] = subTag
            current = subTag
        }
        return current!!
    }

    private fun buildSequencesWithoutRoot(absolutePath: String):Sequence<String> {
        return buildSequences(absolutePath).drop(1)
    }

    private fun buildSequences(absolutePath: String):Sequence<String> {
        checkIsAbsolutePath(absolutePath)
        val pathValue = absolutePath.substring(scheme.length, absolutePath.length)
        val accessSeq = tagNameRegex.findAll(pathValue)
        val pathRoot = accessSeq.first().value
        val rootNameIsNull = pathRoot.first()=='#' && root.name==null
        if (pathRoot != root.name && !rootNameIsNull) {
            throw IllegalArgumentException("path root tag name (path url: $absolutePath) is not equals to locator root (${root.name})!")
        }
        return accessSeq.map { it.value }
    }

    private fun checkIsAbsolutePath(path:String) {
        if (!NbtPathTool.isAbsolutePath(path)) throw IllegalArgumentException("the path URL ($path) passed in is not an absolute path!")
    }
}