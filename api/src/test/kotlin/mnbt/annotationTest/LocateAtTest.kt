package mnbt.annotationTest

import com.myna.mnbt.annotations.LocateAt
import com.myna.mnbt.converter.OverrideTag
import com.myna.mnbt.converter.meta.NbtPathTool
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag
import com.myna.mnbt.tag.UnknownCompound
import mnbt.utils.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LocateAtTest {

    @Test
    fun overrideFlatDataClass() {
        val tk = object: MTypeToken<DataClass4>() {}
        val obj1 = newDataClass4(true)
        val obj2 = newDataClass4(true)

        val comp1 = TestMnbt.inst.toTag("object 1", obj1, tk) as CompoundTag
        // simple assertion before test override
        val dataEntry = (comp1["midTag1"] as CompoundTag)["data class 4 entry"]
        assertTrue(dataEntry is CompoundTag)
        assertEquals(3, (dataEntry!!.value as AnyCompound).size)

        // test override
        val comp2 = TestMnbt.inst.overrideTag(obj2, tk, comp1)
        assertTrue(MockTagEquals().structureEquals(comp1, comp2))
        val comp2DataEntryTag = NbtPathTool.goto(comp2!!, "./midTag1/data class 4 entry")
        assertEquals(obj2.dc4Int, NbtPathTool.goto(comp2DataEntryTag!!, "./midTag2/int tag in data class 4 tag")!!.value)
        // assert DataClass4::dc1 is ignored
        assertNull(NbtPathTool.goto(comp2DataEntryTag, "./dc1"))
    }

    @Test
    fun correctDelegateFromPOJO() {
        val obj1 = DataClass5(newDataClass4(true))
        val obj2 = DataClass5(newDataClass4(true))
        val tk = object: MTypeToken<DataClass5>() {}

        val comp1 = TestMnbt.inst.toTag("root", obj1, tk)!!

        TestMnbt.inst.mockConverterProxy.applyWhenCreateTag("check delegate") { name,value,typeToken,intent->
            if (value is DataClass4) {
                assertEquals("data class 4 field entry", name)
                // assert OverrideTag target
                assertTrue(intent is OverrideTag)
                val target = (intent as OverrideTag).overrideTarget
                assertEquals(name, target!!.name)
                assertTrue(target.value is UnknownCompound)
            }
        }
        val comp2 = TestMnbt.inst.overrideTag(obj2, tk, comp1)
        assertTrue(MockTagEquals().structureEquals(comp1, comp2))
        assertFalse(MockTagEquals().equals(comp1, comp2))
    }

    /**
     * this test is for checking not create redundant tag when two fields in same class have specified part of overlapping paths
     */
    @Test
    fun noRedundantTagCreateTest() {
        var value1 = TestClassA(5)
        val value2 = TestClassA(2)
        val name1 = "Test Class A-1"
        val name2 = "Test Class A-2"

        val template = ApiTestTool.ConverterTestTemplate()
        template.assertValueNotEquals = false

        val mockConverterProxy = template.mnbtInst.mockConverterProxy
        mockConverterProxy.createMockTagSupplier["catch int tag"] = { name, _, _, _->
            if (name=="int tag") MockConverterProxy.CreateTagMockFeedback(true, null)
            else MockConverterProxy.CreateTagMockFeedback(false, null)
        }

        val expectedTag = CompoundTag(name1)
        template.expectedTag = expectedTag

        template.apiTest(name1, name2, value1, value2, object:MTypeToken<TestClassA>() {})

        // one value created but another not
        value1 = TestClassA(100, "some string")
        val tag1 = CompoundTag("tag1").also {expectedTag.add(it)}
        ApiTestValueBuildTool.prepareTag2("string tag", value1.v2!!).also { tag1.add(it) }

        template.apiTest(name1, name2, value1, value2, object:MTypeToken<TestClassA>() {})
    }

    private data class TestClassA(
            @LocateAt("./tag1/tag2/tag3/int tag") val v1:Int,
            @LocateAt("./tag1/string tag") val v2:String? = null
    )
}