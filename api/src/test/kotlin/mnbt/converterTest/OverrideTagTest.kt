package mnbt.converterTest

import com.myna.mnbt.IdTagDouble
import com.myna.mnbt.Tag
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.CompoundTag
import com.myna.mnbt.tag.ListTag
import mnbt.utils.ApiTestTool
import mnbt.utils.ApiTestValueBuildTool
import mnbt.utils.TestMnbt
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class OverrideTagTest {

    // TODO: complete test to all cases

    @Test
    fun overrideFlatTag() {
        val i = 5
        val itag = ApiTestValueBuildTool.prepareTag2("int tag", 0)
        var override = TestMnbt.inst.overrideTag(i, MTypeToken.of(Int::class.java), itag)
        assertEquals(override!!.name, itag.name)
        assertEquals(override.id, itag.id)
        assertEquals(override.value, i)

        // if wrong tag type, override will return null
        override = TestMnbt.inst.overrideTag(i, MTypeToken.of(Short::class.java), itag)
        assertNull(override)
    }

    @Test
    fun overrideFromFlatMap() {
        val mapToken = object: MTypeToken<Map<String, Any>>() {}
        // flat map override
        val (map1,map2) = prepareFlatMap()

        val comp1 = TestMnbt.inst.toTag("map1", map1, mapToken) as CompoundTag
        assertEquals(comp1[extraKey1], ApiTestValueBuildTool.prepareTag2(extraKey1, map1[extraKey1] as Any))

        val comp2 = TestMnbt.inst.toTag("map2", map2, mapToken) as CompoundTag

        val compOverridden = TestMnbt.inst.overrideTag(map1, mapToken, comp2) as CompoundTag
        map1.onEach {
            val subTag = compOverridden[it.key]
            assertNotNull(subTag)
            assertTrue(ApiTestTool.valueEqFun(it.value, subTag!!.value))
        }
        assertNotNull(compOverridden[extraKey2])
        assertEquals(compOverridden[extraKey2]!!.value, map2[extraKey2])
    }

    @Test
    fun overrideFromNestMap() {
        val mapToken = object: MTypeToken<Map<String, Any>>() {}
        val (map1,map2) = prepareFlatMap()
        val map3 = HashMap<String, Any>().also {it["map1"] = map1}
        val map4 = HashMap<String, Any>().also {it["map1"] = map2}

        val comp1 = TestMnbt.inst.toTag("map3", map3, mapToken) as CompoundTag
        val comp2 = TestMnbt.inst.toTag("map4", map4, mapToken) as CompoundTag

        assertEquals((comp1["map1"] as CompoundTag)[extraKey1], ApiTestValueBuildTool.prepareTag2(extraKey1, map1[extraKey1] as Any))
        assertEquals((comp2["map1"] as CompoundTag)[extraKey2], ApiTestValueBuildTool.prepareTag2(extraKey2, map2[extraKey2] as Any))

        val compOverridden = TestMnbt.inst.overrideTag(map3, mapToken, comp2) as CompoundTag
        val map1Comp = compOverridden["map1"] as CompoundTag
        map1.onEach {
            val subTag = map1Comp[it.key]
            assertNotNull(subTag)
            assertTrue(ApiTestTool.valueEqFun(it.value, subTag!!.value))
        }
        assertNotNull(map1Comp[extraKey2])
        assertEquals(map1Comp[extraKey2]!!.value, map2[extraKey2])
    }

    @Test
    fun overrideFromFlatArray() {
        // there is an option from Mnbt which specifies override the whole list or just replace some value in target list tag

        // override whole list
        val listTag = ApiTestValueBuildTool.listPreparation(100) { Random.nextDouble() }.let { vl->
            ListTag<Double>(IdTagDouble, "double list tag 1").also {
                vl.map { d-> ApiTestValueBuildTool.prepareTag2(null, d)}.onEach { dt ->
                    it.add(dt as Tag<Double>)
                }
            }
        }
        val doubleArray = DoubleArray(80) {Random.nextDouble()}

        var overrideTag = TestMnbt.inst.overrideTag(doubleArray, object:MTypeToken<DoubleArray>() {}, listTag) as ListTag<Double>
        assertEquals(doubleArray.size, overrideTag.value.size)
        doubleArray.onEachIndexed { i, d ->
            assertEquals(d, overrideTag[i]!!.value)
        }

        // override some list
        TestMnbt.inst.overridePartOfList = true

        val doubleArray2 = DoubleArray(40) {Random.nextDouble()}
        overrideTag = TestMnbt.inst.overrideTag(doubleArray2, object:MTypeToken<DoubleArray>() {}, overrideTag) as ListTag<Double>
        assertEquals(doubleArray.size, overrideTag.value.size) // because array2 smaller, so size is array1 size
        doubleArray2.onEachIndexed { i, d ->
            assertEquals(d, overrideTag[i]!!.value)
        }
        for (i in doubleArray2.size until doubleArray2.size) {
            assertEquals(doubleArray[i], overrideTag[i]!!.value)
        }

    }

    private fun prepareFlatMap():Pair<HashMap<String, Any>, HashMap<String, Any>> {
        val map1 = ApiTestValueBuildTool.flatMapListsPreparation("map1", 1) {HashMap()}.third.first() as HashMap<String, Any>
        val map2 = ApiTestValueBuildTool.flatMapListsPreparation("map1", 1) {HashMap()}.third.first() as HashMap<String, Any>
        map1[extraKey1] = 5555555
        map2[extraKey2] = "extra string tag in map2"
        return Pair(map1, map2)
    }

    private val extraKey1 = "map1 extra tag"
    private val extraKey2 = "map2 extra tag"
}