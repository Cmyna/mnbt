package mnbt.annotationTest

import com.myna.mnbt.annotations.MapTo

class MapToTest {

    @MapTo("samplePack.sampleName")
    data class TestDataClass(val i:Int)
}