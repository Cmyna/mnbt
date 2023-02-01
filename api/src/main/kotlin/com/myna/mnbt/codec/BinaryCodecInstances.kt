package com.myna.mnbt.codec

import com.myna.mnbt.utils.Extensions.toBasic
import com.myna.mnbt.utils.Extensions.toBytes
import com.myna.mnbt.tag.*
import com.myna.mnbt.reflect.TypeCheckTool
import java.lang.IllegalArgumentException
import com.myna.mnbt.*
import com.myna.mnbt.core.CodecTool
import com.myna.mnbt.presets.BitsArrayLengthGetter
import com.myna.mnbt.utils.CodecIntentExentions.decodeHead
import com.myna.mnbt.utils.CodecIntentExentions.tryGetId
import java.lang.NullPointerException


typealias RArray = java.lang.reflect.Array


object BinaryCodecInstances {

    val intCodec = object: NumberTypeFlatCodec<Int, PrimitiveTag.IntTag>(IdTagInt, 0) {
        override fun createTag(name: String?, value: Int) = PrimitiveTag.IntTag(name, value)
    } as DefaultCodec<Int>
    val byteCodec = object: NumberTypeFlatCodec<Byte, PrimitiveTag.ByteTag>(IdTagByte, 0.toByte()) {
        override fun createTag(name: String?, value: Byte) = PrimitiveTag.ByteTag(name, value)
    } as DefaultCodec<Byte>

    val shortCodec = object: NumberTypeFlatCodec<Short, PrimitiveTag.ShortTag>(IdTagShort, 0.toShort()) {
        override fun createTag(name: String?, value: Short) = PrimitiveTag.ShortTag(name, value)
    } as DefaultCodec<Short>
    val longCodec = object: NumberTypeFlatCodec<Long, PrimitiveTag.LongTag>(IdTagLong, 0.toLong()) {
        override fun createTag(name: String?, value: Long) = PrimitiveTag.LongTag(name, value)
    } as DefaultCodec<Long>
    val floatCodec = object: NumberTypeFlatCodec<Float, PrimitiveTag.FloatTag>(IdTagFloat,0.0f) {
        override fun createTag(name: String?, value: Float) = PrimitiveTag.FloatTag(name, value)
    } as DefaultCodec<Float>
    val doubleCodec = object: NumberTypeFlatCodec<Double, PrimitiveTag.DoubleTag>(IdTagDouble,0.0) {
        override fun createTag(name: String?, value: Double) = PrimitiveTag.DoubleTag(name, value)
    } as DefaultCodec<Double>

    val nullTagCodec = NullTagCodec()


    val stringCodec = StringCodec() as DefaultCodec<String>
    val byteArrayCodec = FixPayloadArrayTagFlatCodec<Byte, ByteArray>(
            IdTagByteArray, 1, { name, value-> ArrayTag.ByteArrayTag(name, value)},
            {data:ByteArray, start:Int -> data.toBasic(start, 0.toByte())},
            {element-> byteArrayOf(element)},
            BitsArrayLengthGetter::defaultToInt,
            ByteArray::class.java
    ) as DefaultCodec<ByteArray>
    val intArrayCodec = FixPayloadArrayTagFlatCodec<Int, IntArray>(
            IdTagIntArray, 4, { name, value-> ArrayTag.IntArrayTag(name, value)},
            {data:ByteArray, start:Int -> data.toBasic(start, 0)},
            {element-> element.toBytes()},
            BitsArrayLengthGetter::defaultToInt,
            IntArray::class.java) as DefaultCodec<IntArray>
    val longArrayCodec = FixPayloadArrayTagFlatCodec<Long, LongArray>(
            IdTagLongArray, 8, { name, value-> ArrayTag.LongArrayTag(name, value)},
            {data:ByteArray, start:Int -> data.toBasic(start, 0.toLong())},
            {element-> element.toBytes()},
            BitsArrayLengthGetter::defaultToInt,
            LongArray::class.java) as DefaultCodec<LongArray>

    private class StringCodec: DefaultCodec<String>(IdTagString, String::class.java) {

