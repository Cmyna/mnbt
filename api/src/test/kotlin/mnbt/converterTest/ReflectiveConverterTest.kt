package mnbt.converterTest

import com.myna.mnbt.*
import com.myna.mnbt.converter.ReflectiveConverter
import com.myna.mnbt.converter.TagLocator
import com.myna.mnbt.converter.meta.NbtPath
import com.myna.mnbt.converter.meta.TagLocatorInstance
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.CompoundTag
import mnbt.utils.*
import org.junit.jupiter.api.Assertions.*
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
        var c = 0
        var dc1Creation:()->DataClass1 = {
            c += 1
            DataClass1(Random.nextInt()+c, RandomValueTool.bitStrC(5)()+c)
        }
        var dc2Creation:()->DataClass2 = {
            val dc1 = dc1Creation()
            c += 1
            DataClass2(dc1, Random.nextDouble()+c)
        }
        var dc3Creation:(num:Int)->DataClass3 = { num->
            val list = ArrayList<DataClass2>().also { list->
                repeat(num) {
                    list.add(dc2Creation())
                }
            }
            c += 1
            DataClass3(list, Random.nextLong()+c)
        }

        val value1 = dc3Creation(10)
        val value2 = dc3Creation(8)
        val name1 = "Data Class 3-1"
        val name2 = "Data Class 3-2"
        val expectedTag = value1.toCompound(name1)
        val template = ApiTestTool.ConverterTestTemplate()
        template.expectedTag = expectedTag
        template.apiTest(name1, name2, value1, value2, object:MTypeToken<DataClass3>() {})
    }


    @Test
    fun testBuildRootToDataEntry() {
        val funBuildToDataEntry = ReflectiveConverter::class.java.declaredMethods.find { it.name=="buildRootToDataEntry" }!!
        val reflectiveConverter = TestMnbt.inst.refReflectiveConverter
        funBuildToDataEntry.trySetAccessible()
        val locator = TagLocatorInstance(CompoundTag("root"))
        val root = locator.findAt("mnbt://root", IdTagCompound) as CompoundTag

        val path = arrayOf("tag1", "tag2", "tag3")
        val compoundArr1 = arrayOfNulls<CompoundTag?>(3)

        funBuildToDataEntry.invoke(reflectiveConverter, path, compoundArr1, locator, "mnbt://root/")
        assertEquals("tag1", compoundArr1[0]!!.name)
        assertEquals("tag2", compoundArr1[1]!!.name)
        assertEquals("tag3", compoundArr1[2]!!.name)
        assertEquals(compoundArr1[0]!!, root.value["tag1"])

        val path2 = arrayOf("tag2", "tag3")
        val compArr2 = arrayOfNulls<CompoundTag?>(2)
        funBuildToDataEntry.invoke(reflectiveConverter, path2, compArr2, locator, "mnbt://root/tag1")
        assertEquals(compoundArr1[1]!!, compArr2[0]!!)
        assertEquals(compoundArr1[2]!!, compArr2[1]!!)

        val path3 = arrayOf("tag4", "tag5")
        val compArr3 = arrayOfNulls<CompoundTag?>(2)
        funBuildToDataEntry.invoke(reflectiveConverter, path3, compArr3, locator, "mnbt://root/tag1/")
        assertEquals("tag4", compArr3[0]!!.name)
        assertEquals("tag5", compArr3[1]!!.name)

        val path4 = arrayOf("tag6", "tag7")
        val compArr4 = arrayOfNulls<CompoundTag?>(2)
        funBuildToDataEntry.invoke(reflectiveConverter, path4, compArr4, locator, "mnbt://root/tag1/tag2/tag3")
        assertEquals("tag6", compArr4[0]!!.name)
        assertEquals("tag7", compArr4[1]!!.name)
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
        override fun getFieldsPaths(): Map<Field, Array<String>> {
            return testClassAFieldsPath
        }

        override fun getFieldsTagType(): Map<Field, Byte> {
            return testClassAFieldsType
        }
    }

    data class TestClassB(val classA:TestClassA, val n:Short, val d:Double, val f:Float):NbtPath {
        override fun getClassExtraPath(): Array<String> {
            return arrayOf()
        }

        override fun getFieldsPaths(): Map<Field, Array<String>> {
            return mapOf(
                    TestClassB::classA.javaField!! to arrayOf("class A tag"),
                    TestClassB::n.javaField!! to arrayOf("class A tag", "tag1", "short tag"),
                    TestClassB::d.javaField!! to arrayOf("class A tag", "tag1", "tag2", "tag3", "double tag")
            )
        }

        override fun getFieldsTagType(): Map<Field, Byte> {
            return mapOf(
                    TestClassB::classA.javaField!! to IdTagCompound,
                    TestClassB::n.javaField!! to IdTagShort,
                    TestClassB::d.javaField!! to IdTagDouble,
                    TestClassB::f.javaField!! to IdTagFloat
            )
        }
    }


    companion object {
        val testClassAPath = arrayOf("tag1", "tag2")

        val testClassAFieldsPath = HashMap<Field, Array<String>>().also {
            it[TestClassA::i.javaField!!] = arrayOf("int tag")
            it[TestClassA::valj.javaField!!] = arrayOf("tag3","string tag")
            it[TestClassA::k.javaField!!] = arrayOf("long tag")
            it[TestClassA::m.javaField!!] = arrayOf("tag3","tag4", "byte tag")
        }

        val testClassAFieldsType = mapOf(
                TestClassA::i.javaField!! to IdTagInt,
                TestClassA::valj.javaField!! to IdTagString,
                TestClassA::k.javaField!! to IdTagLong,
                TestClassA::m.javaField!! to IdTagByte
        )
    }
}



