package com.myna.mnbt.codec

import com.myna.mnbt.Tag
import java.io.InputStream
import java.io.OutputStream
import java.util.*

fun userEncodeIntent(outputStream: OutputStream):OnStreamToDelegatorEncodeIntent {
    return object:OnStreamToDelegatorEncodeIntent {
        override val parents: Deque<Tag<out Any>> = ArrayDeque()
        override val outputStream: OutputStream = outputStream
        override val encodeHead:Boolean = true
    }
}

fun userDecodeIntent(inputStream: InputStream):CodecCallerIntent {
    return object:CodecRecordParents,DecodeOnStream,DecodeHead {
        override val parents: Deque<Tag<out Any>> = ArrayDeque()
        override val inputStream = inputStream
        override val decodeHead: Boolean = true
        override val ignoreIdWhenDecoding: Boolean = false
    }
}

/**
 * intent that want Codec serialize tag to bytes
 */
fun userOnBytesEncodeIntent():OnBytesToProxyEncodeIntent {
    return object: OnBytesToProxyEncodeIntent {
        override val parents: Deque<Tag<out Any>> = ArrayDeque()
        override val encodeHead:Boolean = true
    }
}

fun userOnBytesDecodeIntent(data:ByteArray, start:Int, recordParents: Boolean = true):CodecCallerIntent {
    return if (recordParents) object: CodecCallerIntent, CodecRecordParents, DecodeFromBytes,DecodeHead {
        override val parents: Deque<Tag<out Any>> = ArrayDeque()
        override val data: ByteArray = data
        override val decodeHead: Boolean = true
        override val ignoreIdWhenDecoding: Boolean = false
        var _pointer:Int = start
        override var pointer: Int
            get() = _pointer
            set(value) { _pointer=value}
    } else object:CodecCallerIntent, DecodeFromBytes,DecodeHead {
        override val data: ByteArray = data
        override val decodeHead: Boolean = true
        override val ignoreIdWhenDecoding: Boolean = false
        var _pointer:Int = start
        override var pointer: Int
            get() = _pointer
            set(value) { _pointer=value}
    }
}

fun toProxyIntent(hasHead: Boolean, parents: Deque<Tag<out Any>>,
                  desId: Byte, inputStream: InputStream, ignoreIdWhenDecoding: Boolean = false):CodecCallerIntent {
    return object:CodecRecordParents,DecodeOnStream,SpecifyIdWhenDecoding,DecodeHead{
        override val parents: Deque<Tag<out Any>> = parents
        override val inputStream: InputStream = inputStream
        override val id: Byte = desId
        override val decodeHead: Boolean = hasHead
        override val ignoreIdWhenDecoding: Boolean = ignoreIdWhenDecoding
    }
}

fun proxyDecodeFromBytesIntent(hasHead: Boolean, parents: Deque<Tag<out Any>>,
                               desId: Byte, intent:DecodeFromBytes):CodecCallerIntent {
    return object:CodecRecordParents,SpecifyIdWhenDecoding,DecodeFromBytes,DecodeHead {
        override val parents: Deque<Tag<out Any>> = parents
        override val id: Byte = desId
        override val data: ByteArray = intent.data
        override val decodeHead: Boolean = hasHead
        override val ignoreIdWhenDecoding: Boolean = false
        override var pointer: Int
            get() = intent.pointer
            set(value) { intent.pointer=value}
    }
}


fun toProxyIntent(hasHead: Boolean, parents: Deque<Tag<out Any>>, outputStream: OutputStream):OnStreamToDelegatorEncodeIntent {
    return object:OnStreamToDelegatorEncodeIntent {
        override val parents: Deque<Tag<out Any>> = parents
        override val outputStream: OutputStream = outputStream
        override val encodeHead:Boolean = hasHead
    }
}

fun toProxyEncodeToBytesIntent(hasHead: Boolean, parents: Deque<Tag<out Any>>):OnBytesToProxyEncodeIntent {
    return object:OnBytesToProxyEncodeIntent {
        override val parents: Deque<Tag<out Any>> = parents
        override val encodeHead:Boolean = hasHead
    }
}


