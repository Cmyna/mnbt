package com.myna.mnbt.converter.meta

import com.myna.mnbt.Tag
import com.myna.mnbt.converter.ReflectiveConverter
import com.myna.mnbt.converter.TagConverter
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.lang.reflect.Field

// TODO: deprecate NbtPath
@Deprecated("")
/**
 * this interface is used in reflective conversion that [ReflectiveConverter]
 * can reconstruct tag structure from class TypeToken with its field passed in [TagConverter.createTag]/[TagConverter.toValue] function
 *
 * if one class want to remap to another tag structure (not follow by its class name & field name),
 * it should implement this interface.
 *
 * for all array returns from the interface (can be regarded as a "path"),
 * we denote last element as "data entry tag".
 *
 * [com.myna.mnbt]
 */
interface NbtPath {
    /**
     * @return related extra tag path to the class implement this interface
     * each tag name in related tag path is separated into arrays.
     * the array is "optional", which means it can be empty or null
     *
     * the extra tag path is the sub path for a related tag path for the class, which is presented as:
     * `./rootName/[extra class paths]/`
     *
     * eg: if result is `["tag1","tag2","tag3"]`, Converter will find `./<root>/tag1/tag2/tag3/`
     * starts at tag named `<root>` passed in [TagConverter.toValue] method; else if result is empty array,
     * converter will regard tag passed in [TagConverter.toValue] as tag of target class
     *
     * if it is happen in [TagConverter.createTag] method, it will create the structure like:
     * "./root/tag1/tag2/tag3" where root is the root compound tag with name parameter "root" passed in method;
     * it result is empty array, then tag named "root" will be regard as target tag for the class object
     */
    fun getClassExtraPath():Array<String> = arrayOf()

    /**
     * @return a map recording field name->related path.
     * all value in map is stored as Array<String>, we can rewrite related nbt path by these array as:
     * `./[extra field path]/field`. The `[extra field path]` is optional.
     *
     * The related path is path related to result from [getClassExtraPath].
     * if we look for the field enclosed class, the field related nbt path is like: `./root/[extra class path]/[extra field path]/field`
     *
     * eg: if a field related path is ["subTag1","subTag2"], where class related path is ["tag1", "tag2"],
     * then reflective converter will try find `<root>/tag1/tag2/subTag1/subTag2` at tag named `<root>` passed in
     *
     * @throws IndexOutOfBoundsException if some array is empty.
     * Which means should at least one element represent field name in the array
     */
    fun getFieldsPaths():Map<Field, Array<String>>

    fun getFieldsTagType():Map<Field, Byte>

    companion object {

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
        fun toAccessQueue(path: String): Sequence<String> {
            val pathValue = if (isAbsolutePath(path)) path.substring(scheme.length, path.length)
            else if (isRelatedPath(path)) path.substring(2, path.length)
            else path
            return tagNameRegex.findAll(pathValue).map { it.value }
        }

        fun findTag(source:Tag<out Any>, path:String, targetTagId: Byte? = null):Tag<out Any>? {
            val accessSeq = toAccessQueue(path)
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
}