package net.myna.mnbt

import net.myna.mnbt.codec.binary.BinaryCodecInstances
import net.myna.mnbt.converter.TagConverters
import net.myna.mnbt.reflect.MTypeToken
import net.myna.mnbt.tag.ByteTag
import net.myna.mnbt.tag.PrimitiveTag
import net.myna.mnbt.utils.ApiTestTool
import net.myna.mnbt.utils.ApiTestTool.nestedListTest
import net.myna.mnbt.utils.ApiTestValueBuildTool
import net.myna.mnbt.utils.RandomValueTool
import net.myna.mnbt.utils.TestMnbt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap
import kotlin.random.Random

// FIXME: HARD TO RECURRENT ISSUE: some times not equals assertion failed (on ApiTestTool.template assert tag1 and tag3)
//  seems their name and value are all equals, name represent a chars sequence with unsupported symbols: `<?>`
//  it may comes from unreliable random string creation
class ApiValidationTest {

    private val valueEqFun = ApiTestTool::valueEqFun
    private val rvt = RandomValueTool
    private val template = ApiTestTool.Template()
    private val converterTemplate = ApiTestTool.ConverterTestTemplate()

    @Test
    fun primitiveApiTest() {
        // directly test primitive type tag Codec
        // (int, byte, short, long, float, double, string)

        template.apiTest(TagConverters.intTagConverter, BinaryCodecInstances.intCodec, 4, object: MTypeToken<Int>(){}) {Random.nextInt()}
        template.apiTest(TagConverters.shortTagConverter, BinaryCodecInstances.shortCodec, 2, object: MTypeToken<Short>(){}) {Random.nextInt().toShort()}
        template.apiTest(TagConverters.byteTagConverter, BinaryCodecInstances.byteCodec, 1, object: MTypeToken<Byte>(){}) {Random.nextBytes(1)[0]}
        template.apiTest(TagConverters.longTagConverter, BinaryCodecInstances.longCodec, 8, object: MTypeToken<Long>(){}) {Random.nextLong()}
        template.apiTest(TagConverters.floatTagConverter, BinaryCodecInstances.floatCodec, 4, object: MTypeToken<Float>(){}) {Random.nextFloat()}
        template.apiTest(TagConverters.doubleTagConverter, BinaryCodecInstances.doubleCodec, 8, object: MTypeToken<Double>(){}) {Random.nextDouble()}

        // String tag test
        val str1 = RandomValueTool.bitStrC(5)()
        val str2 = RandomValueTool.bitStrC(5)()
        val str3 = ""
        template.apiTest(
                TagConverters.stringTagConverter, BinaryCodecInstances.stringCodec,
                RandomValueTool.bitStrC(5)(), RandomValueTool.bitStrC(5)(),
                str1, str2, str1.toByteArray(Charsets.UTF_8).size+StringSizePayload,
                object: MTypeToken<String>(){}
        )
        template.apiTest(
                TagConverters.stringTagConverter, BinaryCodecInstances.stringCodec,
                RandomValueTool.bitStrC(5)(), RandomValueTool.bitStrC(5)(),
                str3, str2, StringSizePayload, object: MTypeToken<String>(){}
        )

    }

    /**
     * test convert from boolean value to Tag<Byte>, and Tag<Byte> back to boolean value
     */
    @Test
    fun booleanConversionTest() {
        val value1 = true
        val value2 = false
        val name1 = "boolean tag with true value"
        val name2 = "boolean tag with true value"
        val boolTypeToken = MTypeToken.of(Boolean::class.java)
        val tag1 = TestMnbt.inst.refConverterProxy.createTag(name1, value1, boolTypeToken)!!
        val tag2 = TestMnbt.inst.refConverterProxy.createTag(name2, value2, boolTypeToken)!!
        assertTrue(tag1.value is Byte)
        assertTrue(tag2.value is Byte)
        assertEquals(tag1.value, 1.toByte())
        assertEquals(tag2.value, 0.toByte())
        val backValue1 = TestMnbt.inst.refConverterProxy.toValue(tag1, boolTypeToken)!!
        val backValue2 = TestMnbt.inst.refConverterProxy.toValue(tag2, boolTypeToken)!!
        assertEquals(value1, backValue1.second)
        assertEquals(value2, backValue2.second)
        assertEquals(name1, backValue1.first)
        assertEquals(name2, backValue2.first)

        // test Tag<Byte> that value not 0
        val tag3 = ByteTag("", 127)
        val value3 = TestMnbt.inst.refConverterProxy.toValue(tag3, boolTypeToken)!!
        assertEquals(true, value3.second)

        val tag4 = ByteTag("", -15)
        val value4 = TestMnbt.inst.refConverterProxy.toValue(tag4, boolTypeToken)!!
        assertEquals(true, value4.second)
    }

