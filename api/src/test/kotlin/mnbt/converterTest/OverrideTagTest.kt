package mnbt.converterTest

import com.myna.mnbt.IdTagCompound
import com.myna.mnbt.IdTagDouble
import com.myna.mnbt.IdTagList
import com.myna.mnbt.Tag
import com.myna.mnbt.converter.ConverterCallerIntent
import com.myna.mnbt.converter.OverrideTag
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag
import com.myna.mnbt.tag.ListTag
import com.myna.mnbt.tag.NullTag
import mnbt.utils.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class OverrideTagTest {

    // TODO: test override that not change original tag

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
    fun overrideFromFlatArrayOrList() {
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

        val doubleList = doubleArray.toList()
        var overrideTag2 = TestMnbt.inst.overrideTag(doubleList, object:MTypeToken<List<Double>>() {}, listTag) as ListTag<Double>
        assertTrue(MockTagEquals().equals(overrideTag, overrideTag2))

        // override some list
        TestMnbt.inst.completeOverride = false

        val doubleArray2 = DoubleArray(40) {Random.nextDouble()}
        overrideTag = TestMnbt.inst.overrideTag(doubleArray2, object:MTypeToken<DoubleArray>() {}, listTag) as ListTag<Double>
        assertEquals(listTag.value.size, overrideTag.value.size) // because array2 smaller, so size is array1 size
        doubleArray2.onEachIndexed { i, d ->
            assertEquals(d, overrideTag[i]!!.value)
        }
        for (i in doubleArray2.size until overrideTag.value.size) {
            assertEquals(listTag[i]!!.value, overrideTag[i]!!.value)
        }

        val doubleList2 = doubleArray2.toList()
        overrideTag2 = TestMnbt.inst.overrideTag(doubleList2, object:MTypeToken<List<Double>>() {}, listTag) as ListTag<Double>
        assertTrue(MockTagEquals().equals(overrideTag, overrideTag2))
    }

    @Test
    fun overrideFromPojo() {
        val tk = object:MTypeToken<JavaBean4>() {}
        val listNum = 13
        val bean1 = randomJavaBean4(listNum)
        val subBean = bean1.beanMap!!.entries.first()
        val subComp = TestMnbt.inst.toTag(subBean.key, subBean.value, object:MTypeToken<JavaBean2>() {})!!
        val comp1 = TestMnbt.inst.toTag("java bean 4", bean1, tk) as CompoundTag
        assertTrue(mockTagEq.equals(subComp, (comp1["beanMap"] as CompoundTag)[subBean.key]))
        assertEquals(listNum, (comp1["beanList"] as ListTag<*>).value.size)
        val bean2 = JavaBean4().also {
            it.bean4Var = "bean inst2 string"
            // only 3 element
            it.beanList = ArrayList<JavaBean>().also{ list->
                repeat(3) {
                    list.add(randomJavaBean())
                }
            }
            it.bits = Random.nextBytes(25)
            it.beanMap = HashMap()
            (it.beanMap as HashMap<String, JavaBean2>)["bean inst2 map element1"] = newJavaBean2()
        }

        // test that mock proxy got correct OverrideTag intent
        val supplier1Name = "assert delegate from bean1"
        TestMnbt.inst.mockConverterProxy.applyWhenCreateTag(supplier1Name) {
            name,value,typeToken,intent ->
            // each call should have OverrideTag intent
            assertTrue(intent is OverrideTag)
            val target = (intent as OverrideTag).overrideTarget
            when (name) {
                JavaBean4::beanList.name,JavaBean4::bean4Var.name,JavaBean3::bean3Int.name -> assertEquals(name, target!!.name)
            }
            if (name==JavaBean4::beanList.name) assertTrue(mockTagEq.equals(target!!, comp1[JavaBean4::beanList.name]))
            if (name== "bean inst2 map element1") {
                // target should be null because overridden target don't have it
                assertNull(target)
            }
        }

        val overridden = TestMnbt.inst.overrideTag(bean2, tk, comp1) as CompoundTag
        assertEquals("bean inst2 string", overridden["bean4Var"]!!.value) // override string
        // assert value in comp1 not created from bean2
        val beanMap = overridden[JavaBean4::beanMap.name] as CompoundTag
        assertNotNull(beanMap["bean2 1"])
        assertNotNull(overridden[JavaBean::d.name])
        assertEquals(bean1.d, overridden[JavaBean::d.name]!!.value)
        assertTrue(mockTagEq.equalsOrContains(beanMap, comp1["beanMap"]))
    }

    /**
     * override a list, but list target overridden compound tag is redirect by LocateAt,
     * it will accept an pojo as source, override target compound tag in a list
     */
    @Test
    fun overrideCompoundInList() {
        val tk = object: MTypeToken<DataClass6>() {}
        val dc6 = newDataClass6(true)
        val dc62 = newDataClass6(true)
        val listTag = ListTag<AnyCompound>(IdTagCompound, "root")
        repeat(4) {listTag.add(CompoundTag(null))}
        TestMnbt.inst.toTag(null, dc6, tk).also { listTag.add(it as Tag<AnyCompound>) } // the fifth is target
        val overridden = TestMnbt.inst.overrideTag(dc62, tk, listTag)
        assertTrue(overridden is ListTag<*>)
        assertTrue(MockTagEquals().structureEquals((overridden as ListTag<AnyCompound>)[5]!!, listTag[5]!!))
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

    private val mockTagEq = MockTagEquals()
}