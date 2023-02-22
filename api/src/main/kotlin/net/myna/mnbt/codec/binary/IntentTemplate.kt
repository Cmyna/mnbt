package net.myna.mnbt.codec.binary

import net.myna.mnbt.Tag
import net.myna.mnbt.codec.*
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Proxy
import java.util.*
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter


fun userEncodeIntent(outputStream: OutputStream): EncodeIntent {
    return object: EncodeHead, EncodeOnStream, RecordParentsWhenEncoding {
        override val parents: Deque<Tag<out Any>> = ArrayDeque()
        override val outputStream: OutputStream = outputStream
        override val encodeHead:Boolean = true
    }
}

fun userDecodeIntent(inputStream: InputStream): DecodeIntent {
    return object: DecodeOnStream, DecodeHead, DecodeTreeDepth {
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
fun userOnBytesEncodeIntent(): EncodeIntent {
    return object: EncodeHead, EncodeToBytes, RecordParentsWhenEncoding {
        override val parents: Deque<Tag<out Any>> = ArrayDeque()
        override val encodeHead:Boolean = true
    }
}

@JvmOverloads
fun userOnBytesDecodeIntent(data:ByteArray, start:Int, recordParents: Boolean = true): DecodeIntent {
    return if (recordParents) object: CodecCallerIntent, RecordParentsWhenEncoding, DecodeFromBytes, DecodeHead {
        override val parents: Deque<Tag<out Any>> = ArrayDeque()
        override val data: ByteArray = data
        override val ignoreIdWhenDecoding: Boolean = false
        var _pointer:Int = start
        override var pointer: Int
            get() = _pointer
            set(value) { _pointer=value}
    } else object: CodecCallerIntent, DecodeFromBytes, DecodeHead {
        override val data: ByteArray = data
        override val ignoreIdWhenDecoding: Boolean = false
        var _pointer:Int = start
        override var pointer: Int
            get() = _pointer
            set(value) { _pointer=value}
    }
}

@JvmOverloads
fun toProxyIntent(intent: DecodeIntent, decodeHead: Boolean, desId: Byte, ignoreIdWhenDecoding: Boolean = false): DecodeIntent {
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


fun proxyDecodeFromBytesIntent(decodeHead: Boolean, desId: Byte, intent: DecodeFromBytes): DecodeIntent {
    val interfaces = intent::class.java.interfaces.toMutableSet()
    if (decodeHead) interfaces.add(DecodeHead::class.java)
    else interfaces.remove(DecodeHead::class.java)
    if (intent !is SpecifyIdWhenDecoding) interfaces.add(SpecifyIdWhenDecoding::class.java)

    return Proxy.newProxyInstance(intent::class.java.classLoader, interfaces.toTypedArray()) { _,method,args->
        return@newProxyInstance when(method) {
            SpecifyIdWhenDecoding::id.javaGetter -> desId
            DecodeHead::ignoreIdWhenDecoding.javaGetter -> false
            DecodeFromBytes::pointer.javaGetter -> intent.pointer
            DecodeFromBytes::pointer.javaSetter -> intent.pointer = args.first() as Int
            else -> method.invoke(intent, *args.orEmpty())
        }
    } as DecodeIntent
}


fun toProxyIntent(hasHead: Boolean, intent: EncodeIntent): EncodeIntent {
    return Proxy.newProxyInstance(intent::class.java.classLoader, intent::class.java.interfaces) { _, method, args->
        return@newProxyInstance when (method) {
            EncodeHead::encodeHead.javaGetter -> hasHead
            else -> method.invoke(intent, *args.orEmpty())
        }
    } as EncodeIntent
}


