package mnbt.annotationTest

import com.myna.mnbt.annotation.MapTo

class MapToTest {

    @MapTo("samplePack.sampleName")
    data class TestDataClass(val i:Int)
}