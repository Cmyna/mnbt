package study

import com.google.common.reflect.TypeToken
import org.junit.Test

class genericTypeStudy {

    @Test
    fun listTest() {
        val list = ArrayList<Any>()

        val list2 = list as MutableList<Int>

        val list3 = list as MutableList<String>

        list2.add(55)
        list3.add("str")

        println("[0] ${list[0] as Int}")
        println("[1] ${list[1]}")

        // code below will throw class cast exception
        //val a = list2[1]

        val specifyList:()->MutableList<Any> = {
            ArrayList<Int>() as MutableList<Any>
        }

        val list4 = specifyList()

        // still work, because actually it is just List<Object> in runtime
        list4.add("str2")

        val a = mutableListOf<Int>()::class.java
        val b = TypeToken.of(MutableList::class.java).rawType
        println(b.isAssignableFrom(a))

        // try cast list generic type, still work
        list as MutableList<String>
        list.add("test")
        list as MutableList<Double>
        list.add(5.0)
        list as MutableList<Any>
        list.add(Any::class.java)
        list.add("")
        list as MutableList<Nothing>

    }



}