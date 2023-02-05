package mnbt

import com.myna.mnbt.IdTagByteArray
import com.myna.mnbt.IdTagCompound
import com.myna.mnbt.Tag
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag
import com.myna.mnbt.tag.ListTag
import mnbt.utils.ApiTestValueBuildTool
import mnbt.utils.MockTagEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class MockTagEqTest {

    private val mockTagEquals = MockTagEquals()

    @Test
    fun nameNeqTest() {
        val t1 = ApiTestValueBuildTool.prepareTag2("tag1", 1)
        val t2 = ApiTestValueBuildTool.prepareTag2("tag2", 1)
        assertFalse(mockTagEquals.equals(t1, t2))
    }

    @Test
    fun typeNeqTest() {
        val t1 = ApiTestValueBuildTool.prepareTag2("tag1", 1)
        val t2 = ApiTestValueBuildTool.prepareTag2("tag1", 1.toShort())
        assertFalse(mockTagEquals.equals(t1, t2))
        assertFalse(mockTagEquals.equals(t1, null))
        assertFalse(mockTagEquals.equals(t1, 1))
    }

    @Test
    fun flatCompoundTagTest() {
        val comp1 = prepareFlatCompound("comp1")
        val comp2 = prepareFlatCompound("comp1")
        assertTrue(mockTagEquals.equals(comp1, comp2)) // output nothing if true

        ApiTestValueBuildTool.prepareTag2("comp2 extra", 55).also {comp2.add(it)}
        assertFalse(mockTagEquals.equals(comp1, comp2))

        ApiTestValueBuildTool.prepareTag2("comp1 extra", "str value").also {comp1.add(it)}
        assertFalse(mockTagEquals.equals(comp1, comp2))

        ApiTestValueBuildTool.prepareTag2("int tag", 555).also {comp1.add(it)}
        assertFalse(mockTagEquals.equals(comp1, comp2))
    }

    @Test
    fun flatListTagTest() {
        val list1 = prepareSampleFlatListTag("list1")
        val list2 = prepareSampleFlatListTag("list1")

        assertTrue(mockTagEquals.equals(list1, list2)) // should output nothing

        list1.add(ApiTestValueBuildTool.prepareTag(byteArrays.first()) as Tag<ByteArray>)
        assertFalse(mockTagEquals.equals(list1, list2))
        list1.value.removeLast()

        list1.value[10] = ApiTestValueBuildTool.prepareTag(byteArrays.first()) as Tag<ByteArray>
        assertFalse(mockTagEquals.equals(list1, list2))
    }

    @Test
    fun listTagContainsCompoundTagTest() {
        val list1 = prepareListWithCompound("list1")
        val list2 = prepareListWithCompound("list1")
        assertTrue(mockTagEquals.equals(list1, list2)) // should output nothing

        list1.add(prepareFlatCompound(null))
        assertFalse(mockTagEquals.equals(list1, list2))
        list1.value.removeLast()

        list1.value[7].value["string tag"] = ApiTestValueBuildTool.prepareTag2("string tag2", "lololololol")
        assertFalse(mockTagEquals.equals(list1, list2))
    }

    private fun prepareFlatCompound(name:String?):CompoundTag {
        val comp = CompoundTag(name)
        ApiTestValueBuildTool.prepareTag2("int tag", 1).also {comp.add(it)}
        ApiTestValueBuildTool.prepareTag2<Long>("long tag", 10).also {comp.add(it)}
        ApiTestValueBuildTool.prepareTag2("double tag", 100.0).also {comp.add(it)}
        ApiTestValueBuildTool.prepareTag2("string tag", "string value").also {comp.add(it)}
        ApiTestValueBuildTool.prepareTag2("byte array tag", ByteArray(200)).also {comp.add(it)}
        return comp
    }

    private fun prepareSampleFlatListTag(name:String?):ListTag<ByteArray> {
        val bytesCopy = byteArrays.map { it.copyOf() }
        return ApiTestValueBuildTool.iterableToListTag(name, IdTagByteArray, bytesCopy, ApiTestValueBuildTool::prepareTag) as ListTag<ByteArray>
    }

    private fun prepareListWithCompound(name:String?):ListTag<AnyCompound> {
        val compList = ArrayList<CompoundTag>()
        repeat(15) {compList.add(prepareFlatCompound(null))}
        return ListTag<AnyCompound>(IdTagCompound, "list with compound").also {
            it.value.addAll(compList)
        }
    }

    private val byteArrays = Array(20) {
        Random.Default.nextBytes(100)
    }

}