package mnbt.annotationTest

import com.myna.mnbt.annotations.LinkTo
import com.myna.mnbt.converter.meta.NbtPath
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LinkToTest {

    @LinkTo("samplePack/sampleName")
    data class TestDataClass(val i:Int)

    @Test
    fun annotationProcessTest() {
        // the class annotated with LinkTo should have implemented interface NbtPath
        val a = TestDataClass(1)
        val implementNbtPath = TestDataClass::class.java.interfaces.any {
            it==NbtPath::class.java
        }
        assertTrue(implementNbtPath, "target class ${TestDataClass::class.java} not implemented NbtPath interface")
    }
}