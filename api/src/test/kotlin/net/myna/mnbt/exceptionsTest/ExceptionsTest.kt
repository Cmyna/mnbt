package net.myna.mnbt.exceptionsTest


import net.myna.mnbt.IdTagCompound
import net.myna.mnbt.IdTagEnd
import net.myna.mnbt.IdTagList
import net.myna.mnbt.Tag
import net.myna.mnbt.codec.binary.BinaryCodecInstances
import net.myna.mnbt.codec.userDecodeIntent
import net.myna.mnbt.codec.userEncodeIntent
import net.myna.mnbt.converter.ExcluderConverter
import net.myna.mnbt.exceptions.CircularReferenceException
import net.myna.mnbt.exceptions.MaxNbtTreeDepthException
import net.myna.mnbt.reflect.MTypeToken
import net.myna.mnbt.tag.*
import net.myna.mnbt.utils.TestMnbt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

@Suppress("UNCHECKED_CAST")
class ExceptionsTest {

    /**
     * test circular reference appears on hierarchical TagValue structure.
     * in general, the proxy for TagValue serialization should throw CircularReferenceException
     */
    @Test
    fun testThrowsCircularRefException() {
        // test list circular reference
        val nbtList1 = ListTag("list1", ArrayList<Tag<out AnyTagList>>(), IdTagList)
        val nbtList2 = ListTag("list2", ArrayList<Tag<out AnyTagList>>(), IdTagList)

        nbtList1.add(nbtList2 as Tag<AnyTagList>)
        nbtList2.add(nbtList1 as Tag<AnyTagList>)

        assertThrows<CircularReferenceException> {
            val outputStream = ByteArrayOutputStream()
            TestMnbt.inst.refCodecProxy.encode(nbtList1, userEncodeIntent(outputStream))
        }.also {println("Exception throws: ${it.message}\n")}

        val nbtList3 = ListTag("list3",  ArrayList<Tag<out AnyTagList>>(), IdTagList)
        nbtList3.add(nbtList3 as Tag<AnyTagList>)
        assertThrows<CircularReferenceException> {
            val outputStream = ByteArrayOutputStream()
            TestMnbt.inst.refCodecProxy.encode(nbtList3, userEncodeIntent(outputStream))
        }.also {println("Exception throws: ${it.message}\n")}


        // test compound circular reference
        val compound1 = CompoundTag("comp1")
        val compound2 = CompoundTag("comp2")
        compound1.add(compound2)
        compound2.add(compound1)
        assertThrows<CircularReferenceException> {
            val outputStream = ByteArrayOutputStream()
            TestMnbt.inst.refCodecProxy.encode(compound1, userEncodeIntent(outputStream))
        }.also {println("Exception throws: ${it.message}\n")}

        // test exception throws in hierarchical codec
        assertThrows<CircularReferenceException> {
            val outputStream = ByteArrayOutputStream()
            TestMnbt.inst.refCompoundCodec.encode(compound1, userEncodeIntent(outputStream))
        }.also {println("Exception throws: ${it.message}\n")}

        val compound3 = CompoundTag("comp3")
        compound3.add(compound3)
        assertThrows<CircularReferenceException> {
            val outputStream = ByteArrayOutputStream()
            TestMnbt.inst.refCodecProxy.encode(compound3, userEncodeIntent(outputStream))
        }.also {println("Exception throws: ${it.message}\n")}

        //mixed circular reference
        val compound4 = CompoundTag("comp4")
        val nbtList4 = ListTag("list4", ArrayList<Tag<out AnyCompound>>(), IdTagCompound)
        nbtList4.add(compound4)
        compound4.add(nbtList4)
        assertThrows<CircularReferenceException> {
            val outputStream = ByteArrayOutputStream()
            TestMnbt.inst.refCodecProxy.encode(compound4, userEncodeIntent(outputStream))
        }.also {println("Exception throws: ${it.message}\n")}
        assertThrows<CircularReferenceException> {
            val outputStream = ByteArrayOutputStream()
            TestMnbt.inst.refCodecProxy.encode(nbtList4, userEncodeIntent(outputStream))
        }.also {println("Exception throws: ${it.message}\n")}
    }

