package mnbt.annotationTest

import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag
import mnbt.utils.DataClass4
import mnbt.utils.MockTagEquals
import mnbt.utils.TestMnbt
import mnbt.utils.newDataClass4
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OverrideTagWithLocateAt {

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
    }

    @Test
    fun overrideComplicateDataClassWithLocateAt() {
        TODO()
    }
}