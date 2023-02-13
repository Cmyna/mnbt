package com.myna.mnbt.converter.meta

import com.myna.mnbt.Tag
import java.lang.IllegalArgumentException
import java.lang.StringBuilder

/**
 * an utils for handling mnbt url format string
 */
object NbtPathTool{

    const val scheme = "mnbt://"
    val tagNameRegex = Regex("(?:\\\\/|[^\\/])+")
    val indexFormatRegex = Regex("^#\\d+$")

    /**
     * return mnbt relate path string
     * @param pathSegments: an array of string specifies each segment in the returned relate path
     */
    fun toRelatedPath(vararg pathSegments: String):String {
        if (pathSegments.isEmpty()) return "./"
        val builder = StringBuilder("./${pathSegments.first()}")
        pathSegments.asSequence().drop(1).forEach {
            builder.append("/")
            builder.append(it)
        }
        return builder.toString()
    }

    /**
     * append relate path to absolute path's end
     * @param path mnbt format path
     * @param relatedPath mnbt format relate path
     * @return the appended result
     * @throws IllegalArgumentException if first args is not an absolute path,
     * or second one is not an relate path
     */
    fun combine(path: String, relatedPath:String):String {
        if (!isRelatedPath(relatedPath)) throw IllegalArgumentException("the related path URL ($relatedPath) is not an related path!")

        val builder = StringBuilder()
        builder.append(path)
        if (path.last() != '/') builder.append("/")
        builder.append(relatedPath.substring(2, relatedPath.length))
        return builder.toString()
    }

    /**
     * append a path segment into absolute path string end
     * @param prePath mnbt format path
     * @param pathSegment path segment string
     * @return the appended result
     * @throws IllegalArgumentException if first args is not an absolute path
     */
    fun append(prePath: String, pathSegment:String):String {
        return if (prePath.last() != '/') "$prePath/$pathSegment"
        else "$prePath$pathSegment"
    }

    /**
     * check is mnbt format absolute path or not
     */
    fun isAbsolutePath(path:String):Boolean {
        if (path.length < scheme.length) return false
        path.substring(0, scheme.length).also {
            return it == scheme
        }
    }

    /**
     * check is mnbt format relate path or not
     */
    fun isRelatedPath(path:String):Boolean {
        return path.length>=2 && path.first()=='.' && path.get(1)=='/'
    }

    /**
     * @param path: an mnbt path format string
     * @return a sequence to access each path segment
     */
    fun toAccessSequence(path: String): Sequence<String> {
        val pathValue = if (isAbsolutePath(path)) path.substring(scheme.length, path.length)
        else if (isRelatedPath(path)) path.substring(2, path.length)
        else path
        return tagNameRegex.findAll(pathValue).map { it.value }
    }


    /**
     * function will search tag by relatePath start at entry parameter passed in,
     * @param entry search entry
     * @param relatePath mnbt relate path, except entry tag at beginning
     * @param targetTagId the target tag id, if it is null, then not check found id
     * @return a tag if path and id matched,
     * if no segment in path and source id match targetTagId, return [entry],
     * if path is not an relate path or not found match tag, return null.
     * @throws IllegalArgumentException if path segment in sequence has illegal format
     * @throws IndexOutOfBoundsException if path segment is an index, but index over the related container's length
     * if sequence is empty and source id match targetTagId, return [start]
     */
    fun goto(entry:Tag<out Any>, relatePath:String, targetTagId: Byte? = null):Tag<out Any>? {
        if (!isRelatedPath(relatePath)) return null
        val accessSeq = toAccessSequence(relatePath)
        return findTag(entry, accessSeq, targetTagId)
    }

    /**
     * @param start start point tag
     * @param accessSequence an access sequences with path segments name
     * @param targetTagId the target tag id, if it is null, then not check found id
     * @return a tag if path and id matched,
     * @throws IllegalArgumentException if path segment in sequence has illegal format
     * @throws IndexOutOfBoundsException if path segment is an index, but index over the related container's length
     * if sequence is empty and source id match targetTagId, return [start]
     */
    fun findTag(start:Tag<out Any>, accessSequence:Sequence<String>, targetTagId:Byte? = null): Tag<out Any>? {
        var current: Tag<out Any>? = start // if access sequence is empty (for each not run), return start tag
        var sequenceMatchFlag = true
        accessSequence.forEach { pathSegment->
            sequenceMatchFlag = false
            val value = current!!.value
            val subTag = getSubTag(value, pathSegment)?: return@forEach
            current = subTag
            sequenceMatchFlag = true
        }
        val idMatch = if (current!=null && targetTagId!=null) current!!.id==targetTagId else true
        return if (sequenceMatchFlag && idMatch) current else null
    }

    private fun getSubTag(container:Any?, pathSegment: String):Tag<out Any>? {
        val element = if (pathSegment.first() == '#') {
            if (indexFormatRegex.matchEntire(pathSegment) == null) {
                    throw IllegalArgumentException("segment in input path has illegal index format $pathSegment")
            }
            val index = pathSegment.substring(1, pathSegment.length).toInt()
            if (container !is List<*>) return null
            container[index]
        } else {
            if (container !is Map<*,*>) return null
            container[pathSegment]
        }

        return if (element !is Tag<*>) null
        else element as Tag<out Any>
    }


    fun format(str:String):String {
        if (str.length>=2 && str.first()!='.' && str[1] != '/') return "./$str"
        else if (str.isNotEmpty() && str.first()=='/') return ".$str"
        return str
    }
}
