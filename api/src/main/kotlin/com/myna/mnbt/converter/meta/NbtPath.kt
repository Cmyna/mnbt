package com.myna.mnbt.converter.meta

import com.myna.mnbt.Tag
import com.myna.mnbt.converter.ReflectiveConverter
import com.myna.mnbt.converter.TagConverter
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.lang.reflect.Field

/**
 * nbt url: using url format to represent a locator to a tag structure
 *
 * the url format is: `mnbt://[hierarchical tag path]`
 *
 * where [hierarchical tag path] format is: `<tag name1>/<tag name2>/<tag name3>/...`
 *
 * you can use related tag path, which is started at "./", or access the parent tag by using "../"
 *
 * to represent a list index in path, use "#index" as a separate tag name between two "/" without any other symbol,
 * where "index" is a number to a list index
 *
 * if the path is ended with "/", means the last tag name is a hierarchical tag (eg: a compound tag or a list tag),
 * else it is a flat tag without any subtag inside
 */

/**
 * this interface is used in reflective conversion that [ReflectiveConverter]
 * can reconstruct tag structure from class TypeToken with its field passed in [TagConverter.createTag]/[TagConverter.toValue] function
 *
 * if one class want to remap to another tag structure (not follow by its class name & field name),
 * it should implement this interface.
 *
 * for all array returns from the interface (can be regarded as a "path"),
 * we denote last element as "data entry tag".
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

        fun toRelatedPath(vararg relatedPath: String):String {
            val builder = StringBuilder("./")
            var firstOnEach = true
            relatedPath.onEach {
                if (!firstOnEach) builder.append("/")
                if (firstOnEach) {
                    firstOnEach = false
                }
                builder.append(it)
            }
            return builder.toString()
        }

        fun combine(absolutePath: String, relatedPath:String):String {
            absolutePath.substring(0, scheme.length).also {
                if (it != scheme) throw IllegalArgumentException("the path URL ($absolutePath) passed in is not an absolute path!")
            }
            relatedPath.substring(0, 2).also {
                if (it != "./") throw IllegalArgumentException("the related path URL ($relatedPath) is not an related path!")
            }
            val builder = StringBuilder()
            builder.append(absolutePath)
            if (absolutePath.last() != '/') builder.append("/")
            builder.append(relatedPath.substring(2, relatedPath.length))
            return builder.toString()
        }

        fun appendSubDir(absolutePath: String, subDir:String):String {
            return if (absolutePath.last() != '/') "$absolutePath/$subDir"
            else "$absolutePath$subDir"
        }

        fun isAbsolutePath(path:String):Boolean {
            path.substring(0, scheme.length).also {
                return it == scheme
            }
        }

        fun isRelatedPath(path:String):Boolean {
            return path.length>=2 && path.first()=='.' && path.get(1)=='/'
        }

        fun toAccessQueue(path:String):Sequence<String> {
            val pathValue = if (isAbsolutePath(path)) path.substring(scheme.length, path.length)
            else if (isRelatedPath(path)) path.substring(2, path.length)
            else path
            return tagNameRegex.findAll(pathValue).map {
                it.value
            }
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
    }
}