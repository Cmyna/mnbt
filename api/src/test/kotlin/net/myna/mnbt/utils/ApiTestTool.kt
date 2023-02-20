package net.myna.mnbt.utils


import net.myna.mnbt.*
import net.myna.mnbt.codec.Codec
import net.myna.mnbt.codec.HierarchicalCodec
import net.myna.mnbt.codec.userDecodeIntent
import net.myna.mnbt.codec.userEncodeIntent
import net.myna.mnbt.converter.*
import net.myna.mnbt.reflect.MTypeToken
import net.myna.mnbt.reflect.ObjectInstanceHandler
import net.myna.mnbt.tag.AnyCompound
import net.myna.mnbt.tag.AnyTagList
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.random.Random


typealias RArray = java.lang.reflect.Array

@Suppress("UnstableApiUsage")
object ApiTestTool {

    // mutli type map test, need ensure all values in map can be handled automatically by converters
    // valueTriples: first is tag name, second one is value bits len, third one is value
    val mutliTypeMapTest:(template: Template, mapTypeTagConverter: TagConverter<AnyCompound>, compoundTagCodec: Codec<AnyCompound>,
                          valueTriples1:List<Triple<String, Int, Any>>,
                          valueTriples2:List<Triple<String, Int, Any>>,
                          mapCreation:()->MutableMap<String, Any>)->Unit
            = { template, converter, Codec,
                valueTriples1, valueTriples2, mapCreation ->
        val map1 = mapCreation()
        val map2 = mapCreation()
        val rootName = RandomValueTool.bitStrC(5)()
        val rootName2 = RandomValueTool.bitStrC(5)()
        var rootValueBitsLen = TagIdPayload // add tag end payload
        valueTriples1.onEach {
            val name = it.first
            rootValueBitsLen += it.second+it.first.toByteArray(Charsets.UTF_8).size+TagHeadFixPayload
            val value = it.third
            map1[name] = value
        }
        valueTriples2.onEach { map2[it.first] = it.third }
        val emptyMap = mapCreation()
        template.valueEqFun = ApiTestTool::valueEqFun
        val token = object:MTypeToken<Map<String, Any>>() {}
        template.apiTest(converter, Codec, rootName, rootName2, map1, map2, rootValueBitsLen, token)
        template.apiTest(converter, Codec, rootName, "emptyMap", map1, emptyMap, rootValueBitsLen, token)
    }

    fun mapTypeTest(template: Template,
                    converter: TagConverter<AnyCompound>, codec: Codec<AnyCompound>,
                    emptyMap: Map<String, Any>, mapDepth:Int, mapCreation: () -> MutableMap<String, Any>) {
        val map1 = ApiTestValueBuildTool.nestedMapValuesPreparation(mapDepth, mapCreation)
        val map2 = ApiTestValueBuildTool.nestedMapValuesPreparation(mapDepth, mapCreation)
        mapTypeTest(template, converter, codec,
                map1.third, map2.third, emptyMap,
                map1.first, map2.first, map1.second)
    }

    fun mapTypeTest(template: Template,
                    converter: TagConverter<AnyCompound>, codec: Codec<AnyCompound>,
                    map1:Map<String, Any>, map2:Map<String, Any>, emptyMap:Map<String, Any>, rootName:String, rootName2:String,
                    mapValueBitsLen:Int
    ) {
        template.valueEqFun = ApiTestTool::valueEqFun
        val token = object:MTypeToken<Map<String, Any>>() {}
        template.apiTest(converter, codec, "map1 $rootName", "map2 $rootName2", map1, map2, mapValueBitsLen, token)
        template.apiTest(converter, codec, rootName, "emptyMap", map1, emptyMap, mapValueBitsLen, token)
    }

