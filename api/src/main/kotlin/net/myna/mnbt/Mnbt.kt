package net.myna.mnbt

import net.myna.mnbt.converter.*
import net.myna.mnbt.tag.CompoundTag
import net.myna.mnbt.exceptions.ConverterNullResultException
import net.myna.mnbt.codec.*
import net.myna.mnbt.codec.binary.*
import net.myna.mnbt.exceptions.CircularReferenceException
import net.myna.mnbt.reflect.MTypeToken
import net.myna.mnbt.utils.UnsupportedProperty
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.NullPointerException

//TODO: Exception analyse, refactoring and handling
// should throw to user readable Exceptions, includes where comes the Exceptions, nbt/object stacktrace
//TODO: ensure Java usability
//TODO: let InputStream passed in support mark methods(may use BufferedInputStream)
open class Mnbt {

    @JvmOverloads
    /**
     * serialize value to Nbt binary data stored in a ByteArray(byte[])
     *
     * if value is a Tag, then it will be encapsulated by a compound Tag with name passed in,
     * then serialize to bytes,
     * else value with value will first convert to Tag then serialized to bytes
     * @param name name of nbt root tag, see [varTopTagName]
     * @param value the value want to serialize, see [paramJavaObject]
     * @param typeToken extra typeToken info for value (if ignore this parameter, TypeToken will use [value] class as TypeTokenInfo)
     * see [paramTypeToken]
     * @param createTagIntent see [optionalParamCreateTagIntent]
     * @param encodeIntent see [optionalParamEncodeIntent]
     * @return a ByteArray stores data starts at 0 ends at ByteArray.size
     * @throws ConverterNullResultException if result of value->tag is null
     * @throws CircularReferenceException if circular reference is found
     */
    open fun <V:Any> toBytes(
        name:String, value:V,
        typeToken: MTypeToken<out V> = MTypeToken.of(value::class.java),
        createTagIntent: CreateTagIntent? = null,
        encodeIntent: EncodeIntent? = null
    ):ByteArray {
        val outputStream = ByteArrayOutputStream()
        toStream(name, value, typeToken, outputStream, createTagIntent, encodeIntent)
        return outputStream.toByteArray()
    }

    @JvmOverloads
    /**
     * deserialize nbt binary data from a ByteArray starts at pointer passed in
     * @param bytes the ByteArray stores nbt data
     * @param start pointer to the nbt data
     * @param typeToken see [paramTypeToken]
     * if null it will return default type
     * @param decodeIntent see [optionalParamDecodeIntent]
     * @param toValueIntent see [optionalParamToValueIntent]
     * @return a pair that first element stores root tag name, second one stores deserialized tag value
     * @throws IndexOutOfBoundsException if [start] out of [bytes] index range
     */
    open fun <V:Any> fromBytes(
        bytes: ByteArray,
        start: Int,
        typeToken: MTypeToken<out V>? = null,
        decodeIntent: DecodeIntent? = null,
        toValueIntent: ToValueIntent? = null
    ):Pair<String?, V>? {
        if (start !in 0..bytes.size) throw IndexOutOfBoundsException("parameter start ($start) out of byte array range!")
        val inputStream = ByteArrayInputStream(bytes.copyOfRange(start, bytes.size))
        return fromStream(inputStream, typeToken, decodeIntent, toValueIntent)
    }

    @JvmOverloads
    /**
     * encode the object to binary nbt format, and write encoded result to an [OutputStream]
     * @param name the root tag name, see [varTopTagName]
     * @param value the object to encoded, see [paramJavaObject]
     * @param typeToken see [paramTypeToken]
     * @param createTagIntent see [optionalParamCreateTagIntent]
     * @param encodeIntent see [optionalParamEncodeIntent]
     * @param outputStream see [paramBinOutputStream]
     * @return return encode process success or not
     * @throws ConverterNullResultException if result of value->tag is null
     * @throws CircularReferenceException if circular reference is found
     */
    open fun <V:Any> toStream(
        name: String,
        value: V,
        typeToken: MTypeToken<out V>,
        outputStream: OutputStream,
        createTagIntent: CreateTagIntent? = null,
        encodeIntent: EncodeIntent? = null
    ):Boolean {
        if (value is Tag<*>) {
            codecProxy.encode(encapsulatedWithCompoundTag(name, value), userEncodeIntent(outputStream, encodeIntent))
            return true
        }
        val tag = converterProxy.createTag(name, value, typeToken, createTagUserIntent(createTagIntent))
                ?: throw ConverterNullResultException(value)
        codecProxy.encode(tag, userEncodeIntent(outputStream, encodeIntent))
        return true
    }

