package mnbt.converterTest

import com.myna.mnbt.*
import com.myna.mnbt.converter.meta.NbtPath
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.CompoundTag
import mnbt.utils.ApiTestTool
import mnbt.utils.ApiTestValueBuildTool
import mnbt.utils.TestMnbt
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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
        val tag2 = aComp.value.find {tag->tag.name == "tag2"} as CompoundTag
        val tag3 = tag2.value.find {tag->tag.name=="tag3"} as CompoundTag
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
    fun testFieldConversionHelper() {
        val reflectiveConverter = TestMnbt.inst.refReflectiveConverter
        val helper = reflectiveConverter::class.java.getDeclaredMethod("buildFieldTagContainers", Map::class.java)
        helper.trySetAccessible()
        val res = helper.invoke(reflectiveConverter, testClassAFieldsPath) as Map<String, Array<CompoundTag>>
        assertEquals(res["valj"]!!.size, 1)
        assertEquals(res["m"]!!.size, 2)
        assertEquals(res["valj"]!![0], res["m"]!![0])
        assertEquals(res["valj"]!![0].name, "tag3")
        assertEquals(res["m"]!![1].name, "tag4")
        assertTrue(res["i"]!!.isEmpty())
        assertTrue(res["k"]!!.isEmpty())

        testClassAFieldsPath["null"] = arrayOf()
        helper.invoke(reflectiveConverter, testClassAFieldsPath) as Map<String, Array<CompoundTag>>
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


    data class TestClassA(val i:Int, val valj:String, val k:Long, val m:Byte):NbtPath {
        override fun getClassExtraPath(): Array<String> {
            return testClassAPath
        }
        override fun getFieldsPaths(): Map<String, Array<String>> {
            return testClassAFieldsPath
        }

        override fun getFieldsTagType(): Map<String, Byte> {
            return testClassAFieldsType
        }
    }

    data class TestClassB(val classA:TestClassA, val n:Short, val d:Double, val f:Float):NbtPath {
        override fun getClassExtraPath(): Array<String> {
            return arrayOf()
        }

        override fun getFieldsPaths(): Map<String, Array<String>> {
            return mapOf(
                    "classA" to arrayOf("class A tag"),
                    "n" to arrayOf("class A tag", "tag1", "short tag"),
                    "d" to arrayOf("class A tag", "tag1", "tag2", "tag3", "double tag")
            )
        }

        override fun getFieldsTagType(): Map<String, Byte> {
            return mapOf(
                    TestClassB::classA.name to IdTagCompound,
                    TestClassB::n.name to IdTagShort,
                    TestClassB::d.name to IdTagDouble,
                    TestClassB::f.name to IdTagFloat
            )
        }
    }


    companion object {
        val testClassAPath = arrayOf("tag1", "tag2")

        val testClassAFieldsPath = HashMap<String, Array<String>>().also {
            it["i"] = arrayOf("int tag")
            it["valj"] = arrayOf("tag3","string tag")
            it["k"] = arrayOf("long tag")
            it["m"] = arrayOf("tag3","tag4", "byte tag")
        }

        val testClassAFieldsType = mapOf(
                "i" to IdTagInt,
                "valj" to IdTagString,
                "k" to IdTagLong,
                "m" to IdTagByte
        )
    }
}



