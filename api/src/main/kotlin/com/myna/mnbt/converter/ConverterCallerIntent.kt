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

/**
 * record an compound sub-trees info with target tag returned from current createTag call
 */
interface BuiltCompoundSubTree:CreateTagIntent {
    val root:Tag<out Any>

    /**
     * Target tag's relate path returned from current createTag call
     * the relate path starts at root (exclude root).
     */
    val createdTagRelatePath:String
}

interface OverrideTag:CreateTagIntent {
    val overrideTargetTag:Tag<out Any>
}

interface ToValueIntent:ConverterCallerIntent

/**
 * ignore the typeToken in, which means converter will handle value type by itself.
 * Typical usage: let Converter return default value when calling toValue
 */
interface IgnoreValueTypeToken:ToValueIntent

