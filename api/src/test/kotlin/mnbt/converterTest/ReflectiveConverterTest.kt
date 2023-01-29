package mnbt.converterTest

import com.myna.mnbt.IdTagByte
import com.myna.mnbt.IdTagInt
import com.myna.mnbt.IdTagLong
import com.myna.mnbt.IdTagString
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
        val classARoot = CompoundTag("tag2").also { comp->
            ApiTestValueBuildTool.prepareTag2("int tag", testClassA.i).also { comp.add(it) }
            ApiTestValueBuildTool.prepareTag2("long tag", testClassA.k).also { comp.add(it) }

            val tag3 = CompoundTag("tag3")
            ApiTestValueBuildTool.prepareTag2("string tag", testClassA.valj).also {tag3.add(it)}
            val tag4 = CompoundTag("tag4").also {tag3.add(it)}
            ApiTestValueBuildTool.prepareTag2("byte tag", testClassA.m).also {tag4.add(it)}
            comp.add(tag3)
        }
        val expectedCompound = CompoundTag(rootName).also { root->
            CompoundTag("tag1").also { tag1->
                tag1.add(classARoot)
                root.add(tag1)
            }
        }

        val template = ApiTestTool.ConverterTestTemplate()
        template.expectedTag = expectedCompound
        template.apiTest(TestMnbt.inst.refConverterProxy, rootName, "root2", testClassA, testClassA2, object:MTypeToken<TestClassA>() {})
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


    data class TestClassA(val i:Int, val valj:String, val k:Long, val m:Byte):NbtPath {
        override fun getClassNbtPath(): Array<String> {
            return testClassAPath
        }
        override fun getFieldsPaths(): Map<String, Array<String>> {
            return testClassAFieldsPath
        }

        override fun getFieldsTagType(): Map<String, Byte> {
            return testClassAFieldsType
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



