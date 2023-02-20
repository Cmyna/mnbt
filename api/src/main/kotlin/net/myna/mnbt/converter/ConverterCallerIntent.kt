package net.myna.mnbt.converter

import com.google.common.annotations.Beta
import net.myna.mnbt.Tag
import java.util.Deque

/**
 * default intent to TagConverter
 */
interface ConverterCallerIntent

@Beta
/**
 * the intent that is used for calling Converter handles object with hierarchical data structure
 */
interface RecordParents: ConverterCallerIntent {
    /**
     * record all parents info
     */
    val parents: Deque<Any>
}

@Beta
/**
 * an interface specifies that all sub-interface is used to createTag
 */
interface CreateTagIntent:ConverterCallerIntent

@Beta
/**
 * record a compound subtrees info with target tag returned from current createTag call
 */
interface BuiltCompoundSubTree:CreateTagIntent {
    val root:Tag<out Any>

    /**
     * Target tag's relate path returned from current createTag call
     * relate path starts at root (exclude root).
     */
    val createdTagRelatePath:String
}

@Beta
interface OverrideTag:CreateTagIntent {
    val overrideTarget:Tag<out Any>?
}

@Beta
interface ToValueIntent:ConverterCallerIntent

@Beta
/**
 * ignore the typeToken in, which means converter will handle value type by itself.
 * Typical usage: let Converter return default value when calling toValue
 */
interface IgnoreValueTypeToken:ToValueIntent

