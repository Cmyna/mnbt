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
     * doc: TODO
     */
    fun toRelatedPath(vararg relatedPath: String):String {
        if (relatedPath.isEmpty()) return "./"
        val builder = StringBuilder("./${relatedPath.first()}")
        relatedPath.asSequence().drop(1).forEach {
            builder.append("/")
            builder.append(it)
        }
        return builder.toString()
    }

    /**
     * doc: TODO
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
     * doc: TODO
     */
    fun appendSubDir(absolutePath: String, subDir:String):String {
        return if (absolutePath.last() != '/') "$absolutePath/$subDir"
        else "$absolutePath$subDir"
    }

    /**
     * doc: TODO
     */
    fun isAbsolutePath(path:String):Boolean {
        if (path.length < scheme.length) return false
        path.substring(0, scheme.length).also {
            return it == scheme
        }
    }

    /**
     * doc: TODO
     */
    fun isRelatedPath(path:String):Boolean {
        return path.length>=2 && path.first()=='.' && path.get(1)=='/'
    }

    /**
     * doc: TODO
     */
    fun toAccessSequence(path: String): Sequence<String> {
        val pathValue = if (isAbsolutePath(path)) path.substring(scheme.length, path.length)
        else if (isRelatedPath(path)) path.substring(2, path.length)
        else path
        return tagNameRegex.findAll(pathValue).map { it.value }
    }

    fun findTag(source:Tag<out Any>, path:String, targetTagId: Byte? = null):Tag<out Any>? {
        val accessSeq = toAccessSequence(path)
        val rootName = accessSeq.firstOrNull()?: return null
        // TODO: refact code logic
        val rootNameIsNull = rootName.first()=='#' && source.name==null
        if (rootName != source.name && !rootNameIsNull) return null
        return findTag(source, accessSeq.drop(1), targetTagId)
    }

    /**
     * @param targetTagId nullable parameter, if it is null, not check found tag id type
     */
    fun findTag(source:Tag<out Any>, accessQueue:Sequence<String>, targetTagId:Byte? = null): Tag<out Any>? {
        var current: Tag<out Any>? = source
        var sequenceMatchFlag = true
        accessQueue.forEach { subTagName->
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
