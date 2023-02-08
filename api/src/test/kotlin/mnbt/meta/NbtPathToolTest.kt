package mnbt.meta

import com.myna.mnbt.*
import com.myna.mnbt.converter.meta.NbtPathTool
import com.myna.mnbt.tag.CompoundTag
import com.myna.mnbt.tag.ListTag
import mnbt.utils.ApiTestValueBuildTool
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NbtPathToolTest {

    @Test
    fun locateTagInNestCompounds() {
        val path1 = "mnbt://tag1/tag2/tag3/tag4"
        val comps1 = buildCompoundTags(path1)
        val intTag1 = ApiTestValueBuildTool.prepareTag2("int tag", 5)
        comps1.last().add(intTag1)
        val stringTag1 = ApiTestValueBuildTool.prepareTag2("string tag", "string")
        comps1[2].add(stringTag1)

        assertEquals(intTag1, NbtPathTool.goto(comps1.first(), "./tag2/tag3/tag4/int tag", IdTagInt))
        assertEquals(stringTag1, NbtPathTool.goto(comps1.first(), "./tag2/tag3/string tag", IdTagString))
    }

    @Test
    fun locateTagInNestList() {
        val path1 = "mnbt://root list/#0/#15/#10"
        val listTags = buildListTags(path1)
        val intListTag = ListTag<Int>(IdTagInt, null)
        listTags.last().add(intListTag as Tag<MutableList<*>>)
        assertEquals(intListTag, NbtPathTool.goto(listTags.first(), ".//#0/#15/#10/#0", IdTagList))
        repeat(7) {intListTag.add(ApiTestValueBuildTool.prepareTag2(null, 999) as Tag<Int>)}
        val intTag = ApiTestValueBuildTool.prepareTag2(null, 15) as Tag<Int>
        intListTag.add(intTag)
        repeat(3) {intListTag.add(ApiTestValueBuildTool.prepareTag2(null, 999) as Tag<Int>)}
        assertEquals(intTag, NbtPathTool.goto(listTags.first(), "./#0/#15/#10/#0/#7", IdTagInt))

    }

    private fun buildListTags(absPath: String):List<ListTag<MutableList<*>>> {
        val seq = NbtPathTool.toAccessSequence(absPath)
        val tags:List<ListTag<MutableList<*>>> = seq.map {
            if (it.first() == '#') ListTag<MutableList<*>>(IdTagList, null)
            else ListTag(IdTagList, it)
        }.toList()
        val indices = seq.drop(1).map {
            it.substring(1, it.length).toInt()
        }.toList()
        for (i in indices.indices) {
            val parent = tags[i]
            val child = tags[i+1]
            val childIndex = indices[i]
            for (j in 0 until childIndex) {
                parent.add(ListTag<MutableList<*>>(IdTagEnd, null) as Tag<MutableList<*>>)
            }
            parent.add(child as Tag<MutableList<*>>)
        }
        return tags
    }

    private fun buildCompoundTags(absPath:String):List<CompoundTag> {
        val seq = NbtPathTool.toAccessSequence(absPath)
        val comps = seq.map { CompoundTag(it) }.toList()
        comps.fold<CompoundTag, CompoundTag?>(null) { parent,child ->
            parent?.add(child)
            child
        }
        return comps
    }
}