    @Test
    fun primitiveArrayApiTest() {
        val intArray = IntArray(1000){Random.nextInt()}
        val iatk = object: MTypeToken<IntArray>(){}
        val tag1 = TagConverters.intArrayConverter.createTag("int array tag", intArray, iatk)
        val tag2 = TagConverters.intArrayConverter.createTag("int array tag", IntArray(1000){Random.nextInt()}, iatk)
        val tag3 = TagConverters.intArrayConverter.createTag("int array tag2", intArray, iatk)
        val bitsLen = 4004
        template.apiTest(BinaryCodecInstances.intArrayCodec, tag1!!, tag2!!, tag3!!, bitsLen)
        template.apiTest(TagConverters.intArrayConverter, BinaryCodecInstances.intArrayCodec, 4004, object: MTypeToken<IntArray>(){}, RandomValueTool.intArrC(1000))
        template.apiTest(TagConverters.byteArrayConverter, BinaryCodecInstances.byteArrayCodec, 1004, object: MTypeToken<ByteArray>(){}, RandomValueTool.byteArrC(1000))
        template.apiTest(TagConverters.longArrayConverter, BinaryCodecInstances.longArrayCodec, 8004, object: MTypeToken<LongArray>(){}, RandomValueTool.longArrC(1000))

        // what if boxed array
        template.apiTest(TagConverters.intArrayConverter, BinaryCodecInstances.intArrayCodec, 4004, object: MTypeToken<Array<Int>>(){})
            { Array(1000) {Random.nextInt()} }
        template.apiTest(TagConverters.byteArrayConverter, BinaryCodecInstances.byteArrayCodec, 1004, object: MTypeToken<Array<Byte>>(){})
            { Random.nextBytes(1000).toTypedArray() }
        template.apiTest(TagConverters.longArrayConverter, BinaryCodecInstances.longArrayCodec, 8004, object: MTypeToken<Array<Long>>(){})
            { Array(1000) {Random.nextLong()} }
    }



    @Test
    fun listTagApiTest() {
        //test
        ApiTestTool.listTagApiTest(1000, TestMnbt.inst.refArrayToListConverter, TestMnbt.inst.refListCodec)
        nestedListTest(TestMnbt.inst.refArrayToListConverter, TestMnbt.inst.refListCodec)
    }

    /**
     * test input value is array of flat value or primitive array
     */
    @Test
    fun arrayOfListTest() {
        val listOfArrays = ApiTestValueBuildTool.flatValueArraysPreparation(20)
        val listOfArrays2 = ApiTestValueBuildTool.flatValueArraysPreparation(20)
        listOfArrays.onEachIndexed { i,tri->
            template.apiTest(
                    TestMnbt.inst.refConverterProxy, TestMnbt.inst.refCodecProxy,
                    tri.first, listOfArrays2.get(i).first, tri.third, listOfArrays2.get(i).third,
                    tri.second, object:MTypeToken<Array<Any>>() {}
            )
        }
    }

    /**
     * test input value is list of flat value or primitive array
     */
    @Test
    fun listOfListTest() {
        val listOfArrays = ApiTestValueBuildTool.flatValueListsPreparation(20)
        val listOfArrays2 = ApiTestValueBuildTool.flatValueListsPreparation(20)
        listOfArrays.onEachIndexed { i,tri->
            template.apiTest(
                    TestMnbt.inst.refConverterProxy, TestMnbt.inst.refCodecProxy,
                    tri.first, listOfArrays2[i].first, tri.third, listOfArrays2.get(i).third,
                    tri.second, object:MTypeToken<List<Any>>() {}
            )
        }
    }