    @JvmOverloads
    @Suppress("UNCHECKED_CAST")
    /**
     * decode binary nbt data from an [InputStream], and convert it to an object with specified object type
     * @param typeToken (optional) see [paramTypeToken], if not pas typeToken then function will return value as default type
     * @param inputStream see [paramBinInputStream]
     * @param toValueIntent see [optionalParamToValueIntent]
     * @param decodeIntent see [optionalParamDecodeIntent]
     * @return see [returnFromTagResult]
     */
    open fun <V:Any> fromStream(
        inputStream: InputStream,
        typeToken: MTypeToken<out V>? = null,
        decodeIntent: DecodeIntent? = null,
        toValueIntent: ToValueIntent? = null
    ):Pair<String?, V>? {
        val actualTypeToken = typeToken?:MTypeToken.of(Any::class.java)
        val feedback = codecProxy.decode(userDecodeIntent(inputStream))
        val res = if (toValueIntent!=null) converterProxy.toValue(feedback.tag, actualTypeToken, toValueIntent)
        else converterProxy.toValue(feedback.tag, actualTypeToken)
        return if (res == null) null
        else {
            if (res.first == null) throw NullPointerException()
            res as Pair<String?, V>
        }
    }

    @JvmOverloads
    /**
     * encode a [Tag] as binary nbt format to an [OutputStream]
     * the function will auto override [EncodeOnStream],[EncodeHead],and [RecordParentsWhenEncoding] as encode intent
     * @param tag the [Tag] want to encode
     * @param outputStream see [paramBinOutputStream]
     * @param encodeIntent see [optionalParamEncodeIntent]
     * @throws CircularReferenceException if circular references is found in [tag]
     */
    open fun encode(tag:Tag<out Any>, outputStream: OutputStream, encodeIntent: EncodeIntent? = null) {
        val intent = userEncodeIntent(outputStream, encodeIntent)
        codecProxy.encode(tag, intent)
    }

    @JvmOverloads
    /**
     * encode a [Tag] to a byte array,
     * the function will auto override [EncodeOnStream],[EncodeHead],and [RecordParentsWhenEncoding] as encode intent
     * @param tag the [Tag] want to encode
     * @param encodeIntent see [optionalParamEncodeIntent]
     * @return a byte array stores encoded result starts at index 0, ends at byte array's end
     * @throws CircularReferenceException if circular references is found in [tag]
     */
    open fun encode(tag:Tag<out Any>, encodeIntent: EncodeIntent? = null):ByteArray {
        val outputStream = ByteArrayOutputStream()
        encode(tag, outputStream, encodeIntent)
        return outputStream.toByteArray()
    }

    @JvmOverloads
    /**
     * decode a tag from an [InputStream]
     * @param inputStream see [paramBinInputStream]
     * @param decodeIntent see [optionalParamDecodeIntent]
     * @return decoded [Tag]
     */
    open fun decode(inputStream: InputStream, decodeIntent: DecodeIntent? = null):Tag<out Any> {
        return codecProxy.decode(userDecodeIntent(inputStream, decodeIntent)).tag
    }

    @JvmOverloads
    /**
     * decode a byte array
     * @param bytes byte array contains nbt binary data
     * @param start specifies where the nbt binary data starts
     * @param decodeIntent see [optionalParamDecodeIntent]
     * @return the decoded result
     * @throws IndexOutOfBoundsException if [start] out of [bytes] index range
     */
    open fun decode(bytes: ByteArray, start:Int, decodeIntent: DecodeIntent? = null):Tag<out Any> {
        if (start !in 0..bytes.size) throw IndexOutOfBoundsException("parameter start ($start) out of byte array range!")
        val inputStream = ByteArrayInputStream(bytes.copyOfRange(start, bytes.size))
        return decode(inputStream, decodeIntent)
    }

    @JvmOverloads
    /**
     * convert a java object to [Tag]
     * @param name see [varTopTagName]
     * @param value see [paramJavaObject]
     * @param typeToken (optional) see [paramTypeToken]
     * @param createTagIntent see [optionalParamCreateTagIntent]
     * @return convert result [Tag]
     */
    fun <V:Any> toTag(
        name:String?, value:V,
        typeToken: MTypeToken<out V> = MTypeToken.of(value::class.java),
        createTagIntent: CreateTagIntent? = null
    ):Tag<out Any>? {
        return converterProxy.createTag(name, value, typeToken, createTagUserIntent(createTagIntent))
    }

    @JvmOverloads
    /**
     * convert a [Tag] to an java object with specified type
     * @param tag a [Tag] object
     * @param typeToken see [paramTypeToken]
     * @param toValueIntent see [optionalParamToValueIntent]
     * @return see [returnFromTagResult]
     */
    fun <V:Any> fromTag(tag:Tag<out Any>, typeToken: MTypeToken<out V>, toValueIntent: ToValueIntent? = null):Pair<String?,V>? {
        if (toValueIntent!=null) return converterProxy.toValue(tag, typeToken, toValueIntent)
        return converterProxy.toValue(tag, typeToken)
    }