    fun listTagApiTest(arrSize: Int, converter: HierarchicalTagConverter<AnyTagList>, Codec: HierarchicalCodec<AnyTagList>) {
        val template = Template()
        val shortArray = ShortArray(1000) {Random.nextInt().toShort()}
        val shortArray2 = ShortArray(1000) {Random.nextInt().toShort()}
        val satk = object: MTypeToken<ShortArray>(){}
        val tag1 = converter.createTag("list tag for short tag", shortArray, satk)!!
        val tag2 = converter.createTag("list tag for short tag", shortArray2, satk)!!
        val tag3 = converter.createTag("list tag for short tag2", shortArray, satk)!!
        var bitsLen = 2000+4+1 // list tag value size:2000, list content tag id:1, list size info size: 4

        if (arrSize == 0) template.assertValueNotEquals = false // if empty list, means value always equals

        //template.valueEqFun = this.valueEqFun
        template.apiTest(Codec, tag1, tag2, tag3, bitsLen)

        // primitive type array
        template.testMnbt = false
        template.apiTest(converter, Codec, arrSize*4+5, object: MTypeToken<IntArray>(){}, RandomValueTool.intArrC(arrSize))
        template.apiTest(converter, Codec, arrSize*2+5, object: MTypeToken<ShortArray>(){}, RandomValueTool.shortArrC(arrSize))
        template.apiTest(converter, Codec, arrSize+5, object: MTypeToken<ByteArray>(){}, RandomValueTool.byteArrC(arrSize))
        template.apiTest(converter, Codec, arrSize*8+5, object: MTypeToken<LongArray>(){}, RandomValueTool.longArrC(arrSize))
        template.apiTest(converter, Codec, arrSize*4+5, object: MTypeToken<FloatArray>(){}, RandomValueTool.floatArrC(arrSize))
        template.apiTest(converter, Codec, arrSize*8+5, object: MTypeToken<DoubleArray>(){}, RandomValueTool.doubleArrC(arrSize))

        // wrapped type array
        template.apiTest(converter, Codec, arrSize*4+5, object: MTypeToken<Array<Int>>(){}, RandomValueTool.bintArrC(arrSize))
        template.apiTest(converter, Codec, arrSize*2+5, object: MTypeToken<Array<Short>>(){}, RandomValueTool.bshortArrC(arrSize))
        template.apiTest(converter, Codec, arrSize+5, object: MTypeToken<Array<Byte>>(){}, RandomValueTool.bbyteArrC(arrSize))
        template.apiTest(converter, Codec, arrSize*8+5, object: MTypeToken<Array<Long>>(){}, RandomValueTool.blongArrC(arrSize))
        template.apiTest(converter, Codec, arrSize*4+5, object: MTypeToken<Array<Float>>(){}, RandomValueTool.bfloatArrC(arrSize))
        template.apiTest(converter, Codec, arrSize*8+5, object: MTypeToken<Array<Double>>(){}, RandomValueTool.bdoubleArrC(arrSize))

        // String array
        val str1 = Array(Random.nextInt(0,50)) { RandomValueTool.bitStrC(5)()}
        val str2 = Array(Random.nextInt(0,50)) { RandomValueTool.bitStrC(5)()}
        bitsLen = str1.sumOf { it.toByteArray(Charsets.UTF_8).size+2 }
        template.apiTest(converter, Codec, RandomValueTool.bitStrC(5)(), RandomValueTool.bitStrC(5)(),
                str1, str2, bitsLen+5, object: MTypeToken<Array<String>>(){}
        )
    }

    fun nestedListTest(
            converter: HierarchicalTagConverter<AnyTagList>,
            Codec: HierarchicalCodec<AnyTagList>) {
        // test Array<Array<Int>>
        // but the tag hierarchy seems like List<IntArray>
        val template = Template()
        val arrSize = 1000; val intSize = 4
        val arrarrSize = 10
        val ArrArrInt:()->Array<Array<Int>> = {Array(arrarrSize) { RandomValueTool.bintArrC(arrSize)()} }
        val token1 = object: MTypeToken<Array<Array<Int>>>() {}
        var bitsLen = (arrSize*intSize+ ArraySizePayload)*arrarrSize+ TagIdPayload + ArraySizePayload
        template.apiTest(converter, Codec, bitsLen, token1, ArrArrInt)

        val strlistlist:()->Array<Array<String>> = {
            Array(arrarrSize) { Array(arrSize) {Random.nextBytes(Random.nextInt(0,20)).toString(Charsets.UTF_8)} }
        }
        val token2 = object: MTypeToken<Array<Array<String>>>() {}
        val strlist1 = strlistlist()
        val strlist2 = strlistlist()
        bitsLen = strlist1.sumOf { array->
            array.sumOf { str->
                str.toByteArray(Charsets.UTF_8).size + StringSizePayload
            } + TagIdPayload + ArraySizePayload
        } + TagIdPayload + ArraySizePayload
        template.apiTest(converter, Codec, "list1", "list2", strlist1, strlist2, bitsLen, token2)
    }

