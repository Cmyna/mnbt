package net.myna.mnbt.snbt

import net.myna.mnbt.AnyTag
import net.myna.mnbt.IdTagList
import net.myna.mnbt.tag.AnyTagList
import net.myna.mnbt.tag.ListTag
import net.myna.mnbt.tag.ListTag.Companion.unknownElementId
import net.myna.mnbt.tag.PrimitiveTag
import net.myna.mnbt.utils.ApiTestValueBuildTool
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ListTagToString {

    @Test
    fun withFlatTags() {
        val intListTag = ApiTestValueBuildTool
            .valueSequenceToListTag<PrimitiveTag.IntTag>("intListTag", intArrayOf(0,65536,241111111,-515555,0).asSequence())
        assertEquals("""
            {"intListTag":[0,65536,241111111,-515555,0]}
        """.trimIndent(), intListTag.toString().also { println(it) }
        )
        val floatListTag = ApiTestValueBuildTool
            .valueSequenceToListTag<PrimitiveTag.FloatTag>("floatListTag", floatArrayOf(0.0f, 510.0f, -9951.843f, 7175.05f).asSequence())
        assertEquals("""
            {"floatListTag":[0.0,510.0,-9951.843,7175.05]}
        """.trimIndent(), floatListTag.toString().also { println(it) }
        )
        val doubleListTag = ApiTestValueBuildTool
            .valueSequenceToListTag<PrimitiveTag.DoubleTag>("doubleListTag", doubleArrayOf(0.0, 189.5, 223315.87).asSequence())
        assertEquals("""
            {"doubleListTag":[0.0,189.5,223315.87]}
        """.trimIndent(), doubleListTag.toString().also { println(it) }
        )
        val stringListTag = ApiTestValueBuildTool
            .valueSequenceToListTag<PrimitiveTag.StringTag>("stringListTag", arrayOf("str1", "str2", "", "some string\n\b").asSequence())
        assertEquals("""
            {"stringListTag":["str1","str2","","some string\n\b"]}
        """.trimIndent(), stringListTag.toString().also { println(it) }
        )

        // some exceptions
        // invalid float/double value:
        val floatTagListWithInvalidValue = ApiTestValueBuildTool
            .valueSequenceToListTag<PrimitiveTag.FloatTag>("floatListTag",
                floatArrayOf(Float.MAX_VALUE, Float.MIN_VALUE, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY).asSequence())
            .also { println(it) }
        assertTrue(
            Regex("""
                ^\{"floatListTag"\s*:\s*\[\s*.+,\s*.+,\s*NaN,\s*-Infinity,\s*Infinity]}$
            """.trimIndent()).containsMatchIn(floatTagListWithInvalidValue.toString())
        )
        val listWithInvalidDouble = ApiTestValueBuildTool
            .valueSequenceToListTag<PrimitiveTag.DoubleTag>("doubleListTag",
                doubleArrayOf(Double.NaN, Double.POSITIVE_INFINITY).asSequence())
            .also { println(it) }
        assertTrue(
            Regex("""
                ^\{"doubleListTag"\s*:\s*\[\s*NaN,\s*Infinity]}$
            """.trimIndent()).containsMatchIn(listWithInvalidDouble.toString())
        )
        // empty list
        val emptyListTag = ListTag<AnyTag>(unknownElementId, "empty list").also {println(it)}
        assertTrue(
            Regex("""
                ^\{"empty list"\s*:\s*\[]}$
            """.trimIndent()).containsMatchIn(emptyListTag.toString())
        )
    }

    @Test
    fun handleCircularReference() {
        val list1 = ListTag<AnyTag>(IdTagList, "list1")
        val list2 = ListTag<AnyTag>(IdTagList, "list2")
        list1.add(list2)
        list2.add(list1)
        println(list1)
    }
}