    @Test
    /**
     * this function for testing maximum tagValue tree depth exception.
     * usually it will throws when depth more than or equals to 512
     */
    fun testThrowsMaximumDepthException() {
        val outputStream = ByteArrayOutputStream()
        // if tree depth is smaller than 512, nothing happens
        val list511 = recursiveListTree(511)
        var intent = userEncodeIntent(outputStream)
        TestMnbt.inst.refCodecProxy.encode(list511, intent)

        // tree depth is 512, still nothing happens
        intent = userEncodeIntent(outputStream)
        val list512 = encapsulateWithList(list511)
        TestMnbt.inst.refCodecProxy.encode(list512, intent)

        // if tree depth is 513, MaximumDepthException throws
        intent = userEncodeIntent(outputStream)
        val list513 = encapsulateWithList(list512)
        assertThrows<MaxNbtTreeDepthException> {
            TestMnbt.inst.refCodecProxy.encode(list513, intent)
        }.also {println("Exception throws: ${it.message}\n")}


        // test compound tree like above
        intent = userEncodeIntent(outputStream)
        val comp511 = recursiveCompoundTree(511)
        TestMnbt.inst.refCodecProxy.encode(comp511, intent)

        intent = userEncodeIntent(outputStream)
        val comp512 = encapsulateWithCompound(comp511)
        TestMnbt.inst.refCodecProxy.encode(comp512, intent)

        intent = userEncodeIntent(outputStream)
        val comp513 = encapsulateWithCompound(comp512)
        assertThrows<MaxNbtTreeDepthException> {
            intent = userEncodeIntent(outputStream)
            TestMnbt.inst.refCodecProxy.encode(comp513, intent)
        }.also {println("Exception throws: ${it.message}\n")}

        // test compound list mixed structure
        intent = userEncodeIntent(outputStream)
        val tree511 = recursiveListCompoundTree(511)
        TestMnbt.inst.refCodecProxy.encode(tree511, intent)
        intent = userEncodeIntent(outputStream)
        val tree512 = encapsulateWithCompound(tree511)
        TestMnbt.inst.refCodecProxy.encode(tree512, intent)
        intent = userEncodeIntent(outputStream)
        val tree513 = encapsulateWithCompound(tree512)
        assertThrows<MaxNbtTreeDepthException> {
            intent = userEncodeIntent(outputStream)
            TestMnbt.inst.refCodecProxy.encode(tree513, intent)
        }.also {println("Exception throws: ${it.message}\n")}
    }

    @Test
    fun anonymousObjToNullTagTest() {
        val outputStream = ByteArrayOutputStream()
        val intent = userEncodeIntent(outputStream)
        val nullTag = NullTag.inst
        BinaryCodecInstances.nullTagCodec.encode(nullTag, intent)
        val bits = outputStream.toByteArray()
        assertEquals(1, bits.size)
        assertEquals(IdTagEnd, bits[0])
        val inputStream = ByteArrayInputStream(bits)
        inputStream.mark(Int.MAX_VALUE)
        val desIntent = userDecodeIntent(inputStream)
        val desTag = BinaryCodecInstances.nullTagCodec.decode(desIntent).tag
        assertEquals(nullTag, desTag)
        assertEquals(inputStream.available(), 0)

        // test Codec proxy
        inputStream.reset()
        TestMnbt.inst.refCodecProxy.decode(desIntent).tag.also {
            assertEquals(nullTag, it)
            assertEquals(inputStream.available(), 0)
        }
        outputStream.reset()
        TestMnbt.inst.refCodecProxy.encode(nullTag, intent).also {
            val res = outputStream.toByteArray()
            assertEquals(1, res.size)
            assertEquals(IdTagEnd, res[0])
        }

        // test anonymous/local class conversion
        class Anonymous(val i:Int)
        val a = Anonymous(51515)
        val tk = object: MTypeToken<Anonymous>() {}
        ExcluderConverter.instance.createTag("some name", a, tk).also { assertEquals(nullTag, it)}
        TestMnbt.inst.refConverterProxy.createTag("some name", a, tk).also { assertEquals(nullTag, it) }

        val b = object:ArrayList<String>() {}
        val tk2 = object: MTypeToken<ArrayList<String>>() {}
        ExcluderConverter.instance.createTag("some name", b, tk2).also { assertEquals(nullTag, it)}
        TestMnbt.inst.refConverterProxy.createTag("some name", b, tk2).also { assertEquals(nullTag, it) }


    }

    private fun recursiveListTree(depth:Int): Tag<Any> {
        val nbtList = ListTag("list", ArrayList<Tag<out Any>>(), IdTagList)
        if (depth > 1) {
            val subList = recursiveListTree(depth-1)
            nbtList.add(subList)
        }
        return nbtList as Tag<Any>
    }

    private fun recursiveCompoundTree(depth: Int): Tag<Any> {
        val compound = CompoundTag("sub comp with depth: $depth")
        if (depth > 1) {
            val subComp = recursiveCompoundTree(depth-1)
            compound.add(subComp)
        }
        return compound as Tag<Any>
    }

    private fun recursiveListCompoundTree(depth:Int): Tag<Any> {
        val subTag = if (depth > 1)  recursiveListCompoundTree(depth-1) else null
        if (depth%2==0) {
            return encapsulateWithCompound(subTag)
        } else {
            return encapsulateWithList(subTag)
        }
    }

    private fun encapsulateWithCompound(subTagValue: Tag<Any>?): Tag<Any> {
        val compound = CompoundTag("comp")
        subTagValue?.let { compound.add(it) }
        return compound as Tag<Any>
    }

    private fun encapsulateWithList(subTag: Tag<Any>?): Tag<Any> {
        val nbtList = ListTag("list", ArrayList<Tag<out Any>>(), subTag?.id?:IdTagEnd)
        subTag?.let { nbtList.add(it) }
        return nbtList as Tag<Any>
    }
}