    fun beanObjEqFun(self:Any, other:Any?):Boolean {
        if (other == null || self::class.java!=other::class.java) return false
        ObjectInstanceHandler.getAllFields(self::class.java).onEach { field ->
            field.trySetAccessible()
            val selfValue = field.get(self)
            val otherValue = field.get(other)
            if (!valueEqFun(selfValue, otherValue)) {
                println("$self not equals to $other")
                println("the field ${field.name} not equals")
                return false
            }
        }
        return true
    }



    fun valueEqFun(a:Any?, b:Any?):Boolean {
        if (a==null && b==null) return true
        else if (a==null || b==null) return false
        val aClass = a::class.java
        val bClass = b::class.java

        if ( a is Map<*, *> && b is Map<*, *>) {
            val temp = b.toMutableMap()
            for (entry in a.entries) {
                if (!valueEqFun(entry.value, temp[entry.key])) return false
                temp.remove(entry.key)
            }
            return temp.isEmpty()
        }
        else if (a is List<*> && b is List<*>) {
            if (a.size != b.size) return false
            a.onEachIndexed { index, element ->
                if (!valueEqFun(element, b[index])) return false
            }
            return true
        }
        else if (a is Iterable<*> && b is Iterable<*>) {
            val aIt = a.iterator()
            val bIt = b.iterator()
            while (aIt.hasNext()) {
                val anext = aIt.next()
                val bnext = bIt.next()
                if (!valueEqFun(anext, bnext)) {
                    return false
                }
            }
            if (bIt.hasNext()) return false
            return true
        }
        else if (aClass.isArray && bClass.isArray) {
            val size = RArray.getLength(a)
            if(size!= RArray.getLength(b)) return false
            if (aClass.componentType != bClass.componentType) return false
            for (i in IntRange(0, size-1)) {
                if (!valueEqFun(RArray.get(a, i), RArray.get(b, i))) return false
            }
            return true
        }
        else if (aClass.isPrimitive || bClass.isPrimitive) {
            return a==b
        }
        else {
            return a==b
        }
    }

    class Template {
        var assertNameNotEquals = true
        var assertValueNotEquals = true
        var assertBitsLen = true
        var assertRawType = false
        var assertDecodedConvertedValueEq = true
        var noAssertions = false
        var noPrint = false
        var isHierarchicalData = true
        var testMnbt:Boolean = true

        var valueEqFun:((a:Any?, b:Any?)->Boolean)? = ApiTestTool::valueEqFun

        val soft = SoftAssertions()

        private val mockTagEq = MockTagEquals()

        fun <NbtT:Any, VT:Any> apiTest(
                tagConverter: TagConverter<NbtT>, codec: Codec<NbtT>,
                valueBitsLen: Int, typeToken: MTypeToken<VT>, valueCreation:()->VT) {
            apiTest(
                    tagConverter, codec,
                    RandomValueTool.bitStrC(5)(),
                    RandomValueTool.bitStrC(5)(),
                    valueCreation(), valueCreation(), valueBitsLen, typeToken
            )
        }


