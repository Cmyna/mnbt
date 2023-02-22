package net.myna.mnbt.converterTest


import net.myna.mnbt.converter.TagConverters
import net.myna.mnbt.reflect.MTypeToken
import net.myna.mnbt.tag.ArrayTag
import net.myna.mnbt.tag.ByteArrayTag
import net.myna.mnbt.tag.IntArrayTag
import net.myna.mnbt.tag.LongArrayTag
import net.myna.mnbt.utils.ApiTestTool
import net.myna.mnbt.utils.RandomValueTool
import org.junit.jupiter.api.Test
import kotlin.random.Random

class PrimitiveTypeTest {

    private val testTemplate = ApiTestTool.ConverterTestTemplate()

    @Test
    fun intTest() {
        val intTk = MTypeToken.of(Int::class.java)
        val integerTk = MTypeToken.of(Integer::class.java)
        testTemplate.apiTest(TagConverters.intTagConverter, "", "int2", 0, 5177, intTk)
        testTemplate.apiTest(TagConverters.intTagConverter, "int1", "int2", Int.MAX_VALUE, Int.MIN_VALUE, intTk)
    }

    @Test
    fun byteTest() {
        val byteTk = MTypeToken.of(Byte::class.java)
        val boxedTk = MTypeToken.of(java.lang.Byte::class.java)
        testTemplate.apiTest(TagConverters.byteTagConverter, "", "byte2", 0.toByte(), 33.toByte(), byteTk)
        testTemplate.apiTest(TagConverters.byteTagConverter, "byte1", "byte2", Byte.MAX_VALUE, Byte.MIN_VALUE, byteTk)
    }

    @Test
    fun shortTest() {
        val tk = MTypeToken.of(Short::class.java)
        val boxedTk = MTypeToken.of(java.lang.Short::class.java)
        testTemplate.apiTest(TagConverters.shortTagConverter, "", "short2", 0.toShort(), 33.toShort(), tk)
        testTemplate.apiTest(TagConverters.shortTagConverter, "short1", "short2", Short.MAX_VALUE, Short.MIN_VALUE, tk)

    }

    @Test
    fun longTest() {
        val tk = MTypeToken.of(Long::class.java)
        val boxedTk = MTypeToken.of(java.lang.Long::class.java)
        testTemplate.apiTest(TagConverters.longTagConverter, "", "long2", 0.toLong(), 55555.toLong(), tk)
        testTemplate.apiTest(TagConverters.longTagConverter, "long1", "long2", Long.MAX_VALUE, Long.MIN_VALUE, tk)

    }

    @Test
    fun floatTest() {
        val tk = MTypeToken.of(Float::class.java)
        val boxedTk = MTypeToken.of(java.lang.Float::class.java)
        testTemplate.apiTest(TagConverters.floatTagConverter, "", "float2", 0.0f, 13.0f, tk)
        testTemplate.apiTest(TagConverters.floatTagConverter, "float1", "float2", Float.MAX_VALUE, Float.MIN_VALUE, tk)
        testTemplate.apiTest(TagConverters.floatTagConverter, "float1", "float2", Float.NaN, Float.MIN_VALUE, tk)
        testTemplate.apiTest(TagConverters.floatTagConverter, "float1", "float2", Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, tk)

    }

    @Test
    fun doubleTest() {
        val tk = MTypeToken.of(Double::class.java)
        val boxedTk = MTypeToken.of(java.lang.Double::class.java)
        testTemplate.apiTest(TagConverters.doubleTagConverter, "float1", "float2", 0.0, 13.0, tk)
        testTemplate.apiTest(TagConverters.doubleTagConverter, "float1", "float2", Double.MAX_VALUE, Double.MIN_VALUE, tk)
        testTemplate.apiTest(TagConverters.doubleTagConverter, "float1", "float2", Double.NaN, Double.MIN_VALUE, tk)
        testTemplate.apiTest(TagConverters.doubleTagConverter, "float1", "float2", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, tk)
    }

    @Test
    fun stringTest() {
        val tk = MTypeToken.of(String::class.java)
        testTemplate.apiTest(TagConverters.stringTagConverter, "string1", "string2", "string value1", "string value2", tk)
        testTemplate.apiTest(TagConverters.stringTagConverter, "", "string2", "", "string value2", tk)
    }

