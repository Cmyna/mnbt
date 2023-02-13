package com.myna.mnbt

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
import com.myna.mnbt.utils.UnsupportedProperty
import java.io.InputStream
import java.io.OutputStream
import java.lang.NullPointerException

//TODO: Exception analyse, refactoring and handling

@Suppress("UnstableApiUsage")
open class Mnbt {



    /**
     * serialize value to Nbt binary data stored in a ByteArray(byte[])
     *
     * if value is a Tag, then it will be encapsulated by an compound Tag with name passed in,
     * then serialize to bytes,
     * else value with value will first converted to Tag then serialized to bytes
     * @param name name of nbt root tag, see [varTopTagName]
     * @param value the value want to serialize, see [paramJavaObject]
     * @param typeToken extra typeToken info for value (if ignore this parameter, TypeToken will use value.class as TypeTokenInfo)
     * see [paramTypeToken]
     * @return a ByteArray stores data starts at 0 ends at ByteArray.size
     * @throws ConverterNullResultException if result of value->tag is null
     */
    fun <V:Any> toBytes(name:String, value:V, typeToken: MTypeToken<out V> = MTypeToken.of(value::class.java)):ByteArray {
        if (value is Tag<*>) {
            return (onByteCodecProxy.encode(encapsulatedWithCompoundTag(name, value), userOnBytesEncodeIntent()) as EncodedBytesFeedback).bytes
        }
        val tag = converterProxy.createTag(name, value, typeToken, createTagUserIntent())
                ?: throw ConverterNullResultException(value)
        return (onByteCodecProxy.encode(tag, userOnBytesEncodeIntent()) as EncodedBytesFeedback).bytes
    }

    /**
     * deserialize nbt binary data from a ByteArray starts at pointer passed in
     * @param bytes the ByteArray stores nbt data
     * @param start pointer to the nbt data
     * @param typeToken see [paramTypeToken]
     * if null it will return default type
     * @return a pair that first element stores root tag name, second one stores deserialized tag value
     */
    fun <V:Any> fromBytes(bytes:ByteArray, start:Int, typeToken: MTypeToken<out V>? = null):Pair<String, V>? {
        val actualTypeToken = typeToken?:MTypeToken.of(Any::class.java)
        val feedback = onByteCodecProxy.decode(userOnBytesDecodeIntent(bytes, start))
        val converterCallerIntent = if (typeToken == null)  converterCallerIntent(true) else converterCallerIntent(false)
        val res = converterProxy.toValue(feedback.tag, actualTypeToken, converterCallerIntent)
        if (res?.first==null) throw NullPointerException()
        return res as Pair<String, V>? // runtime check
    }

    /**
     * encode the object to binary nbt format, and write encoded result to an [OutputStream]
     * @param name the root tag name, see [varTopTagName]
     * @param value the object to encoded, see [paramJavaObject]
     * @param typeToken see [paramTypeToken]
     * @param outputStream see [paramBinOutputStream]
     * @return return encode process success or not
     */
    fun <V:Any> toStream(name:String, value:V, typeToken: MTypeToken<out V>, outputStream:OutputStream):Boolean {
        if (value is Tag<*>) {
            codecProxy.encode(encapsulatedWithCompoundTag(name, value), userEncodeIntent(outputStream))
            return true
        }
        val tag = converterProxy.createTag(name, value, typeToken, createTagUserIntent())
                ?: throw ConverterNullResultException(value)
        codecProxy.encode(tag, userEncodeIntent(outputStream))
        return true
    }

    /**
     * decode binary nbt data from an [InputStream], and convert it to an object with specified object type
     * @param typeToken @see [paramTypeToken]
     * @param inputStream see [paramBinInputStream]
     * @param converterIntent TODO
     * @return see [returnFromTagResult]
     */
    fun <V:Any> fromStream(typeToken: MTypeToken<out V>, inputStream: InputStream, converterIntent: ToValueIntent?=null):Pair<String?, V>? {
        val stream = if (adaptedInputStream) AdaptedInputStream(inputStream) else inputStream
        val feedback = codecProxy.decode(userDecodeIntent(stream))
        return if (converterIntent!=null) converterProxy.toValue(feedback.tag, typeToken, converterIntent)
        else converterProxy.toValue(feedback.tag, typeToken)
    }

    /**
     * encode a [Tag] as binary nbt format to an [OutputStream]
     * @param tag the [Tag] want to encode
     * @param outputStream see [paramBinOutputStream]
     */
    fun encode(tag:Tag<out Any>, outputStream: OutputStream) {
        codecProxy.encode(tag, userEncodeIntent(outputStream))
    }

