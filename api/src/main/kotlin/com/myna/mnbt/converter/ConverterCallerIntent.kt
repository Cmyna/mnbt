package com.myna.mnbt.converter

import com.myna.mnbt.Tag
import com.myna.mnbt.tag.CompoundTag
import java.util.Deque

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
     * @param absolutePath mnbt format absolute URL String
     */
    fun findAt(absolutePath: String, id:Byte):Tag<out Any>?


    /**
     * link a new tag with an absolute parent path
     */
    fun linkTagAt(absolutePath: String, tag:Tag<out Any>):Boolean
}

fun TagLocator.toPath(vararg relatedPath: String):String {
    TODO()
}

fun TagLocator.combine(absolutePath: String, relatedPath:String):String {
    TODO()
}

interface ParentTagsInfo:CreateTagIntent {
    /**
     * @return an complete mnbt format url
     */
    fun getRootPath():String
}

interface ToValueIntent:ConverterCallerIntent

interface ToValueTypeToken:ToValueIntent {
    /**
    * ignore the typeToken in, which means converter will handle value type by itself.
    * Typical usage: let Converter return default value when calling toValue
    */
    val ignore:Boolean
}

/**
 * this interface specify the returned value is converted start at which Tag in the whole Tag structure
 */
interface StartAt:ToValueIntent {
    val path:String
    val tagTypeId:Byte
}

