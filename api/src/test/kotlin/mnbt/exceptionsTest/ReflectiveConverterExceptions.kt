package mnbt.exceptionsTest


import com.myna.mnbt.reflect.MTypeToken
import mnbt.utils.JavaBean
import mnbt.utils.TestMnbt
import org.junit.jupiter.api.Assertions.assertEquals
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
}