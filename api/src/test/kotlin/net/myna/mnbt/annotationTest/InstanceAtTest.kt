package net.myna.mnbt.annotationTest

import net.myna.mnbt.Mnbt
import net.myna.mnbt.annotations.InstanceAs
import net.myna.mnbt.annotations.LocateAt
import net.myna.mnbt.reflect.MTypeToken
import net.myna.mnbt.tag.CompoundTag
import net.myna.mnbt.tag.IntTag
import net.myna.mnbt.tag.StringTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InstanceAtTest {

    @Test
    fun test() {
        // create target tag
        val testClassTag = CompoundTag("root")
        val member1 = CompoundTag("member1").also { testClassTag.add(it) }
        member1.add(IntTag("int tag", 5387))
        member1.add(StringTag("string tag", "some string value ab789"))

        val mnbt = Mnbt()
        val obj = mnbt.fromTag(testClassTag, object:MTypeToken<TestClass>() {})!!.second
        assertTrue(obj.member1 is TestInterfaceImpl)
        assertEquals(5387, obj.member1.i)
        assertEquals("some string value ab789", obj.member1.j)
    }

    private interface TestInterface {
        val i:Int
        val j:String
    }

    private class TestClass(
        @InstanceAs(TestInterfaceImpl::class)
        val member1:TestInterface
    )

    private class TestInterfaceImpl(
        @LocateAt("int tag")
        override val i: Int,
        @LocateAt("string tag")
        override val j: String
    ) : TestInterface
}