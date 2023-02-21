package net.myna.mnbt.snbt

import net.myna.mnbt.tag.ArrayTag
import net.myna.mnbt.tag.PrimitiveTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlatTagToString {

    @Test
    fun primitiveTagToString() {
        val intTag = PrimitiveTag.IntTag("int tag", 50)
        val byteTag = PrimitiveTag.ByteTag("byte tag", -127)
        val shortTag = PrimitiveTag.ShortTag("short tag", 3000)
        val longTag = PrimitiveTag.LongTag("long tag", 881818181818)
        val floatTag = PrimitiveTag.FloatTag("float tag", -5.0f)
        val doubleTag = PrimitiveTag.DoubleTag("double tag", 12345.0)
        val stringTag = PrimitiveTag.StringTag("string tag", "some string value")

        assertEquals("{\"int tag\":50}", intTag.toString())
        println(intTag.toString())
        assertEquals("{\"byte tag\":-127}", byteTag.toString())
        println(byteTag.toString())
        assertEquals("{\"short tag\":3000}", shortTag.toString())
        println(shortTag.toString())
        assertEquals("{\"long tag\":881818181818}", longTag.toString())
        println(longTag.toString())
        assertEquals("{\"float tag\":-5.0}", floatTag.toString())
        println(floatTag.toString())
        assertEquals("{\"double tag\":12345.0}", doubleTag.toString())
        println(doubleTag.toString())
        assertEquals("{\"string tag\":\"some string value\"}", stringTag.toString())
        println(stringTag.toString())

        // test escape
        val stringTag2 = PrimitiveTag.StringTag("string tag\\\n\t", "some string value\b\n\r\"/@特殊字符[]{}()<>")
        println(stringTag2.toString())

        val nullNameTag = PrimitiveTag.IntTag(null, 5150)
        println(nullNameTag.toString())
    }

    @Test
    fun arrayTagToString() {
        val byteArrayTag = ArrayTag.ByteArrayTag("byte array tag", byteArrayOf(5,12,30,8,-127,127))
        assertEquals(
            """
                {"byte array tag":[5,12,30,8,-127,127]}
            """.trimIndent(), byteArrayTag.toString().also{println(it)}
        )
        val intArrayTag = ArrayTag.IntArrayTag("int array tag", intArrayOf(0,65536,241111111,-515555,0))
        assertEquals(
            """
                {"int array tag":[0,65536,241111111,-515555,0]}
            """.trimIndent(), intArrayTag.toString().also{println(it)}
        )
        val longArrayTag = ArrayTag.LongArrayTag("long array tag", longArrayOf(0,8177050139, -9999999))
        assertEquals(
            """
                {"long array tag":[0,8177050139,-9999999]}
            """.trimIndent(), longArrayTag.toString().also{println(it)}
        )

        // empty array tag
        val emptyArrTag = ArrayTag.IntArrayTag("emptyTag", intArrayOf())
        assertEquals(
            """
                {"emptyTag":[]}
            """.trimIndent(), emptyArrTag.toString().also{println(it)}
        )
        // single value array tag
        val singleValueArrayTag = ArrayTag.LongArrayTag("singleValueTag", longArrayOf(818199))
        assertEquals(
            """
                {"singleValueTag":[818199]}
            """.trimIndent(), singleValueArrayTag.toString().also{println(it)}
        )
    }
}