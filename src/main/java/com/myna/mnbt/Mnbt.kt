package com.myna.mnbt

import com.google.common.reflect.TypeToken
import com.myna.mnbt.converter.*
import com.myna.utils.AdaptedInputStream
import com.myna.mnbt.tag.CompoundTag
import com.myna.mnbt.exceptions.ConverterNullResultException
import com.myna.mnbt.codec.*
import com.myna.mnbt.experiment.CompoundTagCodec
import com.myna.mnbt.experiment.FlatCodeses
import com.myna.mnbt.experiment.ListTagCodec
import com.myna.mnbt.experiment.OnBytesCodecProxy
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.AnyCompound
import java.io.InputStream
import java.io.OutputStream
import java.lang.NullPointerException
import java.util.ArrayDeque

@Suppress("UnstableApiUsage")
open class Mnbt {
    protected val converterProxy:DefaultConverterProxy = DefaultConverterProxy()
    protected val codecProxy:DefaultCodecProxy = DefaultCodecProxy(BinaryCodecInstances.intCodec, BinaryCodecInstances.shortCodec,
            BinaryCodecInstances.byteCodec, BinaryCodecInstances.longCodec,
            BinaryCodecInstances.floatCodec, BinaryCodecInstances.doubleCodec,
            BinaryCodecInstances.stringCodec, BinaryCodecInstances.intArrayCodec,
            BinaryCodecInstances.byteArrayCodec, BinaryCodecInstances.longArrayCodec,
            BinaryCodecInstances.nullTagCodec)

    protected val onByteCodecProxy:OnBytesCodecProxy = OnBytesCodecProxy(
            FlatCodeses.intCodec, FlatCodeses.shortCodec,
            FlatCodeses.byteCodec, FlatCodeses.longCodec,
            FlatCodeses.floatCodec, FlatCodeses.doubleCodec,
            FlatCodeses.stringCodec, FlatCodeses.intArrayCodec,
            FlatCodeses.byteArrayCodec, FlatCodeses.longArrayCodec,
    )

    /**
     * doc: TODO
     */
    var autoAdaptedStream:Boolean = false

    /**
     * set convert object that has nullable properties or not. in default the value is true
     *
     * if value is true, object returned from deserialize/fromBytes may have some null properties that Mnbt can not handle
     *
     * if value is false, object returned from deserialize/fromByte will be null if Mnbt can not handle some properties in returned object
     * (can not converted to field expected type or can not access field)
     */
    var returnObjectContainsNullableProperties:Boolean
        get() = reflectiveConverter.returnObjectWithNullableProperties
        set(b) {reflectiveConverter.returnObjectWithNullableProperties = b}


    protected val arrayTypeListTagConverter = ListConverters.ArrayTypeListTagConverter(this.converterProxy)
    protected val listTypeConverter = ListConverters.IterableTypeConverter(this.converterProxy)
    protected val reflectiveConverter = ReflectiveConverter(this.converterProxy)
    protected val annotationHandlerConverter = AnnotationHandlerConverter(this.converterProxy)
    protected val mapTypeTagConverter = MapConverters.MapTypeConverter(this.converterProxy)

    protected val listCodec = BinaryCodecInstances.ListTagCodec(this.codecProxy)
    protected val compoundTagCodec = BinaryCodecInstances.CompoundTagCodec(this.codecProxy)

    protected val onByteListCodec = ListTagCodec(this.onByteCodecProxy)
    protected val onByteCompoundTagCodec = CompoundTagCodec(this.onByteCodecProxy)

    /**
     * serialize value to Nbt binary data stored in a ByteArray(byte[])
     *
     * if value is a Tag, then it will be encapsulated by an compound Tag with name passed in,
     * then serialize to bytes,
     * else value with value will first converted to Tag then serialized to bytes
     * @param name name of nbt root tag
     * @param value the value want to serialize
     * @param typeToken extra typeToken info for value (if ignore this parameter, TypeToken will use value.class as TypeTokenInfo)
     * @return a ByteArray stores data starts at 0 ends at ByteArray.size
     * @throws ConverterNullResultException if result of value->tag is null
     */
    fun <V:Any> toBytes(name:String, value:V, typeToken: MTypeToken<out V> = MTypeToken.of(value::class.java)):ByteArray {
        if (value is Tag<*>) {
            return (onByteCodecProxy.encode(encapsulatedWithCompoundTag(name, value), userOnBytesEncodeIntent()) as EncodedBytesFeedback).bytes
        }
        val tag = converterProxy.createTag(name, value, typeToken)
                ?: throw ConverterNullResultException(value)
        return (onByteCodecProxy.encode(tag, userOnBytesEncodeIntent()) as EncodedBytesFeedback).bytes
    }

