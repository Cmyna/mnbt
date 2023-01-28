package com.myna.mnbt.codec

import com.myna.mnbt.Tag

// I decided to name this functionality to `Codec` (from https://english.stackexchange.com/questions/76549/a-word-that-describes-both-encoding-and-decoding)
/**
 * an interface for encode/decode (serialize/deserialize) a complete tag
 */
interface Codec<NbtRelatedType>{
    // because the 1:1:1 relation between NbtBinStructure : java related type : Codec
    // so unlike tagConverter, there is not generic type and type token required
    // the generic type in this interface is to restrict the implementation of Codec
    // however it can not constrain the caller
    // if someone use wrong delegate, it should directly throws exception
    /**
     * the related nbt type id to this Codec
     */
    val id:Byte
    /**
     * typeToken variable should represent the raw type of NbtRelatedType
     * for example, if NbtRelatedType is Map<K,V>, then it is recommended to regard it as Map::class.java
     * another example, maybe NbtRelatedType to List Tag is SomeList<T> then it is recommend to regard it as SomeList::class.java
     */
    val valueTypeToken: Class<NbtRelatedType>

//    val requiredEncodeIntent:List<EncodeIntent>
//    val requiredDecodedIntent:List<DecodeIntent>

    fun encode(tag: Tag<out NbtRelatedType>, intent:CodecCallerIntent):CodecFeedback
    /**
     * @param hasHead: declare the data has head or not (has id+name or not).
     * If no head, it means that no nbt type check, type check should be handled by caller
     * @return a Pair, first element is the Tag instance, second one points to the tag end in input byte array
     * (in consider, we may need to decode multiple tags in a byte stream, thus we need a pointer to remain data
     * after decode)
     */
    fun decode(intent:CodecCallerIntent): TagFeedback<NbtRelatedType>
}

// So the Codec accept something extends from Tag, or related java object as Tag value
// to simplify the interface, there is no 1 to many relation like TagConverter
// each Nbt Binary type related to one java class type, related to one Tag sub-class that implement it, related to one Codec

//TODO: the delegator not guarantees to handle circular ref, so it should not be called directly,
// (if call it directly, proxy's circular check will miss the top level tagValue passed to this delegator)
/**
 * this Codec is for those Tag with hierarchical tag structure.
 * so the Codec needs a proxy helping handle element tags inside the specific tag,
 */
interface HierarchicalCodec<NbtRelatedType>: Codec<NbtRelatedType> {
    var proxy: Codec<Any>
}