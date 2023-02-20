package net.myna.mnbt

import net.myna.mnbt.converter.*
import net.myna.utils.AdaptedInputStream
import net.myna.mnbt.tag.CompoundTag
import net.myna.mnbt.exceptions.ConverterNullResultException
import net.myna.mnbt.codec.*
import net.myna.mnbt.exceptions.CircularReferenceException
import net.myna.mnbt.reflect.MTypeToken
import net.myna.mnbt.tag.AnyCompound
import net.myna.mnbt.utils.UnsupportedProperty
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.NullPointerException

//TODO: Exception analyse, refactoring and handling
open class Mnbt {

    @Suppress("UNCHECKED_CAST")
    /**
     * serialize value to Nbt binary data stored in a ByteArray(byte[])
     *
     * if value is a Tag, then it will be encapsulated by a compound Tag with name passed in,
     * then serialize to bytes,
     * else value with value will first convert to Tag then serialized to bytes
     * @param name name of nbt root tag, see [varTopTagName]
     * @param value the value want to serialize, see [paramJavaObject]
     * @param typeToken extra typeToken info for value (if ignore this parameter, TypeToken will use value.class as TypeTokenInfo)
     * see [paramTypeToken]
     * @return a ByteArray stores data starts at 0 ends at ByteArray.size
     * @throws ConverterNullResultException if result of value->tag is null
     * @throws CircularReferenceException if circular reference is found
     */
    open fun <V:Any> toBytes(name:String, value:V, typeToken: MTypeToken<out V> = MTypeToken.of(value::class.java)):ByteArray {
        if (value is Tag<*>) {
            return encode(value as Tag<out Any>)
        }
        val tag = converterProxy.createTag(name, value, typeToken, createTagUserIntent())
                ?: throw ConverterNullResultException(value)
        return encode(tag)
    }

    @Suppress("UNCHECKED_CAST")
    /**
     * deserialize nbt binary data from a ByteArray starts at pointer passed in
     * @param bytes the ByteArray stores nbt data
     * @param start pointer to the nbt data
     * @param typeToken see [paramTypeToken]
     * if null it will return default type
     * @return a pair that first element stores root tag name, second one stores deserialized tag value
     * @throws NullPointerException TODO
     */
    open fun <V:Any> fromBytes(bytes:ByteArray, start:Int, typeToken: MTypeToken<out V>? = null):Pair<String, V>? {
        val actualTypeToken = typeToken?:MTypeToken.of(Any::class.java)
        val tag = decode(bytes, start)
        val converterCallerIntent = if (typeToken == null)  converterCallerIntent(true) else converterCallerIntent(false)
        val res = converterProxy.toValue<Any>(tag, actualTypeToken, converterCallerIntent)
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
    open fun <V:Any> toStream(name:String, value:V, typeToken: MTypeToken<out V>, outputStream:OutputStream):Boolean {
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
    open fun <V:Any> fromStream(typeToken: MTypeToken<out V>, inputStream: InputStream, converterIntent: ToValueIntent?=null):Pair<String?, V>? {
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
    open fun encode(tag:Tag<out Any>, outputStream: OutputStream) {
        codecProxy.encode(tag, userEncodeIntent(outputStream))
    }

    /**
     * encode a [Tag] to an byte array
     * @param tag the [Tag] want to encode
     * @return a byte array stores encoded result starts at index 0, ends at byte array's end
     * @throws CircularReferenceException if circular references is found in [tag]
     */
    open fun encode(tag:Tag<out Any>):ByteArray {
        val outputStream = ByteArrayOutputStream()
        codecProxy.encode(tag, userEncodeIntent(outputStream))
        return outputStream.toByteArray()
    }

    /**
     * decode a tag from an [InputStream]
     * @param inputStream see [paramBinInputStream]
     * @return decoded [Tag]
     */
    open fun decode(inputStream: InputStream):Tag<out Any> {
        return codecProxy.decode(userDecodeIntent(inputStream)).tag
    }

    /**
     * decode a byte array
     * @start specifies where the nbt binary data starts
     * @return the decoded result
     */
    open fun decode(bytes: ByteArray, start:Int):Tag<out Any> {
        val inputStream = ByteArrayInputStream(bytes.copyOfRange(start, bytes.size))
        return codecProxy.decode(userDecodeIntent(inputStream)).tag
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

    /**
     * @param value see [paramJavaObject]
     * @param typeToken see [paramTypeToken]
     * @param targetTag a [Tag] that will be overridden
     * @return a new [Tag] that is combined from value and targetTag,
     * all same tag in [targetTag] nbt structure is overridden by value
     */
    fun <V:Any> overrideTag(value:V, typeToken: MTypeToken<out V>, targetTag:Tag<out Any>):Tag<out Any>? {
        // for flat tag, it just creates a new tag and use value from parameter, use name from target tag
        // so no extra code to reach functionality of override
        // problem is need to check tag type is equals or not
        val res = converterProxy.createTag(targetTag.name, value, typeToken, overrideTagUserIntent(targetTag))
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
    fun registerCodec(codec:Codec<Any>):Boolean {
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
     * This option is experimental and may be removed in the future!
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
    protected val codecProxy:DefaultCodecProxy = DefaultCodecProxy(BinaryCodecInstances.intCodec, BinaryCodecInstances.shortCodec,
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