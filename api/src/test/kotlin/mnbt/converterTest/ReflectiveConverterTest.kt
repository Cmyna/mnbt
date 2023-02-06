package mnbt.converterTest

import com.myna.mnbt.*
import com.myna.mnbt.annotations.LocateAt

import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.CompoundTag
import mnbt.utils.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

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
        val dc1Creation:()->DataClass1 = {
            c += 1
            DataClass1(Random.nextInt()+c, RandomValueTool.bitStrC(5)()+c)
        }
        val dc2Creation:()->DataClass2 = {
            val dc1 = dc1Creation()
            c += 1
            DataClass2(dc1, Random.nextDouble()+c)
        }
        val dc3Creation:(num:Int)->DataClass3 = { num->
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
    fun cleanRedundantTagTest() {
        val value1 = TestClassC(5)
        val value2 = TestClassC(2)
        val name1 = "Test Class C-1"
        val name2 = "Test Class C-2"

        val template = ApiTestTool.ConverterTestTemplate()
        template.assertValueNotEquals = false

        val mockConverterProxy = template.mnbtInst.mockConverterProxy
        mockConverterProxy.createMockTagSupplier.add { name,_,_,_->
            if (name=="tag2") MockConverterProxy.CreateTagMockFeedback(true, null)
            else MockConverterProxy.CreateTagMockFeedback(false, null)
        }

        val expectedTag = CompoundTag(name1)
        template.expectedTag = expectedTag

        template.apiTest(name1, name2, value1, value2, object:MTypeToken<TestClassC>() {})
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
    data class TestClassA(
            @LocateAt("int tag", IdTagInt) val i:Int,
            @LocateAt("./tag3/string tag", IdTagString) val valj:String,
            @LocateAt("long tag", IdTagLong) val k:Long,
            @LocateAt("./tag3/tag4/byte tag", IdTagByte) val m:Byte)

    data class TestClassB(
            @LocateAt("class A tag") val classA:TestClassA,
            @LocateAt("./class A tag/tag1/short tag", IdTagShort) val n:Short,
            @LocateAt("./class A tag/tag1/tag2/tag3/double tag", IdTagDouble) val d:Double,
            val f:Float)

    data class TestClassC(
            @LocateAt("./tag1/tag2/tag3/int tag", IdTagInt) val v1:Int
    )
}



