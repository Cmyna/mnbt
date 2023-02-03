package com.myna.mnbt.codec

import com.myna.mnbt.Tag
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Proxy
import java.util.*
import kotlin.reflect.jvm.javaGetter

//TODO: change all intent creation called from codec to dynamic proxy

fun userEncodeIntent(outputStream: OutputStream):OnStreamToDelegatorEncodeIntent {
    return object:OnStreamToDelegatorEncodeIntent {
        override val parents: Deque<Tag<out Any>> = ArrayDeque()
        override val outputStream: OutputStream = outputStream
        override val encodeHead:Boolean = true
    }
}

fun userDecodeIntent(inputStream: InputStream):CodecCallerIntent {
    return object:DecodeOnStream,DecodeHead,DecodeTreeDepth {
        override val inputStream = inputStream
        override val ignoreIdWhenDecoding: Boolean = false

        var _depth:Int = 0
        override var depth: Int
            get() = _depth
            set(value) {_depth = value}
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
    return if (recordParents) object: CodecCallerIntent, RecordParentsWhenEncoding, DecodeFromBytes,DecodeHead {
        override val parents: Deque<Tag<out Any>> = ArrayDeque()
        override val data: ByteArray = data
        override val ignoreIdWhenDecoding: Boolean = false
        var _pointer:Int = start
        override var pointer: Int
            get() = _pointer
            set(value) { _pointer=value}
    } else object:CodecCallerIntent, DecodeFromBytes,DecodeHead {
        override val data: ByteArray = data
        override val ignoreIdWhenDecoding: Boolean = false
        var _pointer:Int = start
        override var pointer: Int
            get() = _pointer
            set(value) { _pointer=value}
    }
}

fun toProxyIntent(intent:DecodeIntent, decodeHead: Boolean, desId: Byte, ignoreIdWhenDecoding: Boolean = false):CodecCallerIntent {
    val interfaces = intent::class.java.interfaces.toMutableSet()
    interfaces.add(SpecifyIdWhenDecoding::class.java)
    if (decodeHead) interfaces.add(DecodeHead::class.java)
    else interfaces.remove(DecodeHead::class.java)
    return Proxy.newProxyInstance(
            intent::class.java.classLoader,
            interfaces.toTypedArray()
    ) {
        _,method,args->
        when (method) {
            DecodeHead::ignoreIdWhenDecoding.javaGetter -> return@newProxyInstance ignoreIdWhenDecoding
            SpecifyIdWhenDecoding::id.javaGetter -> return@newProxyInstance desId
            else -> return@newProxyInstance method.invoke(intent, *args.orEmpty())
        }
    } as DecodeIntent
}

fun proxyDecodeFromBytesIntent(decodeHead: Boolean, parents: Deque<Tag<out Any>>,
                               desId: Byte, intent:DecodeFromBytes):CodecCallerIntent {
    if (decodeHead) return object:RecordParentsWhenEncoding,SpecifyIdWhenDecoding,DecodeFromBytes,DecodeHead {
        override val parents: Deque<Tag<out Any>> = parents
        override val id: Byte = desId
        override val data: ByteArray = intent.data
        override val ignoreIdWhenDecoding: Boolean = false
        override var pointer: Int
            get() = intent.pointer
            set(value) { intent.pointer=value}
    } else return object:RecordParentsWhenEncoding,SpecifyIdWhenDecoding,DecodeFromBytes {
        override val parents: Deque<Tag<out Any>> = parents
        override val id: Byte = desId
        override val data: ByteArray = intent.data
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


