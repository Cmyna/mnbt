package study

import org.junit.jupiter.api.Test

class interfaceDelegateStudy {

    interface A {
        fun getSelf():A = this
        fun getDelegate():A
    }
    interface B:A
    interface C:A

    @Test
    fun iDelegateTest() {
        val a = object:A{
            override fun getDelegate(): A = this
        }
        val b = object:A by a,B {
            override fun getSelf():A = this
            override fun getDelegate(): A = a
        }
        val c = object:A by b,C {
            override fun getDelegate():A = b
            override fun getSelf():A = this
        }

        println("c is B: ${(c.getDelegate()) is B}")
    }

}