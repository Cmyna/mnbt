package com.myna.mnbt.converter

import com.myna.mnbt.Tag
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

interface NbtTreeInfo:CreateTagIntent {
    val root:Tag<out Any>

    /**
     * the relate path starts at root (exclude root)
     */
    val createdTagRelatePath:String
}

interface ToValueIntent:ConverterCallerIntent

/**
 * ignore the typeToken in, which means converter will handle value type by itself.
 * Typical usage: let Converter return default value when calling toValue
 */
interface IgnoreValueTypeToken:ToValueIntent

