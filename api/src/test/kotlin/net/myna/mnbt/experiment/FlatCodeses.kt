package net.myna.mnbt.experiment

import net.myna.mnbt.*
import net.myna.mnbt.codec.*
import net.myna.mnbt.codec.binary.*
import net.myna.mnbt.presets.BitsArrayLengthGetter
import net.myna.mnbt.reflect.TypeCheckTool
import net.myna.mnbt.tag.*
import net.myna.utils.Extensions.toBasic
import net.myna.utils.Extensions.toBytes
import net.myna.utils.Extensions.toString
import java.lang.IllegalArgumentException

object FlatCodeses {

    val intCodec = object: NumberTypeFlatCodec<Int, IntTag>(IdTagInt, 0) {
        override fun createTag(name: String?, value: Int) = IntTag(name, value)
    } as OnByteFlatCodec<Int>
    val byteCodec = object: NumberTypeFlatCodec<Byte, ByteTag>(IdTagByte, 0.toByte()) {
        override fun createTag(name: String?, value: Byte) = ByteTag(name, value)
    } as OnByteFlatCodec<Byte>

    val shortCodec = object: NumberTypeFlatCodec<Short, ShortTag>(IdTagShort, 0.toShort()) {
        override fun createTag(name: String?, value: Short) = ShortTag(name, value)
    } as OnByteFlatCodec<Short>
    val longCodec = object: NumberTypeFlatCodec<Long, LongTag>(IdTagLong, 0.toLong()) {
        override fun createTag(name: String?, value: Long) = LongTag(name, value)
    } as OnByteFlatCodec<Long>
    val floatCodec = object: NumberTypeFlatCodec<Float, FloatTag>(IdTagFloat,0.0f) {
        override fun createTag(name: String?, value: Float) = FloatTag(name, value)
    } as OnByteFlatCodec<Float>
    val doubleCodec = object: NumberTypeFlatCodec<Double, DoubleTag>(IdTagDouble,0.0) {
        override fun createTag(name: String?, value: Double) = DoubleTag(name, value)
    } as OnByteFlatCodec<Double>

    val stringCodec = StringCodec() as OnByteFlatCodec<String>

    val byteArrayCodec = FixPayloadArrayTagFlatCodec<Byte, ByteArray>(
            IdTagByteArray, 1, { name, value -> ByteArrayTag(name, value) },
            { data: ByteArray, start: Int -> data.toBasic(start, 0.toByte()) },
            { element -> byteArrayOf(element) },
            BitsArrayLengthGetter::defaultToInt,
            ByteArray::class.java
    ) as OnByteFlatCodec<ByteArray>
    val intArrayCodec = FixPayloadArrayTagFlatCodec<Int, IntArray>(
            IdTagIntArray, 4, { name, value -> IntArrayTag(name, value) },
            { data: ByteArray, start: Int -> data.toBasic(start, 0) },
            { element -> element.toBytes() },
            BitsArrayLengthGetter::defaultToInt,
            IntArray::class.java) as OnByteFlatCodec<IntArray>
    val longArrayCodec = FixPayloadArrayTagFlatCodec<Long, LongArray>(
            IdTagLongArray, 8, { name, value -> LongArrayTag(name, value) },
            { data: ByteArray, start: Int -> data.toBasic(start, 0.toLong()) },
            { element -> element.toBytes() },
            BitsArrayLengthGetter::defaultToInt,
            LongArray::class.java) as OnByteFlatCodec<LongArray>


    @Suppress("UNCHECKED_CAST")
    /**
     * now this class should not be implemented in other place, because it only support limit of subclass from Number.
     * the main reason is the extend function not supported
     */
    private abstract class NumberTypeFlatCodec<V:Number, T: Tag<V>>(id:Byte, val inst:V) :
            OnByteFlatCodec<V>(id, inst::class.java as Class<V>) {
        val instSize = inst.toBytes().size

        override fun encodeValue(value: V, intent: CodecCallerIntent): CodecFeedback {
            return object: EncodedBytesFeedback {
                override val bytes: ByteArray = value.toBytes()
            }
        }
        override fun decodeToValue(intent: CodecCallerIntent): V {
            intent as DecodeFromBytes
            val value = intent.data.toBasic(intent.pointer, inst)
            intent.pointer += instSize
            return value
        }
    }

    private class StringCodec: OnByteFlatCodec<String>(IdTagString, String::class.java) {

        override fun encodeValue(value: String, intent: CodecCallerIntent): CodecFeedback {
            val valueBits = value.toByteArray(Charsets.UTF_8)
            if (valueBits.size > 65535) throw IllegalArgumentException("String Tag value length is over 65535!")
            val valueLen = valueBits.size
            val bits = ByteArray(ShortSizePayload+valueBits.size)
            System.arraycopy(valueLen.toShort().toBytes(), 0, bits, 0, ShortSizePayload)
            System.arraycopy(valueBits, 0, bits, ShortSizePayload, valueLen)
            return object: EncodedBytesFeedback {
                override val bytes: ByteArray = bits
            }

        }

        override fun decodeToValue(intent: CodecCallerIntent): String {
            intent as DecodeFromBytes
            val bitsLen = intent.data.toBasic<Short>(intent.pointer, 0).toInt()
            intent.pointer += ShortSizePayload
            val value = intent.data.toString(intent.pointer, bitsLen)
            intent.pointer += bitsLen
            return value
        }

        override fun createTag(name: String?, value: String): Tag<String> {
            return StringTag(name, value)
        }
    }

    private class FixPayloadArrayTagFlatCodec<E:Any, ARR:Any>
    (
            id:Byte,
            val elementSize:Int,
            val tagCreation: (name: String?, value: ARR) -> Tag<ARR>,
            val bitsToElement: (data:ByteArray, start:Int)->E,
            val elementToBits: (element:E)->ByteArray,

            val bitsToArrayLength: (data:ByteArray, start:Int)->Int, // bits to array length function
            valueTypeToken:Class<ARR>,
    ) : OnByteFlatCodec<ARR>(id, valueTypeToken) {

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
        override fun encodeValue(value: ARR, intent: CodecCallerIntent): CodecFeedback {
            val elementNum = RArray.getLength(value)

            val bits = ByteArray(IntSizePayload+elementNum*elementSize)
            System.arraycopy(elementNum.toBytes(), 0, bits, 0, IntSizePayload)
            var pointer = IntSizePayload
            for (i in IntRange(0, elementNum-1)) {
                val elementBits = elementToBits(RArray.get(value, i) as E)
                System.arraycopy(elementBits, 0, bits, pointer, elementSize)
                pointer += elementSize
            }
            return object: EncodedBytesFeedback {
                override val bytes: ByteArray = bits
            }
        }


        @Suppress("UNCHECKED_CAST")
        override fun decodeToValue(intent: CodecCallerIntent): ARR {
            intent as DecodeFromBytes
            val size = bitsToArrayLength(intent.data, intent.pointer)
            intent.pointer += IntSizePayload
            val array = RArray.newInstance(valueTypeToken.componentType, size) as ARR
            for (i in 0 until size) {
                val element = bitsToElement(intent.data, intent.pointer)
                intent.pointer += elementSize
                RArray.set(array, i, element)
            }
            return array
        }
    }
}