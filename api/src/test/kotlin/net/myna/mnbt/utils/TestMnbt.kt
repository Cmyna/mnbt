package net.myna.mnbt.utils

import net.myna.mnbt.*
import net.myna.mnbt.codec.*
import net.myna.mnbt.experiment.CompoundTagCodec
import net.myna.mnbt.experiment.FlatCodeses
import net.myna.mnbt.experiment.ListTagCodec
import net.myna.mnbt.experiment.OnBytesCodecProxy

open class TestMnbt: Mnbt() {

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

    override fun decode(bytes: ByteArray, start:Int, decodeIntent: DecodeIntent?): Tag<out Any> {
        return onByteCodecProxy.decode(userOnBytesDecodeIntent(bytes, start)).tag
    }

    override fun encode(tag:Tag<out Any>, encodeIntent: EncodeIntent?):ByteArray {
        return (onByteCodecProxy.encode(tag, userOnBytesEncodeIntent()) as EncodedBytesFeedback).bytes
    }

    protected val onByteCodecProxy: OnBytesCodecProxy = OnBytesCodecProxy()

    protected val onByteListCodec = ListTagCodec(this.onByteCodecProxy)
    protected val onByteCompoundTagCodec = CompoundTagCodec(this.onByteCodecProxy)


    init {
        onByteCodecProxy.registerCodec(FlatCodeses.intCodec, IdTagInt)
        onByteCodecProxy.registerCodec(FlatCodeses.shortCodec, IdTagShort)
        onByteCodecProxy.registerCodec(FlatCodeses.byteCodec, IdTagByte)
        onByteCodecProxy.registerCodec(FlatCodeses.longCodec, IdTagLong)
        onByteCodecProxy.registerCodec(FlatCodeses.floatCodec, IdTagFloat)
        onByteCodecProxy.registerCodec(FlatCodeses.doubleCodec, IdTagDouble)
        onByteCodecProxy.registerCodec(FlatCodeses.stringCodec, IdTagString)
        onByteCodecProxy.registerCodec(FlatCodeses.intArrayCodec, IdTagIntArray)
        onByteCodecProxy.registerCodec(FlatCodeses.byteArrayCodec, IdTagByteArray)
        onByteCodecProxy.registerCodec(FlatCodeses.longArrayCodec, IdTagLongArray)

        super.reflectiveConverter.proxy = mockConverterProxy
        super.arrayTypeListTagConverter.proxy = mockConverterProxy
        super.mapTypeTagConverter.proxy = mockConverterProxy
        super.listTypeConverter.proxy = mockConverterProxy

        super.listCodec.proxy = mockCodecProxy
        super.compoundTagCodec.proxy = mockCodecProxy

        super.reflectiveConverter.printStacktrace = true

        this.onByteCodecProxy.registerCodec(onByteCompoundTagCodec, IdTagCompound)
        this.onByteCodecProxy.registerCodec(onByteListCodec, IdTagList)
    }

    companion object {
        val inst = TestMnbt()
    }
}