    @Test
    fun byteArrayTest() {
        val tk = MTypeToken.of(ByteArray::class.java)
        val boxedTk = object:MTypeToken<Array<Byte>>() {}
        var bytes = Random.nextBytes(0)
        testTemplate.expectedTag = ByteArrayTag("", bytes)
        testTemplate.apiTest(TagConverters.byteArrayConverter, "", "bytes2", bytes,  Random.nextBytes(50), tk)
        bytes = Random.nextBytes(10)
        testTemplate.expectedTag = ByteArrayTag("bytes1", bytes)
        testTemplate.apiTest(TagConverters.byteArrayConverter, "bytes1", "bytes2", bytes, Random.nextBytes(10), tk)
        var bytes2 = Array(100){Random.nextBytes(1)[0]}
        testTemplate.expectedTag = ByteArrayTag("bytes1", bytes2.toByteArray())
        testTemplate.apiTest(TagConverters.byteArrayConverter, "bytes1", "bytes2",
                bytes2, Array(100){Random.nextBytes(1)[0]}, boxedTk)
        bytes2 = Array(0){Random.nextBytes(1)[0]}
        testTemplate.expectedTag = ByteArrayTag("bytes1", bytes2.toByteArray())
        testTemplate.apiTest(TagConverters.byteArrayConverter, "bytes1", "bytes2",
                bytes2, Array(100){Random.nextBytes(1)[0]}, boxedTk)
    }

    @Test
    fun intArrayTest() {
        val tk = MTypeToken.of(IntArray::class.java)
        val boxedTk = object:MTypeToken<Array<Int>>() {}
        var ints1 = RandomValueTool.intArrC(10)()
        testTemplate.expectedTag = IntArrayTag("", ints1)
        testTemplate.apiTest(TagConverters.intArrayConverter, "", "ints2", ints1,  RandomValueTool.intArrC(10)(), tk)
        ints1 = RandomValueTool.intArrC(0)()
        testTemplate.expectedTag = IntArrayTag("ints1", ints1)
        testTemplate.apiTest(TagConverters.intArrayConverter, "ints1", "ints2", ints1, RandomValueTool.intArrC(10)(), tk)

        var ints2 = Array(100){Random.nextInt()}
        testTemplate.expectedTag = IntArrayTag("ints1", ints2.toIntArray())
        testTemplate.apiTest(TagConverters.intArrayConverter, "ints1", "ints2",
                ints2, Array(100){Random.nextInt()}, boxedTk)
        ints2 = Array(0){Random.nextInt()}
        testTemplate.expectedTag = IntArrayTag("ints1", ints2.toIntArray())
        testTemplate.apiTest(TagConverters.intArrayConverter, "ints1", "ints2",
                ints2, Array(100){Random.nextInt()}, boxedTk)
    }

    @Test
    fun longArrayTest() {
        val tk = MTypeToken.of(LongArray::class.java)
        val boxedTk = object:MTypeToken<Array<Long>>() {}
        var ints1 = RandomValueTool.longArrC(10)()
        testTemplate.expectedTag = LongArrayTag("", ints1)
        testTemplate.apiTest(TagConverters.longArrayConverter, "", "longs2", ints1,  RandomValueTool.longArrC(10)(), tk)
        ints1 = RandomValueTool.longArrC(0)()
        testTemplate.expectedTag = LongArrayTag("longs1", ints1)
        testTemplate.apiTest(TagConverters.longArrayConverter, "longs1", "longs2", ints1, RandomValueTool.longArrC(10)(), tk)

        var ints2 = Array(100){Random.nextLong()}
        testTemplate.expectedTag = LongArrayTag("longs1", ints2.toLongArray())
        testTemplate.apiTest(TagConverters.longArrayConverter, "longs1", "longs2",
                ints2, Array(100){Random.nextLong()}, boxedTk)
        ints2 = Array(0){Random.nextLong()}
        testTemplate.expectedTag = LongArrayTag("longs1", ints2.toLongArray())
        testTemplate.apiTest(TagConverters.longArrayConverter, "longs1", "longs2",
                ints2, Array(100){Random.nextLong()}, boxedTk)
    }

}