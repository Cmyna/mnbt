package net.myna.mnbt.codec

import com.google.common.annotations.Beta
import net.myna.mnbt.Tag
import java.io.InputStream
import java.io.OutputStream
import java.util.Deque

//TODO: find better way to manage those intents with feedback
// in general, an intent may related to an feedback, or also it may not related to an feedback
// there are some feedbacks that don't related to any specific intents, or we regard them as 'feedbacks to all intents'
// it seems that we need something like channel?

// For flexibility, I want a set of intents that can do types intersection together
// However in kotlin(also java) there is no interface type intersection
// I have to develop those intent interfaces as below(high flexibility, less type safety checking)
// so there is a rule in using these intents: do checking when first receive an intent! check the type you want!

interface CodecCallerIntent

@Beta
interface EncodeIntent:CodecCallerIntent

@Beta
interface EncodeHead:EncodeIntent{
    /**
     * declares that caller want Codec to encode with head or not.
     *
     * if encodeHead=false when encoded, the Codec will only encode value.
     *
     * if encodeHead=false when decoded, which means caller specifies pointer to data is start point
     * of tag value, which also means that Codec can not check nbt tag format, format check
     * should be done by caller
     */
    val encodeHead:Boolean
}

@Beta
interface RecordParentsWhenEncoding:EncodeIntent {
    /**
     * pass parents tag info to HierarchicalCodec
     *
     * use for handling circular reference and tree depth limit
     */
    val parents: Deque<Tag<out Any>>
}

@Beta
/**
 * specify that serialization base on an output stream
 */
interface EncodeOnStream:EncodeIntent {
    val outputStream: OutputStream
}

@Beta
interface OnStreamToDelegatorEncodeIntent:EncodeHead,EncodeOnStream,RecordParentsWhenEncoding

@Beta
/**
 * an interface specifies that encode tag to ByteArray,
 * the Codec should return object implements [EncodedBytesFeedback] to contain ByteArray
 */
interface EncodeToBytes:EncodeIntent

@Beta
interface OnBytesToProxyEncodeIntent:EncodeHead,EncodeToBytes,RecordParentsWhenEncoding

@Beta
interface DecodeIntent:CodecCallerIntent

@Beta
/**
 * decode head or not where tag head contains id+tag name.
 * it also means input binary data has tag head info
 */
interface DecodeHead:DecodeIntent {

    /**
     * ignore id when decode head is true, if it is true,
     * the binary tag head data has id at front, else no tag id info in binary data
     */
    val ignoreIdWhenDecoding:Boolean
}

@Beta
/**
 * specify that deserialization base on an input stream
 */
interface DecodeOnStream:DecodeIntent {
    val inputStream: InputStream
}

@Beta
/**
 * record binary data tree depth when decoding
 */
interface DecodeTreeDepth:DecodeIntent {
    var depth:Int
}

@Beta
interface SpecifyIdWhenDecoding:DecodeIntent {
    /**
     * used for deserialization, caller specifies tag id in binary nbt data, typical use case: delegate deserialization to proxy when hasHead is false
     */
    val id:Byte
}

@Beta
/**
 * an interface specifies that decode binary tag from a ByteArray
 */
interface DecodeFromBytes:DecodeIntent {
    val data:ByteArray
    /**
     * current pointer to data
     */
    var pointer:Int
}





