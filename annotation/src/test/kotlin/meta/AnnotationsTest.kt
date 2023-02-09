package meta

import com.myna.mnbt.annotations.FieldValueProvider
import com.myna.mnbt.annotations.FieldValueProvider.Companion.tryProvide
import com.myna.mnbt.annotations.Ignore
import com.myna.mnbt.annotations.IgnoreFromTag
import com.myna.mnbt.annotations.IgnoreToTag
import com.myna.mnbt.annotations.meta.AnnotationAlias.Companion.toAliasTarget
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.NullPointerException
import java.lang.reflect.Field
import kotlin.reflect.jvm.javaField

class AnnotationsTest {

    data class TestClassA(
            @IgnoreToTag val i:Int,
            @IgnoreFromTag(TestClassAFProvider::class)val j:Int,
            @Ignore(true, true, TestClassAFProvider::class) val k:String,
            @Ignore(true, false, Ignore.NullProvider::class) val m:Float,
            )

    class TestClassAFProvider: FieldValueProvider {
        override fun provide(field: Field): Any? {
            return when (field) {
                TestClassA::j.javaField -> 5
                TestClassA::k.javaField -> "some string"
                else -> null
            }
        }
    }


    @Test
    fun ignoreAnnTest() {
        val ignore = TestClassA::k.javaField!!.getAnnotation(Ignore::class.java)
        assertTrue(ignore is Ignore)
        val ignoreEncode = toAliasTarget(ignore, IgnoreToTag::class)
        assertTrue(ignoreEncode is IgnoreToTag)
        val ignoreDecode = toAliasTarget(ignore, IgnoreFromTag::class)
        assertTrue(ignoreDecode is IgnoreFromTag)

        // test set IgnoreWhenDecode to false
        val ignore2 = TestClassA::m.javaField!!.getAnnotation(Ignore::class.java)
        assertTrue(ignore2 is Ignore)
        val ignoreEncode2 = toAliasTarget(ignore2, IgnoreToTag::class)
        assertTrue(ignoreEncode2 is IgnoreToTag)
        assertTrue(toAliasTarget(ignore2, IgnoreFromTag::class) == null)
    }

    @Test
    fun throwProviderStateException() {
        val provider = object:FieldValueProvider{
            override fun provide(field: Field): Any? {
                throw NullPointerException()
            }
        }

        assertThrows<FieldValueProvider.StateException> {
            provider.tryProvide(TestClassA::j.javaField!!)
        }.printStackTrace()
    }
}