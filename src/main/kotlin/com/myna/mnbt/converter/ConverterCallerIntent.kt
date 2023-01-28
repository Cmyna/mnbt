package com.myna.mnbt.converter

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