    @JvmOverloads
    /**
     * @param value see [paramJavaObject]
     * @param typeToken see [paramTypeToken]
     * @param targetTag a [Tag] that will be overridden
     * @param overrideTagIntent (optional) an [CreateTagIntent], function will auto override [RecordParents] and [OverrideTag] interfaces
     * @return a new [Tag] that is combined from value and targetTag,
     * all same tag in [targetTag] nbt structure is overridden by value
     */
    fun <V:Any> overrideTag(value:V, typeToken: MTypeToken<out V>, targetTag:Tag<out Any>, overrideTagIntent: CreateTagIntent? = null):Tag<out Any>? {
        // for flat tag, it just creates a new tag and use value from parameter, use name from target tag
        // so no extra code to reach functionality of override
        // problem is need to check tag type is equals or not
        val res = converterProxy.createTag(targetTag.name, value, typeToken, overrideTagUserIntent(targetTag, overrideTagIntent))
        // check top tag type
        return if (res?.id != targetTag.id) null
        else res
    }

    /**
     * add [converter] that with the highest priority, which means all value/tag will first be passed to this [converter] to handle,
     * if no result comes out, then delegate to other [TagConverter].
     *
     * if register more than one [TagConverter], the last one be registered will have the highest priority
     * @return specifies that register is success or not
     */
    fun registerConverter(converter:TagConverter<Any>):Boolean {
        return this.converterProxy.registerToFirst(converter)
    }

    /**
     * register Codec to encode/decode a tag to binary format. Because one [Codec] can only handle one Tag type (with id specified in [Tag.id]),
     * so it will replace the [Codec] which handle same type tag
     * @return specifies that register is success or not
     */
    fun registerCodec(codec: Codec<Any>):Boolean {
        return this.codecProxy.registerCodec(codec)
    }

    @Suppress("UNCHECKED_CAST")
    private fun encapsulatedWithCompoundTag(name:String, value:Tag<*>):CompoundTag {
        val compound = CompoundTag(name)
        compound.add(value as Tag<out Any>)
        return compound
    }


    // options start

    /**
     * set convert object that has nullable properties or not. in default the value is true
     *
     * if value is true, object returned from deserialize/fromBytes may have some null properties that Mnbt can not handle
     *
     * if value is false, object returned from deserialize/fromByte will be null if Mnbt can not handle some properties in returned object
     * (can not convert to field expected type or can not access field)
     */
    var returnObjectContainsNullableProperties:Boolean
        get() = reflectiveConverter.returnObjectWithNullableProperties
        set(b) {reflectiveConverter.returnObjectWithNullableProperties = b}

    /**
     * override part of list if override target is a list tag.
     * Converters will only override value in source index to target list tag index element
     */
    var completeOverride: Boolean
        get() = ListConverters.completeOverride
        set(value) {ListConverters.completeOverride = value}



    // options end




    protected val converterProxy:DefaultConverterProxy = DefaultConverterProxy()
    protected val codecProxy: DefaultCodecProxy = DefaultCodecProxy(BinaryCodecInstances.intCodec, BinaryCodecInstances.shortCodec,
            BinaryCodecInstances.byteCodec, BinaryCodecInstances.longCodec,
            BinaryCodecInstances.floatCodec, BinaryCodecInstances.doubleCodec,
            BinaryCodecInstances.stringCodec, BinaryCodecInstances.intArrayCodec,
            BinaryCodecInstances.byteArrayCodec, BinaryCodecInstances.longArrayCodec,
            BinaryCodecInstances.nullTagCodec)


    protected val arrayTypeListTagConverter = ListConverters.ArrayTypeListTagConverter(this.converterProxy)
    protected val listTypeConverter = ListConverters.IterableTypeConverter(this.converterProxy)
    protected val reflectiveConverter = ReflectiveConverter(this.converterProxy)
    protected val mapTypeTagConverter = MapTypeConverter(this.converterProxy)

    protected val listCodec = BinaryCodecInstances.ListTagCodec(this.codecProxy)
    protected val compoundTagCodec = BinaryCodecInstances.CompoundTagCodec(this.codecProxy)


    init {
        this.codecProxy.registerCodec(listCodec)
        this.codecProxy.registerCodec(compoundTagCodec)
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
         * a [String] value represent top tag name in a Nbt structure
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

        /**
         * (Optional Parameter) specify [EncodeIntent] by function caller,
         * the function will override interfaces [EncodeOnStream],[EncodeHead],and [RecordParentsWhenEncoding]
         */
        private val optionalParamEncodeIntent:Byte by UnsupportedProperty

        /**
         * (Optional Parameter) specify [DecodeIntent] by function caller,
         * the function will override interfaces [DecodeHead],[DecodeOnStream] and [DecodeTreeDepth]
         */
        private val optionalParamDecodeIntent:Byte by UnsupportedProperty

        /**
         * (Optional Parameter) specify [ToValueIntent] by function caller when convert a [Tag] to another java object
         */
        private val optionalParamToValueIntent:Byte by UnsupportedProperty

        /**
         * (Optional Parameter) specify [CreateTagIntent] by function caller when convert any java object to a [Tag],
         * the function will auto override [RecordParents] interfaces
         */
        private val optionalParamCreateTagIntent:Byte by UnsupportedProperty


    }

}