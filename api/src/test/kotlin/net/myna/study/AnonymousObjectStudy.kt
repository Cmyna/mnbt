package net.myna.study

import org.junit.jupiter.api.Test

class AnonymousObjectStudy {

    interface A {
        val s:String
    }
    interface B {
        val b:Boolean
    }

    @Test
    fun test() {
        val obj = object: A, B {
            override val b: Boolean = true
            override val s: String = "string"
        }
        val clazz = obj::class.java
        println(obj::class.java)
        val builder = StringBuilder("interfaces: ")
        obj::class.java.interfaces.onEach {
            builder.append("$it, ")
        }
        println(builder.toString())

        println("interfaces' methods:")
        obj::class.java.interfaces.onEach {
            it.methods.onEach {
                println(it)
            }
        }
    }
}