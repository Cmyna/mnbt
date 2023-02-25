package net.myna.mnbt


import net.myna.mnbt.utils.NbtPathTool
import net.myna.mnbt.tag.AnyCompound
import net.myna.mnbt.tag.CompoundTag
import net.myna.mnbt.tag.ListTag
import net.myna.mnbt.utils.ApiTestValueBuildTool
import net.myna.mnbt.utils.MockTagEquals
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

        list1.add(prepareFlatCompound(null) as Tag<AnyCompound>)
        assertFalse(mockTagEquals.equals(list1, list2))
        list1.value.removeLast()

        list1.value[7].value["string tag"] = ApiTestValueBuildTool.prepareTag2("string tag2", "lololololol")
        assertFalse(mockTagEquals.equals(list1, list2))
    }

    @Test
    fun equalsOrContainsTest() {
        val comp1 = prepareFlatCompound("comp1")
        val comp2 = prepareFlatCompound("comp1")

        comp1.add(ApiTestValueBuildTool.prepareTag2("comp1 extra string tag", "extra value"))
        assertFalse(mockTagEquals.equals(comp1, comp2))
        println("------------------------------------------------")
        assertTrue(mockTagEquals.equalsOrContains(comp1, comp2))
        assertFalse(mockTagEquals.equalsOrContains(comp2, comp1))
        println("------------------------------------------------")


        val list1 = prepareSampleFlatListTag("list1")
        val list2 = prepareSampleFlatListTag("list1")

        list1.add(ApiTestValueBuildTool.prepareTag2(null, Random.nextBytes(50)) as Tag<ByteArray>)
        assertFalse(mockTagEquals.equals(list1, list2))
        assertTrue(mockTagEquals.equalsOrContains(list1, list2))
        assertFalse(mockTagEquals.equalsOrContains(list2, list1))
    }

    /**
     * test structure equals/containsOrEquals
     */
    @Test
    fun structureEqualsTest() {
        val root1 = CompoundTag("root")
        buildCompoundsByPath(root1, "./comp1/comp2/comp3/comp4")
        buildCompoundsByPath(root1, "./comp1/comp2/comp5/comp6")
        buildCompoundsByPath(root1, "./comp7")

        val root2 = CompoundTag("root")
        buildCompoundsByPath(root2, "./comp1/comp2/comp3/comp4")
        buildCompoundsByPath(root2, "./comp1/comp2/comp5/comp6")
        buildCompoundsByPath(root2, "./comp7")

        assertTrue(mockTagEquals.equals(root1, root2))
        (NbtPathTool.findTag(root1, "./comp1") as CompoundTag).add(ApiTestValueBuildTool.prepareTag2("int tag", 55))
        assertFalse(mockTagEquals.equals(root1, root2))
        println("assert: key not exists tags, so structure not equals")
        assertFalse(mockTagEquals.structureEquals(root1, root2)) // key not exists tags, so structure not equals

        // same tag name but different type, structure not equals
        println("assert: same tag name but different type, structure not equals")
        (NbtPathTool.findTag(root2, "./comp1") as CompoundTag).add(ApiTestValueBuildTool.prepareTag2("int tag", 55.toShort()))
        assertFalse(mockTagEquals.structureEquals(root1, root2))

        // same tag name and tag type, but value not equals, cause structure equals
        println("assert: same tag name and tag type, but value not equals, cause structure equals")
        (NbtPathTool.findTag(root2, "./comp1") as CompoundTag).add(ApiTestValueBuildTool.prepareTag2("int tag", 1288))
        assertFalse(mockTagEquals.equals(root1, root2))
        assertTrue(mockTagEquals.structureEquals(root1, root2))

        // list tag should have same elements num, same elements type, elements also structure equals, then it is structure equals
        val listEntry1 = (NbtPathTool.findTag(root1, "./comp1/comp2/comp5") as CompoundTag)
        val listEntry2 = (NbtPathTool.findTag(root2, "./comp1/comp2/comp5") as CompoundTag)
        val listName1 = "list tag1";
        val listTag1 = ListTag<Tag<Int>>(IdTagInt, listName1).also { listTag->
            repeat(50) {ApiTestValueBuildTool.prepareTag2(null, 0).also {listTag.add(it as Tag<Int>)}}
        }
        val listTag2 = ListTag<CompoundTag>(IdTagCompound, listName1).also { listTag ->
            CompoundTag(null).also { buildCompoundsByPath(it, "./comp8/comp9"); listTag.add(it) }
            CompoundTag(null).also { listTag.add(it); it.add(ApiTestValueBuildTool.prepareTag2("long tag", 128.toLong())) }
            CompoundTag(null).also { buildCompoundsByPath(it, "./comp10"); listTag.add(it) }
        }
        val listTag3 = ListTag<CompoundTag>(IdTagCompound, listName1).also { listTag ->
            CompoundTag(null).also { buildCompoundsByPath(it, "./comp8/comp9"); listTag.add(it) }
            CompoundTag(null).also { listTag.add(it); it.add(ApiTestValueBuildTool.prepareTag2("long tag", 128.toLong())) }
            CompoundTag(null).also { buildCompoundsByPath(it, "./comp10"); listTag.add(it) }
            CompoundTag(null).also { listTag.add(it) } // no equals number
        }
        val listTag4 = ListTag<CompoundTag>(IdTagCompound, listName1).also { listTag ->
            CompoundTag(null).also { buildCompoundsByPath(it, "./comp8/comp9/comp11"); listTag.add(it) } // element not structural equals
            CompoundTag(null).also { listTag.add(it); it.add(ApiTestValueBuildTool.prepareTag2("long tag", 128.toLong())) }
            CompoundTag(null).also { buildCompoundsByPath(it, "./comp10"); listTag.add(it) }
        }
        val listTag5 = ListTag<CompoundTag>(IdTagCompound, listName1).also { listTag ->
            CompoundTag(null).also { buildCompoundsByPath(it, "./comp8/comp9"); listTag.add(it) }
            CompoundTag(null).also { listTag.add(it); it.add(ApiTestValueBuildTool.prepareTag2("long tag", 555555.toLong())) }
            CompoundTag(null).also { buildCompoundsByPath(it, "./comp10"); listTag.add(it) }
        }

        // list element type not equals, final not equals
        println("assert: list element type not equals, final not structural equals")
        listEntry1.add(listTag1); listEntry2.add(listTag2)
        assertFalse(mockTagEquals.structureEquals(root1, root2))

        // list size not equals, final not equals
        println("assert: list size not equals, final not structural equals")
        listEntry1.add(listTag3)
        assertFalse(mockTagEquals.structureEquals(root1, root2))

        // element not structural equals, final not equals
        println("assert: element not structural equals, final not structural equals")
        listEntry1.add(listTag4)
        assertFalse(mockTagEquals.structureEquals(root1, root2))

        // flag tag not value equals, but structure equals
        println("assert: flag tag not value equals, but structure equals")
        listEntry1.add(listTag5)
        assertTrue(mockTagEquals.structureEquals(root1, root2))
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

    private fun prepareSampleFlatListTag(name:String?):ListTag<Tag<ByteArray>> {
        val bytesCopy = byteArrays.map { it.copyOf() }
        return ApiTestValueBuildTool.iterableToListTag(name, IdTagByteArray, bytesCopy, ApiTestValueBuildTool::prepareTag) as ListTag<Tag<ByteArray>>
    }

    private fun prepareListWithCompound(name:String?):ListTag<Tag<AnyCompound>> {
        val compList = ArrayList<CompoundTag>()
        repeat(15) {compList.add(prepareFlatCompound(null))}
        return ListTag<Tag<AnyCompound>>(IdTagCompound, "list with compound").also {
            it.value.addAll(compList)
        }
    }

    private fun buildCompoundsByPath(root:CompoundTag, path:String) {
        val sequence = NbtPathTool.toAccessSequence(path)
        sequence.fold(root) { tag,segment->
            val child = NbtPathTool.findTag(tag, segment, IdTagCompound)?: CompoundTag(segment)
            tag.add(child)
            child as CompoundTag
        }
    }

    private val byteArrays = Array(20) {
        Random.Default.nextBytes(100)
    }

}