package net.myna.study

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class CommonStudy {

    @Test
    fun timestampInEpochSeconds() {
        println(Instant.now().epochSecond)
        println(Instant.now().epochSecond.toUInt())
    }

    @Test
    fun dataClassEqHashCodeTest() {
        data class A(val i:Int, val j:Int)

        assertEquals(A(5,7), A(5,7))
        assertEquals(A(5,7).hashCode(), A(5,7).hashCode())
    }
}