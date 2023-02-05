package meta

import com.myna.mnbt.annotations.DefaultFieldValueProvider
import com.myna.mnbt.annotations.Ignore
import com.myna.mnbt.annotations.IgnoreDecode
import com.myna.mnbt.annotations.IgnoreEncode
import com.myna.mnbt.annotations.meta.AnnotationAlias
import com.myna.mnbt.annotations.meta.toAliasTarget
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.jvm.javaField

class AnnotationAliasTest {

    data class TestClassA(
            @IgnoreEncode val i:Int,
            @IgnoreDecode(ProvideJ::class)val j:Int,
            @Ignore(true, true, ProvideK::class)val k:String
            )

    class ProvideJ: DefaultFieldValueProvider<Int> {
        override fun provide(): Int = 5
    }

    class ProvideK: DefaultFieldValueProvider<String> {
        override fun provide(): String = "some string"
    }

    @Test
    fun ignoreAnnTest() {
        val ignore = TestClassA::k.javaField!!.getAnnotation(Ignore::class.java)
        assertTrue(ignore is Ignore)
        val ignoreEncode = toAliasTarget(ignore, IgnoreEncode::class)
        assertTrue(ignoreEncode is IgnoreEncode)
        val ignoreDecode = toAliasTarget(ignore, IgnoreDecode::class)
        assertTrue(ignoreDecode is IgnoreDecode)
    }
}