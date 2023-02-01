package com.myna.mnbt.converter.meta

import com.myna.mnbt.Tag
import com.myna.mnbt.converter.TagLocator
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag
import java.lang.IllegalArgumentException

class TagLocatorInstance
    (private val root:Tag<out Any>) :TagLocator {


    override fun findAt(absolutePath: String, id:Byte): Tag<out Any>? {
        return findTag(absolutePath)
    }

    override fun linkTagAt(absolutePath: String, tag: Tag<out Any>): Boolean {
        val parent = findTag(absolutePath)?: return false
        val parentContainer = parent.value
        if (parentContainer !is MutableMap<*,*>) return false
        parentContainer as MutableMap<String, Tag<out Any>>
        if (tag.name == null) return false
        parentContainer[tag.name!!] = tag
        return true
    }

    override fun buildPath(absolutePath: String): Tag<out Any> {
        absolutePath.substring(0, scheme.length).also {
            if (it != scheme) throw IllegalArgumentException("the path URL ($absolutePath) passed in is not an absolute path!")
        }
        val pathValue = absolutePath.substring(scheme.length, absolutePath.length)
        val accessSeq = tagNameRegex.findAll(pathValue)
        val pathRoot = accessSeq.first().value
        if (pathRoot != root.name && pathRoot!="null" && root.name!=null) {
            throw IllegalArgumentException("the first segment of path passed in ($absolutePath) is not equal to the root name (${root.name}) in locator!")
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

    private fun findTag(path:String):Tag<out Any>? {
        path.substring(0, scheme.length).also {
            if (it != scheme) throw IllegalArgumentException("the path URL ($path) passed in is not an absolute path!")
        }
        val pathValue = path.substring(scheme.length, path.length)
        val accessSeq = tagNameRegex.findAll(pathValue)
        return findTag(accessSeq)
    }

    /**
     * @param accessQueue list of tag names, specifies the access sequences
     */
    private fun findTag(accessQueue:Sequence<MatchResult>):Tag<out Any>? {
        var current:Tag<out Any>? = null
        var sequenceMatchFlag = false
        accessQueue.forEach { match->
            if (current == null) {
                current = root
                sequenceMatchFlag = true
                return@forEach
            }
            sequenceMatchFlag = false
            val subTagName = match.value
            val value = current!!.value
            if (value !is Map<*,*>) return@forEach
            val subTag = value[subTagName]?: return@forEach
            current = subTag as Tag<out Any>
            sequenceMatchFlag = true
        }
        return if (sequenceMatchFlag) current else null
    }

    companion object {
        const val scheme = "mnbt://"
        private val tagNameRegex = Regex("(?:(?:\\\\\\/)|[^\\/])+")
    }
}