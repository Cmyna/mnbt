package mnbt.converterTest

import com.myna.mnbt.converter.meta.NbtPath
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.CompoundTag
import mnbt.utils.ApiTestTool
import mnbt.utils.ApiTestValueBuildTool
import mnbt.utils.TestMnbt
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
        val helper = reflectiveConverter::class.java.getDeclaredMethod("buildFieldTagContainers")
        val res = helper.invoke(testClassAFieldsPath) as Map<String, Array<CompoundTag?>>

    }


    data class TestClassA(val i:Int, val valj:String, val k:Long, val m:Byte):NbtPath {
        override fun getRemappedClass(): Array<String> {
            return testClassAPath
        }
        override fun getRemappedField(): Map<String, Array<String>> {
            return testClassAFieldsPath
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
    }
}



