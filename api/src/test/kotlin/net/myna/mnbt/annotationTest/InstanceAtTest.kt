package net.myna.mnbt.annotationTest

import net.myna.mnbt.Mnbt
import net.myna.mnbt.annotations.InstanceAs
import net.myna.mnbt.annotations.LocateAt
import net.myna.mnbt.exceptions.InvalidInstanceAsClassException
import net.myna.mnbt.reflect.MTypeToken
import net.myna.mnbt.tag.CompoundTag
import net.myna.mnbt.tag.IntTag
import net.myna.mnbt.tag.StringTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

    @Test
    fun exception() {
        // testing when field is not declared correct instance
        val testClassTag = CompoundTag("root")
        val member1 = CompoundTag("member1").also { testClassTag.add(it) }
        member1.add(IntTag("int tag", 5387))
        member1.add(StringTag("string tag", "some string value ab789"))

        val mnbt = Mnbt()
        assertThrows<InvalidInstanceAsClassException>() {
            mnbt.fromTag(testClassTag, object: MTypeToken<TestClass2>() {})
        }.also { it.printStackTrace() }

        // test annotation declared class is an interface
        assertThrows<InvalidInstanceAsClassException>() {
            mnbt.fromTag(testClassTag, object: MTypeToken<TestClass3>() {})
        }.also { it.printStackTrace() }
    }

    private interface TestInterface {
        val i:Int
        val j:String
    }

    private interface TestInterface2: TestInterface

    private class TestClass(
        @InstanceAs(TestInterfaceImpl::class)
        val member1:TestInterface
    )

    private class TestClass2(
        @InstanceAs(Int::class)
        val member1:TestInterface
    )

    private class TestClass3(
        @InstanceAs(TestInterface2::class)
        val member1:TestInterface
    )

    private class TestInterfaceImpl(
        @LocateAt("int tag")
        override val i: Int,
        @LocateAt("string tag")
        override val j: String
    ) : TestInterface
}