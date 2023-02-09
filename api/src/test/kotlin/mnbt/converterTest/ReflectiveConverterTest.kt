package mnbt.converterTest

import com.myna.mnbt.annotations.FieldValueProvider
import com.myna.mnbt.annotations.Ignore
import com.myna.mnbt.annotations.LocateAt

import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.CompoundTag
import mnbt.utils.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Field
import kotlin.random.Random
import kotlin.reflect.jvm.javaField

class ReflectiveConverterTest {

    @Test
    fun classWithNbtPathITest() {
        val rootName = "root"
        val testClassA = TestClassA(515, "string value", 2333333333333333, 35)
        val testClassA2 = TestClassA(0, "string value2", 9150055533881563, -127)
        val classATag = getClassACompound(testClassA)
        val expectedCompound = CompoundTag(rootName).also { root->
            root.add(classATag)
        }

        val template = ApiTestTool.ConverterTestTemplate()
        template.expectedTag = expectedCompound
        template.apiTest(TestMnbt.inst.refConverterProxy, rootName, "root2", testClassA, testClassA2, object:MTypeToken<TestClassA>() {})
    }

    @Test
    fun restructureTagTest() {
        val testClassA = TestClassA(515, "string value", 2333333333333333, 35)
        val testClassB = TestClassB(testClassA, 23, 5.0, 0.8887f)
        val testClassB2 = TestClassB(testClassA, 555, 0.3, 55555f)
        val aComp = getClassACompound(testClassA)
        val tag2 = aComp.value["tag2"] as CompoundTag
        val tag3 = tag2.value["tag3"] as CompoundTag
        val bCompName = "test class B tag"
        val bComp = CompoundTag(bCompName).also { bComp->
            CompoundTag("class A tag").also {
                it.add(aComp)
                bComp.add(it)
            }
            ApiTestValueBuildTool.prepareTag2("short tag", testClassB.n).also {aComp.add(it)}
            ApiTestValueBuildTool.prepareTag2("double tag", testClassB.d).also {tag3.add(it)}
            ApiTestValueBuildTool.prepareTag2(TestClassB::f.name, testClassB.f).also {bComp.add(it)}
        }
        val template = ApiTestTool.ConverterTestTemplate()
        template.expectedTag = bComp
        template.apiTest(TestMnbt.inst.refConverterProxy, bCompName, "root2", testClassB, testClassB2, object:MTypeToken<TestClassB>() {})
    }

    @Test
    fun complicateDataClassesTest() {
        val value1 = testClass3(10)
        val value2 = testClass3(8)
        val name1 = "Data Class 3-1"
        val name2 = "Data Class 3-2"
        val expectedTag = value1.toCompound(name1)
        val template = ApiTestTool.ConverterTestTemplate()
        template.expectedTag = expectedTag
        template.apiTest(name1, name2, value1, value2, object:MTypeToken<DataClass3>() {})

        // remap tags to TestClassD
        val dc1 = value1.dataClass2List[3].dataClass1
        val expectedD = TestClassD(dc1, dc1.j, value1.dc3L, null)
        val result = TestMnbt.inst.fromTag(expectedTag, object:MTypeToken<TestClassD>() {})
        assertEquals(expectedD, result?.second)
    }

    @Test
    fun cleanRedundantTagTest() {
        var value1 = TestClassC(5)
        val value2 = TestClassC(2)
        val name1 = "Test Class C-1"
        val name2 = "Test Class C-2"

        val template = ApiTestTool.ConverterTestTemplate()
        template.assertValueNotEquals = false

        val mockConverterProxy = template.mnbtInst.mockConverterProxy
        mockConverterProxy.createMockTagSupplier["catch int tag"] = { name, _, _, _->
            if (name=="int tag") MockConverterProxy.CreateTagMockFeedback(true, null)
            else MockConverterProxy.CreateTagMockFeedback(false, null)
        }

        val expectedTag = CompoundTag(name1)
        template.expectedTag = expectedTag

        template.apiTest(name1, name2, value1, value2, object:MTypeToken<TestClassC>() {})

        // one value created but another not
        value1 = TestClassC(100, "some string")
        val tag1 = CompoundTag("tag1").also {expectedTag.add(it)}
        ApiTestValueBuildTool.prepareTag2("string tag", value1.v2!!).also { tag1.add(it) }

        template.apiTest(name1, name2, value1, value2, object:MTypeToken<TestClassC>() {})
    }

