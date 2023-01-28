package mnbt.profilingTest

import com.myna.utils.AdaptedInputStream
import com.myna.mnbt.Tag
import com.myna.mnbt.codec.*
import com.myna.mnbt.reflect.MTypeToken
import mnbt.utils.ApiTestValueBuildTool
import mnbt.utils.TestMnbt
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.collections.HashMap

class PerformanceProfilingTest {

    /**
     * test all the possible tag conversion & Codec process, without any extra assertion/comparison
     *
     * this test aims to provide clean running process to performance profilers
     */
    @Test
    fun flatTypeIntegratedTest() {
        val list = ApiTestValueBuildTool.flatTagValuesPreparation()
        list.onEach {
            testProcess(it.first, it.third, MTypeToken.of(it.third::class.java) as MTypeToken<Any>)
        }
    }

    @Test
    fun listWithFlatTypeIntegratedTest() {
        // array type
        val list = ApiTestValueBuildTool.flatValueArraysPreparation(20)
        list.onEach {
            testProcess(it.first, it.third, object:MTypeToken<Array<Any>>() {} as MTypeToken<Any>)
        }

        //list type
        val list2 = ApiTestValueBuildTool.flatValueListsPreparation(20)
        list2.onEach {
            testProcess(it.first, it.third, object:MTypeToken<List<Any>>() {} as MTypeToken<Any>)
        }
    }

    @Test
    fun mapTest() {
        // flat map
        val map = ApiTestValueBuildTool.nestedMapValuesPreparation(0) { HashMap() }
        testProcess(map.first, map.third, object:MTypeToken<Map<String, Any>>() {} as MTypeToken<Any>)

        // nested map with depth:4
        val map2 = ApiTestValueBuildTool.nestedMapValuesPreparation(4) { HashMap() }
        testProcess(map2.first, map2.third, object:MTypeToken<Map<String, Any>>() {} as MTypeToken<Any>)
    }

    /**
     * use for profiling value preparation(memory record)
     */
    fun flatTagCodecValuePreparationTest() {
        val list = ApiTestValueBuildTool.flatTagsPreparation(true)
        while(true) { // let test keep running for heap iterator analyse
            var name:String? = ""
            var flag = false
            list.onEach {
                if (name!=it.name) flag = true
            }
            if (flag) break
        }
    }

    @Test
    fun flatTagsCodecOnStreamTest() {
        val list = ApiTestValueBuildTool.flatTagsPreparation(true)
        println("list size: ${list.size}")
        repeat(1000) {
            list.onEach { codecProcess(it) }
        }
    }

    @Test
    fun flatTagsCodecOnBytesTest() {
        val list = ApiTestValueBuildTool.flatTagsPreparation(true)
        println("list size: ${list.size}")
        repeat(1000) {
            list.onEach {
                val bits = TestMnbt.inst.encode(it)
                TestMnbt.inst.decode(bits, 0)
            }
        }
    }

    @Test
    fun listWithFlatTagTest() {
        val list = ApiTestValueBuildTool.flatValuesListTagsPreparations(20)
        repeat(1000) {
            list.onEach { codecProcess(it) }
        }
    }

    @Test
    fun CodecCallerIntentCreationCost() {
        repeat(1000*9400) {
            val i2 = userEncodeIntent(ByteArrayOutputStream())
        }
    }


    private fun testProcess(name:String, value:Any, typeToken: MTypeToken<Any>) {
        val tag = convertToTagTest(name, value, typeToken)
        val bits = tagToBitsTest(tag)
        val desTag = bitsToTagTest(bits)
        val value = tagToValueTest(desTag, typeToken)
    }

    private fun codecProcess(tag: Tag<out Any>) {
        val bits = tagToBitsTest(tag)
        val desTag = bitsToTagTest(bits)
    }


    private fun convertToTagTest(name:String, value:Any, typeToken: MTypeToken<Any>): Tag<out Any> {
        return TestMnbt.inst.tConverterProxy.createTag(name, value, typeToken)!!
    }

    private fun tagToValueTest(tag: Tag<out Any>, typeToken: MTypeToken<Any>): Any {
        return TestMnbt.inst.tConverterProxy.toValue(tag, typeToken)!!.second
    }

    private fun bitsToTagTest(bits:ByteArray): Tag<out Any> {
        val inputStream = AdaptedInputStream(bits)
        val intent = userDecodeIntent(inputStream)
        return TestMnbt.inst.tCodecProxy.decode(intent).tag
    }

    private fun tagToBitsTest(tag: Tag<out Any>):ByteArray {
        val outputStream = ByteArrayOutputStream()
        val intent = userEncodeIntent(outputStream)
        val feedback = TestMnbt.inst.tCodecProxy.encode(tag, intent)
        val bits = outputStream.toByteArray()
        return bits
    }

}