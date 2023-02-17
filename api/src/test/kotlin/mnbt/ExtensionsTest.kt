package mnbt

import com.myna.utils.Extensions.toBasic
import com.myna.utils.Extensions.toBytes
import com.myna.utils.Extensions.toInt
import com.myna.utils.Extensions.toShort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class ExtensionsTest {

    @Test
    fun testBytesConversion() {

        //test int<->byte[]
        repeat(1000) {
            val i = Random.nextInt()
            val bytes = i.toBytes()
            assertEquals(4, bytes.size)
            val i2 = bytes.toInt()
            assertEquals(i, i2)
        }
        assertEquals(Int.MAX_VALUE, Int.MAX_VALUE.toBytes().toInt())
        assertEquals(Int.MIN_VALUE, Int.MIN_VALUE.toBytes().toInt())
        assertEquals(0, 0.toBytes().toInt())
        assertNotEquals(0, 5.toBytes().toBasic<Int>(0,0))

        //test short<->byte[]
        repeat(1000) {
            val i = Random.nextInt().toShort()
            val bytes = i.toBytes()
            assertEquals(2, bytes.size)
            val i2 = bytes.toShort(0)
            assertEquals(i, i2)
        }

        //test Long<->byte[]
        repeat(1000) {
            Random.nextLong().let { Pair(it, it.toBytes()) }
                    .also { assertEquals(8, it.second.size) }
                    .let { Pair(it.first, it.second.toBasic<Long>(0, 0)) }
                    .also { assertEquals(it.first, it.second) }
        }

        repeat(1000) {
            Random.nextFloat().let { Pair(it, it.toBytes()) }
                    .also { assertEquals(4, it.second.size) }
                    .let { Pair(it.first, it.second.toBasic<Float>(0, 0.0f)) }
                    .also { assertEquals(it.first, it.second) }
        }
        assertEquals(Float.MAX_VALUE, Float.MAX_VALUE.toBytes().toBasic<Float>(0, 0.0f))
        assertEquals(Float.MIN_VALUE, Float.MIN_VALUE.toBytes().toBasic<Float>(0, 0.0f))

            repeat(1000) {
                Random.nextDouble().let { Pair(it, it.toBytes()) }
                        .also { assertEquals(8, it.second.size) }
                        .let { Pair(it.first, it.second.toBasic<Double>(0, 0.0)) }
                        .also { assertEquals(it.first, it.second, Double.MIN_VALUE) }

            }

    }
}