        fun <NbtT:Any, VT:Any> apiTest(
                tagConverter: TagConverter<NbtT>, codec: Codec<NbtT>,
                name1:String, name2:String,
                value1:VT, value2:VT,
                valueBitsLen: Int, typeToken: MTypeToken<out VT>) {
            val actualName1 = "name1: $name1"
            val actualName2 = "name1: $name2"
            val tag1 = tagConverter.createTag(actualName1, value1, typeToken)!!
            val tag2 = tagConverter.createTag(actualName1, value2, typeToken)!!
            val tag3 = tagConverter.createTag(actualName2, value1, typeToken)!!
            val tagFromMnbt = if (testMnbt) TestMnbt.inst.toTag(actualName1, value1, typeToken)!! else null
            val desTag = apiTest(codec, tag1, tag2, tag3, valueBitsLen)
            val value3 = tagConverter.toValue(desTag, typeToken)!!
            if (!noAssertions && assertDecodedConvertedValueEq) {
                if (!noAssertions) valueEqFun?.let { assertTrue(it(value1, value3.second)) }
            }
            println("value Equal test pass")
            if (!noAssertions && assertRawType) {
                val resRawType = MTypeToken.of(value3.second::class.java).rawType
                if (!noPrint) println("\traw type of of inputTypeToken: ${typeToken.rawType}")
                if (!noPrint) println("\traw type after encode & decoded: $resRawType")
                if (!noAssertions) assertEquals(typeToken.rawType, resRawType)
                if (!noPrint) println("\traw type equals: ${typeToken.rawType==resRawType}")
            }
            if (testMnbt) {
                assertTrue(mockTagEq.equals(tag1, tagFromMnbt))
                mnbtTest(actualName1, value1, typeToken, valueBitsLen)
            }
        }

        fun <NbtT:Any> apiTest(codec: Codec<in NbtT>, tag1: Tag<out NbtT>, tag2: Tag<out NbtT>, tag3: Tag<out NbtT>, valueBitsLen:Int): Tag<out NbtT> {
            if (!noPrint) {
                println("tag1: $tag1")
                println("tag2: $tag2")
                println("tag3: $tag3")
            }

            // prepare serialization intent
            val outputStream = ByteArrayOutputStream()
            val intent = userEncodeIntent(outputStream)

            // serialization
            codec.encode(tag1, intent)
            //val feedback2 = codec.encode(tag1, onBytesIntent)
            val bytes = outputStream.toByteArray()
            //val bytes2 = (feedback2 as EncodedBytesFeedback).bytes

            // calculate actual bytes length from serialization
            val tagHeadBitsLen = tag1.name!!.toByteArray(Charsets.UTF_8).size + TagIdPayload + StringSizePayload
            val tagLen = tagHeadBitsLen + valueBitsLen

            // prepare deserialization intent
            val inputStream = ByteArrayInputStream(bytes)
            val desIntent = userDecodeIntent(inputStream)
            //val onBytesDesIntent = userOnBytesDecodeIntent(bytes2, 0, isHierarchicalData)

            // deserialization
            val desTag = codec.decode(desIntent).tag
            //val desTag2 = codec.decode(onBytesDesIntent).tag
            if (!noPrint) println("decoded tag: $desTag")

            // Assertion Part Start
            if (!noAssertions) {
                soft.assertThat(tag1).isNotEqualTo(null) // assert not equals to null
                if (assertValueNotEquals) soft.assertThat(tag1).`as`("assert tag1 tag2 not Eq").isNotEqualTo(tag2) // assert value not equals but name equals, final not equals
                soft.assertThat(tag1).`as`("assert tag1 tag3 not Eq").isNotEqualTo(tag3) // assert name not equals but value equals, final not equals
                soft.assertThat(tag2).`as`("assert tag2 tag3 not Eq").isNotEqualTo(tag3) // assert name and value both not equals, final not equals
                soft.assertThat(tagLen).`as`("assert bits len as expected").isEqualTo(bytes.size) // assert bits len size
                soft.assertThat(inputStream.available()).`as`("assert pointer after deserialization as expected").isEqualTo(0) // check pointer after deserialization
                soft.assertThat(mockTagEq.equals(tag1, desTag)).`as`("assert tag1 decoded tag Eq") // check decoded tag as expected
                //soft.assertThat(desTag).`as`("assert on bytes result equals to on Stream result").isEqualTo(desTag2)
                //soft.assertThat(bytes.size).`as`("assert on bytes result bits len equals to on stream result").isEqualTo(bytes2.size)
                soft.assertThat(tag3).`as`("assert tag3 decoded tag not Eq").isNotEqualTo(desTag)
                if (assertValueNotEquals) soft.assertThat(tag2).`as`("assert tag3 decoded tag not Eq").isNotEqualTo(desTag) // assert value not equals but name equals, final not equals

                soft.assertAll()
            }
            // Assertion Part End
            return desTag as Tag<out NbtT>
        }

