package net.myna.study

import com.google.common.reflect.TypeToken
import org.junit.jupiter.api.Test
import java.lang.reflect.Field


class ReflectionStudy {


    @Test
    fun interfaceAbstractClassTest() {
        abstract class testAbs {}
        // test interface and abstract class is sub class of object or not
        val tk1 = TypeToken.of(Map::class.java)
        val tk2 = TypeToken.of(testAbs::class.java)

        println("Map interface is assignable to object: ${tk1.isSubtypeOf(Any::class.java)}")
        println("testAbs is assignable to object: ${tk2.isSubtypeOf(Any::class.java)}")
    }

    @Test
    fun constructorTest() {
        class A
        class B(val i:Int)
        // test class with empty constructor
        val c = A::class.java.constructors[0]
        val a = c.newInstance()
        println("create instance from A: $a")

        val c2 = B::class.java.constructors[0]
        //val b = c2.newInstance() // this code cause runtime exception: wrong number of arguments
        c2.newInstance(1) // pass matched value, it works
        // try pass null value into constructor
        //val b = c2.newInstance(null) // still not work, just IllegalArgumentException
        //c2.newInstance(arrayOfNulls<Any>(1)) // not work, argument type mismatch

        //val c3 = B::class.java.getDeclaredConstructor() // NoSuchMethodException...
        val c3 = B::class.java.getDeclaredConstructor(Int::class.java) // it works
        //c3.newInstance(null) // still just IllegalArgumentException
        //c3.newInstance(arrayOfNulls<Any>(1)) // still IllegalArgumentException and argument type mismatch
        val nullArgs:Array<Any?>? = null
        //c3.newInstance(nullArgs) // still IllegalArgumentException...

        // test private constructor accessiable or not
        class C {
            private constructor() {}
        }
        val c4 = C::class.java.getDeclaredConstructor()
        // c4.newInstance() // directly call it will cause IllegalAccessException
        println(c4.isAccessible) // print accessible flag (but it is deprecated)
        println(c4.canAccess(null)) // we test constructor accessible by this method (with null varg)

        // works after set accessible
        c4.trySetAccessible()
        c4.newInstance()
    }

    @Test
    fun inheritancesClassTest() {
        open class A(val i:Int)
        class B(val s:String, private val i2:Int, i:Int):A(i) {
        }

        print("B's fields: ")
        getAllFields(object:TypeToken<B>() {}.rawType).onEach {
            print("${it.name}:${it.type.name}  ")
        }
//        B::class.java.declaredFields.onEach {
//            print("${it.name}:${it.type.name}  ")
//        }
        println()
    }

    fun getAllFields(type:Class<*>?):List<Field> {
        if (type==null) return mutableListOf()
        val fields = mutableListOf<Field>()
        fields.addAll(type.declaredFields)
        getAllFields(type.superclass)?.also {fields.addAll(it)}
        return fields
    }
}