package net.myna.mnbt.snbt

import net.myna.mnbt.IdTagCompound
import net.myna.mnbt.IdTagFloat
import net.myna.mnbt.IdTagString
import net.myna.mnbt.tag.*
import net.myna.mnbt.utils.ApiTestValueBuildTool
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class ComplicateNbtToString {

    @Test
    /**
     * with only flat tags in compound tag
     */
    fun compoundTagToString() {
        // empty compound Tag:
        val emptyCompoundTag = CompoundTag("EmptyCompound").also { println(it) }

        // simple compound tag
        val comp1 = CompoundTag("compound tag1")
        comp1.add(ApiTestValueBuildTool.prepareTag2("int tag", 12345))
        comp1.add(ApiTestValueBuildTool.prepareTag2("String", "string value"))
        comp1.add(ApiTestValueBuildTool.prepareTag2("double", Double.NaN))
        comp1.add(ApiTestValueBuildTool.prepareTag2("float", -51.3f))
        println(comp1)

        // compound tag with null name：
        val comp2 = CompoundTag(null)
        comp1.value.onEach { comp2.add(it.value) }
        println(comp2)
    }

    @Test
    fun nestCompoundTag() {
        // a better format from toString methods
        val comp3 = CompoundTag("comp3")
        comp3.add(ApiTestValueBuildTool.prepareTag2("byte tag", 0.toByte()))
        val comp1 = CompoundTag("compound tag1")
        comp1.add(ApiTestValueBuildTool.prepareTag2("int tag", 12345))
        comp1.add(ApiTestValueBuildTool.prepareTag2("String", "string value"))
        comp1.add(comp3)
        val comp2 = CompoundTag("root")
        comp2.add(comp1)
        comp2.add(ApiTestValueBuildTool.prepareTag2("long tag", -1827364590))
        comp2.add(ApiTestValueBuildTool.prepareTag2("byte array tag", byteArrayOf(0,51,-35,-127,66)))
        println(comp2.toString())

        val comp4 = CompoundTag(null)
        comp2.value.onEach { comp4.add(it.value) }
        println(comp4.toString())
    }

    @Test
    fun listWithCompound() {
        val comp1 = CompoundTag(null)
        comp1.add(ApiTestValueBuildTool.prepareTag2("int tag", 12345))
        comp1.add(ApiTestValueBuildTool.prepareTag2("String", "string value"))
        val listTag = ListTag<CompoundTag>(IdTagCompound, "list tag")
        repeat(5) {listTag.add(comp1)}
        val comp2 = CompoundTag("root")
        comp2.add(listTag)
        comp2.add(ApiTestValueBuildTool.prepareTag2("long tag", -1827364590))
        comp2.add(ApiTestValueBuildTool.prepareTag2("byte array tag", byteArrayOf(0,51,-35,-127,66)))
        println(comp2.toString())
    }

    @Test
    fun hideLongJsonArray() {
        // array tag
        val intArrayTag = IntArrayTag("int array tag",
            IntArray(200) { Random.nextInt() }
        )
        println(intArrayTag)
        val floatTagsList = ListTag<FloatTag>(IdTagFloat, "floatList")
        repeat(100) { floatTagsList.add(ApiTestValueBuildTool.prepareTag2(null, Random.nextFloat()) as FloatTag)}
        println(floatTagsList)

        val stringTagsList = ListTag<StringTag>(IdTagString, "stringList")
        repeat(100) { stringTagsList.add(ApiTestValueBuildTool.prepareTag2(null, "some string value") as StringTag)}
        println(stringTagsList)
    }

    @Test
    fun handleCompoundCircularReference() {
        val comp1 = CompoundTag("comp1")
        val comp2 = CompoundTag("comp2")
        comp1.add(comp2)
        comp2.add(comp1)
        println(comp1)
    }
}