        override fun encodeValue(value: String, intent: EncodeOnStream):CodecFeedback {
            val valueBits = value.toByteArray(Charsets.UTF_8)
            if (valueBits.size > 65535) throw IllegalArgumentException("String Tag value length is over 65535!")
            val valueLen = valueBits.size
            intent.outputStream.write(valueLen.toShort().toBytes())
            intent.outputStream.write(valueBits)
            return object:CodecFeedback {}
        }

        override fun decodeToValue(intent: DecodeOnStream): String {
            val inputStream = intent.inputStream
            val bitsLen = inputStream.readNBytes(2).toBasic<Short>(0, 0).toInt()
            return inputStream.readNBytes(bitsLen).toString(Charsets.UTF_8)
        }

        override fun createTag(name: String?, value: String): Tag<String> {
            return PrimitiveTag.StringTag(name, value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    /**
     * now this class should not be implemented in other place, because it only support limit of subclass from Number.
     * the main reason is the extend function not supported
     */
    private abstract class NumberTypeFlatCodec<V:Number, T: Tag<V>>(id:Byte, val inst:V) :
            DefaultCodec<V>(id, inst::class.java as Class<V>) {
        val instSize = inst.toBytes().size

        override fun encodeValue(value: V, intent: EncodeOnStream):CodecFeedback {
            intent.outputStream.write(value.toBytes())
            return object:CodecFeedback {}
        }
        override fun decodeToValue(intent: DecodeOnStream):V {
            return intent.inputStream.readNBytes(instSize).toBasic(0, inst)
        }
    }


    // TODO: optimization: replace RArray.get/set
    /**
     * here restrict the Tag sub-class related generic type is an array by check generic type ET of input parameters clazz
     */
    private class FixPayloadArrayTagFlatCodec<E:Any, ARR:Any>
    (
            id:Byte,
            val elementSize:Int,
            val tagCreation: (name: String?, value: ARR) -> Tag<ARR>,
            val bitsToElement: (data:ByteArray, start:Int)->E,
            val elementToBits: (element:E)->ByteArray,

            val bitsToArrayLength: (data:ByteArray, start:Int)->Int, // bits to array length function
            valueTypeToken:Class<ARR>,
            ) : DefaultCodec<ARR>(id, valueTypeToken) {

        init {
            //check Tag value type is fix payload array (has actual type)
            if (
                    (!TypeCheckTool.isArray(valueTypeToken)) ||
                    (valueTypeToken.componentType) !is Class<*>) {
                throw IllegalArgumentException("the Codec class can not accept a type that is $valueTypeToken" +
                        "with component ${valueTypeToken.componentType}!"+
                        "Codec can only accept tag value type which is an actual Array with fix payload!")
            }
        }

        override fun createTag(name: String?, value: ARR): Tag<ARR> = tagCreation(name, value)

        @Suppress("UNCHECKED_CAST")
        override fun encodeValue(value: ARR, intent: EncodeOnStream):CodecFeedback {
            val elementNum = RArray.getLength(value)
            val outputStream = intent.outputStream
            outputStream.write(elementNum.toBytes())
            for (i in IntRange(0, elementNum-1)) {
                val elementBits = elementToBits(RArray.get(value, i) as E)
                outputStream.write(elementBits)
            }
            return object:CodecFeedback {}
        }


        @Suppress("UNCHECKED_CAST")
        override fun decodeToValue(intent: DecodeOnStream): ARR {
            val inputStream = intent.inputStream
            // read element num
            val size = bitsToArrayLength(inputStream.readNBytes(ArraySizePayload), 0)
            if (size < 0) throw IllegalArgumentException("get invalid binary Nbt data, array size $size is negative!")
            val array = RArray.newInstance(valueTypeToken.componentType, size) as ARR
            for (i in 0 until size) {
                val element = bitsToElement(inputStream.readNBytes(elementSize), 0)
                RArray.set(array, i, element)
            }
            return array
        }
    }

    @Suppress("UNCHECKED_CAST")
    class ListTagCodec(override var proxy: Codec<Any>)
        : HierarchicalCodec<AnyTagList> {
        override val id: Byte = IdTagList
        override val valueTypeToken = MutableList::class.java as Class<AnyTagList>

        override fun encode(tag: Tag<out AnyTagList>, intent: CodecCallerIntent):CodecFeedback {
            intent as EncodeHead; intent as EncodeOnStream
            val hasHead = intent.encodeHead
            val parents = (intent as CodecRecordParents).parents
            if (tag !is ListTag<*>) throw IllegalArgumentException("List Tag Codec can only handle tag type that is ListTag, but ${tag::class.java} is passed")
            val name = if (hasHead) tag.name?: throw NullPointerException("want serialize tag with tag head, but name was null!") else null
            if (name != null) CodecTool.writeTagHead(id, name, intent.outputStream)
            // write element id
            intent.outputStream.write(tag.elementId.toInt())
            // write array size
            intent.outputStream.write(tag.value.size.toBytes())
            val proxyIntent = toProxyIntent(false, parents, intent.outputStream)
            for (tags in tag.value) {
                proxy.encode(tags, proxyIntent)
            }
            return object:OutputStreamFeedback{
                override val outputStream = intent.outputStream
            }
        }

        override fun decode(intent: CodecCallerIntent): TagFeedback<AnyTagList> {
            intent as DecodeOnStream; intent as CodecRecordParents; intent as DecodeHead
            val parents = intent.parents
            // read tag head if intent wants
            val name = intent.decodeHead(id)
            // read element id
            val elementId = intent.inputStream.read().toByte()
            // read list size
            val size = intent.inputStream.readNBytes(4).toBasic(0,0)
            val nbtlist = ListTag<Any>(elementId, name)
            val proxyIntent = toProxyIntent(false, parents, elementId, intent.inputStream)
            for (i in 0 until size) {
                val feedback = proxy.decode(proxyIntent)
                nbtlist.add(feedback.tag)
            }
            return object:TagFeedback<AnyTagList> {
                override val tag: Tag<AnyTagList> = nbtlist
            }
        }
    }

    class CompoundTagCodec(override var proxy: Codec<Any>)
        : DefaultCodec<AnyCompound>(IdTagCompound, Collection::class.java as Class<AnyCompound>),HierarchicalCodec<AnyCompound> {

            override fun createTag(name: String?, value: AnyCompound): Tag<AnyCompound> {
                return CompoundTag(name, value)
            }

            override fun encodeValue(value: AnyCompound, intent: EncodeOnStream):CodecFeedback {
                val parents = (intent as CodecRecordParents).parents
                val proxyIntent = toProxyIntent(true, parents, intent.outputStream)
                value.onEach {
                    checkNotNull(it.value.name) // name should not be null
                    proxy.encode(it.value, proxyIntent)
                }
                // write TagEnd
                intent.outputStream.write(IdTagEnd.toInt())
                return object: CodecFeedback{}
            }

            override fun decodeToValue(intent: DecodeOnStream): AnyCompound {
                val parents = (intent as CodecRecordParents).parents
                val inputStream = intent.inputStream
                var subTagId = intent.tryGetId()
                val compound = mutableMapOf<String, Tag<out Any>>()
                while (subTagId != IdTagEnd) {
                    val proxyIntent = toProxyIntent(true, parents, subTagId, inputStream, true)
                    val feedback = proxy.decode(proxyIntent)
                    compound[feedback.tag.name!!] = feedback.tag
                    subTagId = intent.tryGetId()
                }
                //inputStream.read()
                return compound
            }
        }

    class NullTagCodec : Codec<Nothing> {
        override val id: Byte = IdTagEnd
        // hacky way, because can not init TypeToken<Nothing>(or TypeToken<void>)
        override val valueTypeToken = Nothing::class.java

        override fun encode(tag: Tag<out Nothing>, intent: CodecCallerIntent):CodecFeedback {
            intent as EncodeOnStream
            intent.outputStream.write(IdTagEnd.toInt())
            return object:CodecFeedback{}
        }

        override fun decode(intent: CodecCallerIntent): TagFeedback<Nothing> {
            intent as DecodeOnStream; intent as DecodeHead
            val inputStream = intent.inputStream
            if (!intent.ignoreIdWhenDecoding)CodecTool.checkNbtFormat(inputStream, id)
            return object:TagFeedback<Nothing> {
                override val tag = NullTag() as Tag<Nothing>
            }
        }

    }


}