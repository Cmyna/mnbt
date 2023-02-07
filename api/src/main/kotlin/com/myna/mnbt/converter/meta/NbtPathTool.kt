package com.myna.mnbt.converter.meta

import com.myna.mnbt.Tag
import java.lang.IllegalArgumentException
import java.lang.StringBuilder

/**
 * doc: TODO
 */
object NbtPathTool{

    const val scheme = "mnbt://"
    val tagNameRegex = Regex("(?:(?:\\\\\\/)|[^\\/])+")

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
     * @param absolutePath mnbt format absolute path
     * @param relatedPath mnbt format relate path
     * @return the appended result
     * @throws IllegalArgumentException if first args is not an absolute path,
     * or second one is not an relate path
     */
    fun combine(absolutePath: String, relatedPath:String):String {
        if (!isAbsolutePath(absolutePath)) throw IllegalArgumentException("the path URL ($absolutePath) passed in is not an absolute path!")
        if (!isRelatedPath(relatedPath)) throw IllegalArgumentException("the related path URL ($relatedPath) is not an related path!")

        val builder = StringBuilder()
        builder.append(absolutePath)
        if (absolutePath.last() != '/') builder.append("/")
        builder.append(relatedPath.substring(2, relatedPath.length))
        return builder.toString()
    }

    /**
     * append a path segment into absolute path string end
     * @param absolutePath mnbt format absolute path
     * @param pathSegment path segment string
     * @return the appended result
     * @throws IllegalArgumentException if first args is not an absolute path
     */
    fun appendSubDir(absolutePath: String, pathSegment:String):String {
        if (!isAbsolutePath(absolutePath)) throw IllegalArgumentException("the path URL ($absolutePath) passed in is not an absolute path!")

        return if (absolutePath.last() != '/') "$absolutePath/$pathSegment"
        else "$absolutePath$pathSegment"
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
     * @param source start point tag
     * @param path mnbt absolute path
     * @param targetTagId the target tag id, if it is null, then not check found id
     * @return a tag if path and id matched
     */
    fun findTag(source:Tag<out Any>, path:String, targetTagId: Byte? = null):Tag<out Any>? {
        val accessSeq = toAccessSequence(path)
        val rootName = accessSeq.firstOrNull()?: return null
        // TODO: refact code logic
        val rootNameIsNull = rootName.first()=='#' && source.name==null
        if (rootName != source.name && !rootNameIsNull) return null
        return findTag(source, accessSeq.drop(1), targetTagId)
    }

    /**
     * @param source start point tag
     * @param accessSequence an access sequences with path segments name
     * @param targetTagId the target tag id, if it is null, then not check found id
     * @return a tag if path and id matched
     */
    fun findTag(source:Tag<out Any>, accessSequence:Sequence<String>, targetTagId:Byte? = null): Tag<out Any>? {
        var current: Tag<out Any>? = source
        var sequenceMatchFlag = true
        accessSequence.forEach { subTagName->
            sequenceMatchFlag = false
            val value = current!!.value
            if (value !is Map<*,*>) return@forEach
            val subTag = value[subTagName]?: return@forEach
            current = subTag as Tag<out Any>
            sequenceMatchFlag = true
        }
        val idMatch = if (current!=null && targetTagId!=null) current!!.id==targetTagId else true
        return if (sequenceMatchFlag && idMatch) current else null
    }


    fun format(str:String):String {
        if (str.length>=2 && str.first()!='.' && str[1] != '/') return "./$str"
        else if (str.isNotEmpty() && str.first()=='/') return ".$str"
        return str
    }
}
