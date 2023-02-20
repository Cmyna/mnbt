package mnbt.utils

import com.myna.mnbt.Mnbt
import com.myna.mnbt.Tag
import com.myna.mnbt.codec.EncodedBytesFeedback
import com.myna.mnbt.codec.userOnBytesDecodeIntent
import com.myna.mnbt.codec.userOnBytesEncodeIntent
import com.myna.mnbt.converter.DefaultConverterProxy
import com.myna.mnbt.converter.TagConverter
import mnbt.experiment.CompoundTagCodec
import mnbt.experiment.FlatCodeses
import mnbt.experiment.ListTagCodec
import mnbt.experiment.OnBytesCodecProxy
import kotlin.reflect.full.memberProperties

class TestMnbt: Mnbt() {

    val mockConverterProxy = MockConverterProxy(super.converterProxy)
    val mockCodecProxy = MockCodecProxy(super.codecProxy)

    val refConverterProxy = super.converterProxy
    val refCodecProxy = super.codecProxy
    val refReflectiveConverter = super.reflectiveConverter
    val refMapConverter = super.mapTypeTagConverter
    val refArrayToListConverter = super.arrayTypeListTagConverter
    val refListConverter = super.listTypeConverter

    val refCompoundCodec = super.compoundTagCodec
    val refListCodec = super.listCodec

    override fun decode(bytes: ByteArray, start:Int): Tag<out Any> {
        return onByteCodecProxy.decode(userOnBytesDecodeIntent(bytes, start)).tag
    }

    override fun encode(tag:Tag<out Any>):ByteArray {
        return (onByteCodecProxy.encode(tag, userOnBytesEncodeIntent()) as EncodedBytesFeedback).bytes
    }

    protected val onByteCodecProxy: OnBytesCodecProxy = OnBytesCodecProxy(
    FlatCodeses.intCodec, FlatCodeses.shortCodec,
    FlatCodeses.byteCodec, FlatCodeses.longCodec,
    FlatCodeses.floatCodec, FlatCodeses.doubleCodec,
    FlatCodeses.stringCodec, FlatCodeses.intArrayCodec,
    FlatCodeses.byteArrayCodec, FlatCodeses.longArrayCodec,
    )

    protected val onByteListCodec = ListTagCodec(this.onByteCodecProxy)
    protected val onByteCompoundTagCodec = CompoundTagCodec(this.onByteCodecProxy)


    init {
        super.reflectiveConverter.proxy = mockConverterProxy
        super.arrayTypeListTagConverter.proxy = mockConverterProxy
        super.mapTypeTagConverter.proxy = mockConverterProxy
        super.listTypeConverter.proxy = mockConverterProxy

        super.listCodec.proxy = mockCodecProxy
        super.compoundTagCodec.proxy = mockCodecProxy

        super.reflectiveConverter.printStacktrace = true

        this.onByteCodecProxy.registerCodec(onByteCompoundTagCodec)
        this.onByteCodecProxy.registerCodec(onByteListCodec)
    }

    companion object {
        val inst = TestMnbt()
    }
}