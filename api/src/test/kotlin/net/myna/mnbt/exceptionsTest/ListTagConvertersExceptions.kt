package net.myna.mnbt.exceptionsTest

import net.myna.mnbt.reflect.MTypeToken
import net.myna.mnbt.tag.ListTag
import net.myna.mnbt.utils.TestMnbt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ListTagConvertersExceptions {

    @Test
    fun emptyListException() {
        // problem when convert empty list to list tag
        // if empty list, then list tag element id will be specified as unknown
        val emptyList = ArrayList<Short>()
        val res = TestMnbt.inst.toTag("empty list", emptyList, object:MTypeToken<List<Short>>() {})
        assertTrue(res is ListTag<*>)
        assertEquals((res as ListTag<*>).elementId, ListTag.unknownElementId)
    }
}