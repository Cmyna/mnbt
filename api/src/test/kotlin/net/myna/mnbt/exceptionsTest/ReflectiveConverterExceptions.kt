package net.myna.mnbt.exceptionsTest


import net.myna.mnbt.reflect.MTypeToken
import net.myna.mnbt.tag.CompoundTag
import net.myna.mnbt.utils.JavaBean
import net.myna.mnbt.utils.TestMnbt
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReflectiveConverterExceptions {

    @Test
    fun nullablePropertiesTest() {
        val bean = JavaBean()
        val reflectiveConverter = TestMnbt.inst.refReflectiveConverter
        val tk = object: MTypeToken<JavaBean>() {}
        reflectiveConverter.returnObjectWithNullableProperties = false // set it to false
        var tag = reflectiveConverter.createTag("empty bean", bean, tk)!!
        assertEquals(tag.value.size, 0)
        var res = reflectiveConverter.toValue(tag, tk)
        assertEquals(res, null)
        reflectiveConverter.returnObjectWithNullableProperties = true // set it to true
        res = reflectiveConverter.toValue(tag, tk)!!
        assertEquals(bean, res.second)

        bean.j = "some string"
        tag = reflectiveConverter.createTag("empty bean", bean, tk)!!
        res = reflectiveConverter.toValue(tag, tk)!!
        assertEquals(bean, res.second)

        reflectiveConverter.returnObjectWithNullableProperties = false // set it to false
        res = reflectiveConverter.toValue(tag, tk)
        assertEquals(res, null)
    }



    @Test
    fun exceptInterfaceOrAbstractClass() {
        val testComp = CompoundTag()

        val testI = TestMnbt.inst.fromTag(testComp, object:MTypeToken<TestI>() {})
        assertNull(testI)

        val testClassA = TestMnbt.inst.fromTag(testComp, object:MTypeToken<TestClassA>() {})
        assertNotNull(testClassA)
        assertEquals(testClassA!!.first, null)

        val testAbs = TestMnbt.inst.fromTag(testComp, object:MTypeToken<TestAbstract>() {})

        assertNull(testAbs)
    }

    interface TestI

    private class TestClassA

    private abstract class TestAbstract
}