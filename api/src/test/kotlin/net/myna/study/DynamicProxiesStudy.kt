package net.myna.study

import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.jvm.javaGetter

class DynamicProxiesStudy {

    interface TestA {
        val a:Int
    }

    interface TestB {
        val b:String
    }

    @Test
    fun proxyOverrideInterface() {
        val obj1 = object: TestA, TestB {
            override val a = 5
            override val b = "test str1"
        }
        println(obj1.a)
        println(obj1.b)


        val obj2 = Proxy.newProxyInstance(obj1::class.java.classLoader, arrayOf(TestA::class.java, TestB::class.java)) {
            proxy:Any, method:Method, args:Array<Any>? ->
            when(method) {
                TestA::a.javaGetter -> {
                    return@newProxyInstance 50
                }
                else -> return@newProxyInstance method.invoke(obj1, *args.orEmpty())
            }
            return@newProxyInstance null
        }
        obj2 as TestA
        obj2 as TestB
        obj2.a
        println(obj2.a)
        println(obj2.b)

    }
}