    /**
     * test map type with flat nbt structure base on Tag<Compound>
     */
    @Test
    fun flatMapTypeCompoundApiTest() {
        val singleTypeMapTest:(typeToken:MTypeToken<out Any>, mapCreation:()->MutableMap<String, Int>)->Unit = { typeToken,mapCreation ->
            val map1 = mapCreation()
            val emptyMap = mapCreation()
            val name1 = "int tag1"; map1[name1] = Random.nextInt()
            val name2 = "int tag2"; map1[name2] = Random.nextInt()
            val name3 = "int tag3"; map1[name3] = Random.nextInt()
            val compName = "compound tag"

            var bitsLen = 0
            bitsLen += (TagIdPayload+StringSizePayload+IntSizePayload)*3
            bitsLen += name1.toByteArray().size
            bitsLen += name2.toByteArray().size
            bitsLen += name3.toByteArray().size

            bitsLen += TagIdPayload // tag end payload
            template.valueEqFun = this.valueEqFun
            template.apiTest(TestMnbt.inst.refMapConverter, TestMnbt.inst.refCompoundCodec,
                    compName, "empty compound tag", map1, emptyMap, bitsLen, typeToken)
        }

        // test map which have empty parameters constructor
        template.assertRawType = true
        singleTypeMapTest(object:MTypeToken<HashMap<String, Int>>() {}) { HashMap() }
        singleTypeMapTest(object:MTypeToken<LinkedHashMap<String, Int>>() {}) {LinkedHashMap()}
        singleTypeMapTest(object:MTypeToken<LinkedHashMap<String, Int>>() {}) {mutableMapOf()}
        singleTypeMapTest(object:MTypeToken<TreeMap<String, Int>>() {}) {TreeMap()}

        // test map without specific map type token in
        template.assertRawType = false
        val genericMapTk = object:MTypeToken<Map<String, Any>>() {}
        singleTypeMapTest(genericMapTk) { HashMap() }
        singleTypeMapTest(genericMapTk) {LinkedHashMap()}
        singleTypeMapTest(genericMapTk) {mutableMapOf()}
        singleTypeMapTest(genericMapTk) {TreeMap()}

        // flat structure multi type map
        template.assertRawType = false
        val emptyMap = HashMap<String, Any>()
        ApiTestTool.mapTypeTest(template, TestMnbt.inst.refMapConverter, TestMnbt.inst.refCompoundCodec,
                emptyMap, 1) { HashMap() }
        ApiTestTool.mapTypeTest(template, TestMnbt.inst.refMapConverter, TestMnbt.inst.refCompoundCodec,
                emptyMap, 1) { TreeMap() }
    }

    /**
     * test map type with nested nbt structure base on Tag<Compound>
     */
    @Test
    fun nestedMapTypeCompoundApiTest() {
        val emptyMap = HashMap<String, Any>()
        ApiTestTool.mapTypeTest(template, TestMnbt.inst.refMapConverter, TestMnbt.inst.refCompoundCodec,
                emptyMap, 6) { HashMap() }
        ApiTestTool.mapTypeTest(template, TestMnbt.inst.refMapConverter, TestMnbt.inst.refCompoundCodec,
                emptyMap, 6) { TreeMap() }
    }

    /**
     * test transfer hierarchical java object (with map and list) to nbt type
     */
    @Test
    fun listWithMapApiTest() {
        val typeToken = object: MTypeToken<List<Map<String, Any>>>() {}
        val list1 = ApiTestValueBuildTool.flatMapListsPreparation("list1", 4) { HashMap() }
        val list2 = ApiTestValueBuildTool.flatMapListsPreparation("list2", 5) { HashMap() }
        template.apiTest(TestMnbt.inst.refListConverter, TestMnbt.inst.refListCodec,
                list1.first, list2.first, list1.third, list2.third, list1.second, typeToken)

        val typeToken2 = object:MTypeToken<Map<String, Any>>() {}
        val map1 = ApiTestValueBuildTool.mapWithListPreparation("map1", 3)
        val map2 = ApiTestValueBuildTool.mapWithListPreparation("map2", 3)
        template.apiTest(TestMnbt.inst.refMapConverter, TestMnbt.inst.refCompoundCodec,
                map1.first, map2.first, map1.third, map2.third, map1.second, typeToken2)
    }

    /**
     * test the functionality when there are circular references in Java object/Tag object.
     * in general, it may throw Exceptions, but maybe we can choose different ways to handle it, for eg. just ignore those reference (like they are null)
     */
    @Test
    fun circularReferenceTest() {
        // circular list
        val list1:MutableList<Any> = ArrayList()
        val list2:MutableList<Any> = ArrayList()
        list1.add(list2)
        list2.add(list1)
        val list3:MutableList<Any> = ArrayList()

        template.assertDecodedConvertedValueEq = false
        template.apiTest(
                TestMnbt.inst.refConverterProxy, TestMnbt.inst.refCodecProxy,
                "list1", "list3", list1, list3, TagIdPayload+ArraySizePayload+TagIdPayload+ArraySizePayload,
                object:MTypeToken<List<Any>>() {}
        )
    }

}
