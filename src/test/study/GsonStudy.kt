package study

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

open class S_A(private val x:Boolean,private val y:Int, val z:String)
data class S_B( val bx:Boolean, val by:Int, val bz:String)
class S_C(private val cx:Long, x:Boolean, y:Int, z:String):S_A(x,y,z)

class S_D(val m1:Long) {
    val innDc: InnDC = InnDC("value(String) in innDC")
    class InnDC(val str:String)
}

class GsonStudy {

    @Test
    fun simplePojoTest() {
        val str = "{cx:555,x:true,y:1,z:somestr}"
        val gson = Gson()
        val res = gson.fromJson<S_C>(str, object:TypeToken<S_C>() {}.type)
        println("${res.z}")
    }

    class InnerB(val i2:Int, val someStr:String, val c:Boolean)

    @Test
    fun innerClassStudy() {
        class InnerA(val i:Int, val someStr:String)
        val a = InnerA(55555, "some string ababababa")
        val gson = Gson()
        var jsonStr = gson.toJson(a, object:TypeToken<InnerA>() {}.type)
        println(jsonStr) // result is null, so gson's strategy is ignore anonymous class or local class

        val tk = object:TypeToken<InnerA>() {}

        // what if some class with member var is inner class?
        val sd = S_D(51354)
        jsonStr = gson.toJson(sd, object:TypeToken<S_D>() {}.type) // so it is serialized normally
        println(jsonStr) // so in normal, gson serialize inner class, but check the code gson since can set not serialize/deserialize inner class

        val innDc = S_D.InnDC("what about this?")
        jsonStr = gson.toJson(innDc, object:TypeToken<S_D.InnDC>() {}.type)
        println(jsonStr) // work

        val b = InnerB(984518, "sdfsdfedcc  ", false)
        jsonStr = gson.toJson(b, object:TypeToken<InnerB>() {}.type)
        println(jsonStr) // work
    }



    @Test
    fun mapTypeStudy() {
        // test if a sub-class from map and no empty construction
        class MyMap(val a:Int):LinkedHashMap<String, Int>() {}

        class MyMap2:LinkedHashMap<String,Int>() {}



        val mapInst = MyMap(0)
        mapInst.put("value", 5050)

        val gson = Gson()

        var jsonStr = gson.toJson(mapInst, object: TypeToken<Map<String, Int>>() {}.type)

        println(jsonStr)

        // so gson can not cast any sub-class not added in adapter
        val newInst = gson.fromJson<MyMap>(jsonStr, object: TypeToken<MyMap>() {}.type)
        val myMap2Inst = gson.fromJson<MyMap2>(jsonStr, object: TypeToken<MyMap2>() {}.type)
        assertTrue(newInst==null); assertTrue(myMap2Inst==null)
        println()

        // test a map with any type will happen what
        val map2 = HashMap<String, Any>()
        map2["int"] = 1
        map2["string"] = "some string value"
        jsonStr = gson.toJson(map2, object:TypeToken<Map<String, Any>>() {}.type)
        println(jsonStr)

        // if some object not in gson default type?
        jsonStr = gson.toJson(map2, object:TypeToken<Map<String, Any>>() {}.type)
        println(jsonStr) // custom class is just ignored
        println()

        // what if type token is Map<String, TagValue<Int>>?
        // result: it directly throws exception, seems gson can not handle abstract class with generic type automatically
        //jsonStr = gson.toJson(map2, object:TypeToken<Map<String, TagValue<Int>>>() {}.type)
        //println(jsonStr)


        //test a Map<String, Any> with different custom obj
        val map3 = HashMap<String, Any>()
        map3["A"] = S_A(false, 505, "A Object")
        map3["B"] = S_B(true, 1111, "B Object")

        jsonStr = gson.toJson(map3, object:TypeToken<Map<String, Any>>() {}.type)
        println(jsonStr)
        var resMap = gson.fromJson<Map<String, Any>>(jsonStr, object:TypeToken<Map<String, Any>>() {}.type)
        resMap.onEach {
            System.out.println("map key: ${it.key}, value type: ${it.value::class.java}")
        }
        println()

        val map4 = HashMap<String, S_A>()
        map4["A"] = S_A(false, 505, "A Object")
        jsonStr = gson.toJson(map4, object:TypeToken<Map<String, S_A>>() {}.type)
        println(jsonStr)
        resMap = gson.fromJson<Map<String, S_A>>(jsonStr, object:TypeToken<Map<String, S_A>>() {}.type)
        resMap.onEach {
            System.out.println("map key: ${it.key}, value type: ${it.value::class.java}")
        }
    }

    @Test
    fun subClassStudy() {
        val gson = Gson()

        val s_c = S_C(500,false, 1155, "S_C object")
        var jsonStr = gson.toJson(s_c,  object:TypeToken<S_C>() {}.type)
        println(jsonStr)
        jsonStr = gson.toJson(s_c,  object:TypeToken<S_A>() {}.type)
        println(jsonStr)
    }

    @Test
    fun cyclicRefStudy() {
        val list1 = ArrayList<Any>()
        val list2 = ArrayList<Any>()
        list1.add(list2)
        list2.add(list1)
        list1.add("a string")

        val gson = Gson()

        // if we directly use gson like this, stack overflow lol
//        var jsonStr = gson.toJson(list1, object:TypeToken<List<Any>>() {}.type)
//        println("cyclic reference list serialized result: $jsonStr")
    }

}