    @Test
    fun ignoreAnnotationTest() {
        val tc3 = testClass3(10)
        val dc1 = tc3.dataClass2List[3].dataClass1
        val tcd = TestClassD(dc1, dc1.j, tc3.dc3L, "")
        val name1 = "Data Class 3-1"
        val name2 = "test class d"
        val expectedTag = tc3.toCompound(name1)

        val expectedTag2 = tcd.toCompound(name2)
        val tag = TestMnbt.inst.toTag(name2, tcd, object:MTypeToken<TestClassD>() {})
        assertTrue(MockTagEquals().equals(expectedTag2, tag))
    }

    private fun getClassACompound(testClassA:TestClassA):CompoundTag {
        val classADataContainerTag = CompoundTag("tag2").also { comp->
            ApiTestValueBuildTool.prepareTag2("int tag", testClassA.i).also { comp.add(it) }
            ApiTestValueBuildTool.prepareTag2("long tag", testClassA.k).also { comp.add(it) }

            val tag3 = CompoundTag("tag3")
            ApiTestValueBuildTool.prepareTag2("string tag", testClassA.valj).also {tag3.add(it)}
            val tag4 = CompoundTag("tag4").also {tag3.add(it)}
            ApiTestValueBuildTool.prepareTag2("byte tag", testClassA.m).also {tag4.add(it)}
            comp.add(tag3)
        }
        return CompoundTag("tag1").also { tag1->
            tag1.add(classADataContainerTag)
        }
    }


    @LocateAt("./tag1/tag2")
    private data class TestClassA(
            @LocateAt("int tag") val i:Int,
            @LocateAt("./tag3/string tag") val valj:String,
            @LocateAt("long tag") val k:Long,
            @LocateAt("./tag3/tag4/byte tag") val m:Byte)

    private data class TestClassB(
            @LocateAt("class A tag") val classA:TestClassA,
            @LocateAt("./class A tag/tag1/short tag") val n:Short,
            @LocateAt("./class A tag/tag1/tag2/tag3/double tag") val d:Double,
            val f:Float)

    private data class TestClassC(
            @LocateAt("./tag1/tag2/tag3/int tag") val v1:Int,
            @LocateAt("./tag1/string tag") val v2:String? = null
    )

    /**
     * test class that try to remap tags created from DataClass3
     */
    private data class TestClassD(
            @LocateAt("./dataClass2List/dataClass1/", "./dataClass2List/#3/dataClass1/") var dataClass1:DataClass1,
            @LocateAt("./dataClass2List/dataClass1/j", "./dataClass2List/#3/dataClass1/j") var str:String,
            @LocateAt("./dc3L") var dc3L:Long,
            @Ignore(true, true, FVProvider::class)val s:String?
    )

    private fun TestClassD.toCompound(name:String?):CompoundTag {
        val tcd = CompoundTag(name)
        val dataClass2List = CompoundTag("dataClass2List")
        tcd.add(dataClass2List)
        val dataClass1 = this.dataClass1.toCompound("dataClass1")
        dataClass2List.add(dataClass1)
        ApiTestValueBuildTool.prepareTag2("j", this.str).also {dataClass1.add(it)}
        ApiTestValueBuildTool.prepareTag2("dc3L", this.dc3L).also {tcd.add(it)}
        return tcd
    }


    private class FVProvider: FieldValueProvider {
        override fun provide(field: Field): Any? {
            return if (field == TestClassD::s.javaField) testStr1
            else null
        }
    }

    private fun dataClass1():DataClass1 {
        c += 1
        return DataClass1(Random.nextInt()+c, RandomValueTool.bitStrC(5)()+c)
    }

    private fun testClass2():DataClass2  {
        val dc1 = dataClass1()
        c += 1
        return DataClass2(dc1, Random.nextDouble()+c)
    }

    private fun testClass3(num:Int):DataClass3 {
        val list = ArrayList<DataClass2>().also { list->
            repeat(num) {
                list.add(testClass2())
            }
        }
        c += 1
        return DataClass3(list, Random.nextLong()+c)
    }

    companion object {

        private const val testStr1 = ""

        private var c = 0

    }
}