    /**
     * deserialize nbt binary data from a ByteArray starts at pointer passed in
     * @param bytes the ByteArray stores nbt data
     * @param start pointer to the nbt data
     * @param typeToken the returned value type converted from Nbt data,
     * if null it will return default type
     * @return a pair that first element stores root tag name, second one stores deserialized tag value
     */
    fun <V:Any> fromBytes(bytes:ByteArray, start:Int, typeToken: MTypeToken<out V>? = null):Pair<String, V>? {
        val actualTypeToken = typeToken?:MTypeToken.of(Any::class.java)
        val feedback = onByteCodecProxy.decode(userOnBytesDecodeIntent(bytes, start))
        val converterCallerIntent = if (typeToken == null)  nestCIntent(ArrayDeque(), true) else nestCIntent(ArrayDeque(), false)
        val res = converterProxy.toValue(feedback.tag, actualTypeToken, converterCallerIntent)
        if (res?.first==null) throw NullPointerException()
        return res as Pair<String, V>? // runtime check
    }

    /**
     * doc:TODO
     */
    fun <V:Any> toStream(name:String, value:V, typeToken: MTypeToken<out V>, outputStream:OutputStream):Boolean {
        if (value is Tag<*>) {
            codecProxy.encode(encapsulatedWithCompoundTag(name, value), userEncodeIntent(outputStream))
            return true
        }
        val tag = converterProxy.createTag(name, value, typeToken)
                ?: throw ConverterNullResultException(value)
        codecProxy.encode(tag, userEncodeIntent(outputStream))
        return true
    }

    /**
     * doc:TODO
     */
    fun <V:Any> fromStream(typeToken: MTypeToken<out V>, inputStream: InputStream, converterIntent: ConverterCallerIntent?=null):Pair<String?, V>? {
        val stream = if (autoAdaptedStream) AdaptedInputStream(inputStream) else inputStream
        val feedback = codecProxy.decode(userDecodeIntent(stream))
        return if (converterIntent!=null) converterProxy.toValue(feedback.tag, typeToken, converterIntent)
        else converterProxy.toValue(feedback.tag, typeToken)
    }

    /**
     * doc:TODO
     */
    fun encode(tag:Tag<out Any>, outputStream: OutputStream) {
        codecProxy.encode(tag, userEncodeIntent(outputStream))
    }

    /**
     * doc:TODO
     */
    fun encode(tag:Tag<out Any>):ByteArray {
        return (onByteCodecProxy.encode(tag, userOnBytesEncodeIntent()) as EncodedBytesFeedback).bytes
    }

    /**
     * doc:TODO
     */
    fun decode(inputStream: InputStream):Tag<out Any> {
        return codecProxy.decode(userDecodeIntent(inputStream)).tag
    }

    /**
     * doc:TODO
     */
    fun decode(bytes: ByteArray, start:Int):Tag<out Any> {
        return onByteCodecProxy.decode(userOnBytesDecodeIntent(bytes, start)).tag
    }

    /**
     * doc: TODO
     */
    fun <V:Any> toTag(name:String, value:V, typeToken: MTypeToken<out V>):Tag<out Any>? {
        return converterProxy.createTag(name, value, typeToken)
    }

    /**
     * doc: TODO
     */
    fun <V:Any> fromTag(tag:Tag<out Any>, typeToken: MTypeToken<out V>):Pair<String?,V>? {
        return converterProxy.toValue(tag, typeToken)
    }

    /**
     * doc:TODO
     */
    fun registerConverter(converter:TagConverter<Any, out ConverterCallerIntent>):Boolean {
        return this.converterProxy.registerToFirst(converter)
    }

    /**
     * doc:TODO
     */
    fun registerCodec(codec:Codec<Any>):Boolean {
        return this.codecProxy.registerCodec(codec)
    }

    private fun encapsulatedWithCompoundTag(name:String, value:Tag<*>):Tag<AnyCompound> {
        val compound = CompoundTag(name)
        compound.add(value as Tag<out Any>)
        return compound
    }

    init {
        this.codecProxy.registerCodec(listCodec)
        this.codecProxy.registerCodec(compoundTagCodec)
        this.onByteCodecProxy.registerCodec(onByteCompoundTagCodec)
        this.onByteCodecProxy.registerCodec(onByteListCodec)
        converterProxy.also {
            it.registerToFirst(ExcluderConverter.instance)
            it.registerToLast(TagConverters.booleanConverter)
            it.registerToLast(TagConverters.intTagConverter)
            it.registerToLast(TagConverters.byteTagConverter)
            it.registerToLast(TagConverters.shortTagConverter)
            it.registerToLast(TagConverters.longTagConverter)
            it.registerToLast(TagConverters.floatTagConverter)
            it.registerToLast(TagConverters.doubleTagConverter)
            it.registerToLast(TagConverters.stringTagConverter)

            it.registerToLast(TagConverters.intArrayConverter)
            it.registerToLast(TagConverters.byteArrayConverter)
            it.registerToLast(TagConverters.longArrayConverter)
        }

        this.converterProxy.registerToLast(arrayTypeListTagConverter)
        this.converterProxy.registerToLast(listTypeConverter)
        this.converterProxy.registerToLast(mapTypeTagConverter)

        this.converterProxy.registerToLast(annotationHandlerConverter)
        this.converterProxy.registerToLast(reflectiveConverter)
    }

}