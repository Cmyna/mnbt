package net.myna.mnbt.exceptionsTest

import net.myna.mnbt.reflect.MTypeToken
import net.myna.mnbt.tag.CompoundTag
import net.myna.mnbt.utils.ApiTestTool
import net.myna.mnbt.utils.ApiTestValueBuildTool
import net.myna.mnbt.utils.TestMnbt
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class MapTypeConverterTest {

    @Test
    /**
     * test pass an type variable TypeToken
     */
    fun typeVariableTest() {
        val tk = genericTypeToken<Int>() // in runtime it will become type variable

        // test toValue method
        val compoundTag = CompoundTag("tag").also { comp->
            ApiTestValueBuildTool.prepareTag2("int tag", 5).also{comp.add(it)}
            ApiTestValueBuildTool.prepareTag2("byte array tag", Random.Default.nextBytes(50)).also{comp.add(it)}
        }

        val expectedMap = HashMap<String, Any>().also {
            compoundTag.value.onEach { entry->
                it[entry.key] = entry.value.value
            }
        }

        val res = TestMnbt.inst.fromTag(compoundTag, tk)
        assertNotNull(res); assertNotNull(res?.second)
        val gotMap = res!!.second as Map<String, Any>
        expectedMap.onEach {
            assertTrue(ApiTestTool.valueEqFun(it.value, gotMap[it.key]))
        }

    }

    private fun <V> genericTypeToken():MTypeToken<Map<String, V>> = object:MTypeToken<Map<String, V>>() {}
}