    /**
     * encode a [Tag] to an byte array
     * @param tag the [Tag] want to encode
     * @return a byte array stores encoded result starts at index 0, ends at byte array's end
     */
    fun encode(tag:Tag<out Any>):ByteArray {
        return (onByteCodecProxy.encode(tag, userOnBytesEncodeIntent()) as EncodedBytesFeedback).bytes
    }

    /**
     * decode a tag from an [InputStream]
     * @param inputStream see [paramBinInputStream]
     * @return decoded [Tag]
     */
    fun decode(inputStream: InputStream):Tag<out Any> {
        return codecProxy.decode(userDecodeIntent(inputStream)).tag
    }

    /**
     * decode a byte array
     * @start specifies where the nbt binary data starts
     * @return the decoded result
     */
    fun decode(bytes: ByteArray, start:Int):Tag<out Any> {
        return onByteCodecProxy.decode(userOnBytesDecodeIntent(bytes, start)).tag
    }

    /**
     * convert a java object to [Tag]
     * @param name see [varTopTagName]
     * @param value see [paramJavaObject]
     * @param typeToken see [paramTypeToken]
     * @return convert result [Tag]
     */
    fun <V:Any> toTag(name:String?, value:V, typeToken: MTypeToken<out V>):Tag<out Any>? {
        return converterProxy.createTag(name, value, typeToken, createTagUserIntent())
    }

    /**
     * convert a [Tag] to an java object with specified type
     * @param tag a [Tag] object
     * @param typeToken see [paramTypeToken]
     * @return see [returnFromTagResult]
     */
    fun <V:Any> fromTag(tag:Tag<out Any>, typeToken: MTypeToken<out V>):Pair<String?,V>? {
        return converterProxy.toValue(tag, typeToken)
    }

    fun <V:Any> overrideTag(value:V, typeToken: MTypeToken<out V>, targetTag:Tag<out Any>):Tag<out Any>? {
        // for flat tag, it just create a new tag and use value from parameter, use name from target tag
        // so no extra code to reach functionality of override
        // problem is need to check tag type is equals or not
        val res = converterProxy.createTag(targetTag.name, value, typeToken, overrideTagUserIntent(targetTag))
        // check top tag type
        return if (res?.id != targetTag.id) null
        else res
    }

    /**
     * doc:TODO
     */
    fun registerConverter(converter:TagConverter<Any>):Boolean {
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


    // options start

    /**
     * This options is experimental and may be removed in the future!
     *
     * Specifies that use an AdaptedInputStream that inherit and encapsulate all functionality from the original InputStream
     * and optimize its performance by refactor its method code
     */
    var adaptedInputStream:Boolean = false

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

    /**
     * override part of list if override target is an list tag.
     * Converters will only override value in source index to target list tag index element
     */
    var completeOverride: Boolean
        get() = ListConverters.completeOverride
        set(value) {ListConverters.completeOverride = value}


    // options end




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

    protected open val arrayTypeListTagConverter = ListConverters.ArrayTypeListTagConverter(this.converterProxy)
    protected open val listTypeConverter = ListConverters.IterableTypeConverter(this.converterProxy)
    protected open val reflectiveConverter = ReflectiveConverter(this.converterProxy)
    protected open val mapTypeTagConverter = MapTypeConverter(this.converterProxy)

    protected open val listCodec = BinaryCodecInstances.ListTagCodec(this.codecProxy)
    protected open val compoundTagCodec = BinaryCodecInstances.CompoundTagCodec(this.codecProxy)

    protected open val onByteListCodec = ListTagCodec(this.onByteCodecProxy)
    protected open val onByteCompoundTagCodec = CompoundTagCodec(this.onByteCodecProxy)

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

        this.converterProxy.registerToLast(reflectiveConverter)
    }

    companion object {

        /**
         * a [paramJavaObject] class type token, presented by [MTypeToken]
         */
        private val paramTypeToken:Byte by UnsupportedProperty

        /**
         * the [InputStream] that contains Nbt binary data
         */
        private val paramBinInputStream:Byte by UnsupportedProperty

        /**
         * the [OutputStream] for writing Nbt binary data
         */
        private val paramBinOutputStream:Byte by UnsupportedProperty

        /**
         * a [String] value represent top tag name in an Nbt structure
         */
        private val varTopTagName:Byte by UnsupportedProperty

        /**
         * an Object or primitive value wants to encode or decode
         */
        private val paramJavaObject by UnsupportedProperty

        /**
         * a pair that first element stores the top tag name, second one
         * stores the tag value converted to specified type (from [paramTypeToken])
         */
        private val returnFromTagResult by UnsupportedProperty

    }

}