package com.myna.mnbt.converter

import com.myna.mnbt.Tag
import com.myna.mnbt.converter.meta.TagLocatorInstance
import com.myna.mnbt.tag.CompoundTag
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.util.Deque
import kotlin.math.abs

/**
 * default intent to TagConverter
 */
interface ConverterCallerIntent

/**
 * the intent that is used for calling Converter handles object with hierarchical data structure
 */
interface RecordParents: ConverterCallerIntent {
    /**
     * record all parents info
     */
    val parents: Deque<Any>
}

/**
 * an interface specifies that all sub-interface is used to createTag
 */
interface CreateTagIntent:ConverterCallerIntent

/**
 * an locator for finding target tag in already built tag hierarchical structure
 */
interface TagLocator:CreateTagIntent {
    /**
     * find a tag in TagLocator by absolute path passed in
     * @param absolutePath mnbt format absolute URL String
     * @return target tag. Returns null if path incorrect or found a tag in path but id incorrect
     */
    fun findAt(absolutePath: String, id:Byte):Tag<out Any>?


    /**
     * link a new tag with an absolute parent path
     * @param tag the tag want to linked
     * @param absolutePath: the parent path to target [tag]
     */
    fun linkTagAt(absolutePath: String, tag:Tag<out Any>):Boolean

    /**
     * build hierarchical tag structure by input absolute path.
     * All path segment related tag is regarded as a Hierarchical Tag
     * @return the tag related to the last directory in the path
     * @throws IllegalArgumentException if first path segment is not equal to the root name in TagLocator,
     * or it is not a absolute path,
     * or a path segment related tag already exists in tag structure, but it is not an Hierarchical Tag
     */
    fun buildPath(absolutePath: String):Tag<out Any>
}



interface ParentTagsInfo:CreateTagIntent {
    val rootContainerPath:String
}

interface ToValueIntent:ConverterCallerIntent

/**
 * ignore the typeToken in, which means converter will handle value type by itself.
 * Typical usage: let Converter return default value when calling toValue
 */
interface IgnoreValueTypeToken:ToValueIntent