        private fun <V:Any> mnbtTest(name:String, value:V, typeToken: MTypeToken<out V>, valueBitsLen: Int) {
            val bytes1 = TestMnbt.inst.toBytes(name, value, typeToken)
            val outputStream = ByteArrayOutputStream()
            TestMnbt.inst.toStream(name, value, typeToken, outputStream)
            val bytes2 = outputStream.toByteArray()
            val tagHeadBitsLen = name!!.toByteArray(Charsets.UTF_8).size + TagIdPayload + StringSizePayload
            val tagLen = tagHeadBitsLen + valueBitsLen

            val value2 = TestMnbt.inst.fromBytes(bytes1, 0, typeToken)!!
            val value3 = TestMnbt.inst.fromStream(typeToken, ByteArrayInputStream(bytes2))!!

            assertEquals(tagLen, bytes1.size)
            assertEquals(tagLen, bytes2.size)
            if (assertDecodedConvertedValueEq) {
                assertTrue(valueEqFun(value, value2.second))
                assertTrue(valueEqFun(value, value3.second))
                assertTrue(valueEqFun(name, value2.first))
                assertTrue(valueEqFun(name, value3.first))
            }
        }

    }

    class ConverterTestTemplate{
        var assertNameNotEquals = true
        var assertValueNotEquals = true
        var noAssertions = false
        var expectedTag:Tag<out Any>? = null
        var testMnbt:Boolean = true
        val mnbtInst = TestMnbt()

        private val soft = SoftAssertions()
        private val mockTagEq = MockTagEquals()

        fun <V:Any> apiTest(name1: String, name2: String, value1:V, value2:V, typeToken:MTypeToken<V>) {
            this.testMnbt = false
            apiTest(mnbtInst.mockConverterProxy, name1, name2, value1, value2, typeToken)
        }

        fun <V:Any> apiTest(converter: TagConverter<Any>,
                            name1: String, name2: String, value1:V, value2:V, typeToken:MTypeToken<V>) {

            val tag1 = converter.createTag(name1, value1, typeToken, createTagUserIntent())!!
            val tag2 = converter.createTag(name1, value2, typeToken, createTagUserIntent())!!
            val tag3 = converter.createTag(name2, value1, typeToken, createTagUserIntent())!!
            val tag4 = if (testMnbt) TestMnbt.inst.toTag(name1, value1, typeToken)!! else null
            val fromTag1 = converter.toValue(tag1, typeToken)!!
            val fromTag2 = converter.toValue(tag2, typeToken)!!
            val fromTag4 = tag4?.let { TestMnbt.inst.fromTag(it, typeToken)!! }
            if (!noAssertions) {
                if (assertValueNotEquals) soft.assertThat(tag1).isNotEqualTo(tag2)
                if (assertNameNotEquals) soft.assertThat(tag1).isNotEqualTo(tag3)
                if (assertNameNotEquals && assertNameNotEquals) soft.assertThat(tag2).isNotEqualTo(tag3)
                soft.assertThat(valueEqFun(value1, fromTag1.second))
                soft.assertThat(valueEqFun(value2, fromTag2.second))
                if (expectedTag!=null) soft.assertThat(mockTagEq.equals(tag1, expectedTag)).`as`("assert converted tag is as expected").isTrue
                if (testMnbt) {
                    soft.assertThat(mockTagEq.equals(tag1, tag4))
                    soft.assertThat(valueEqFun(fromTag1.second, fromTag4!!.second)).`as`("assert converted tag (by Mnbt) is as expected")
                }
                soft.assertAll()
            }
        }

    }


}