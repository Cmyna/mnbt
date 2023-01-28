package study

import com.google.common.reflect.TypeToken
import org.junit.jupiter.api.Test

class GuavaTypeTokenStudy {
    // for study Guava TypeToken use

    @Test
    fun resolveFunStudy() {

        // can resolve super class type parameters
        val tk1 = object:TypeToken<LinkedHashMap<String, Int>>() {}
        println("LinkedHashMap<String,Int> type: ${tk1.type}\n")
        val Ktype = tk1.resolveType(Map::class.java.typeParameters[0])
        val Vtype = tk1.resolveType(Map::class.java.typeParameters[1])
        println("LinkedHashMap<String,Int> type token resolve Map.class result: ")
        println("\t key type: ${Ktype.type}")
        println("\t value type: ${Vtype.type}")

    }

    @Test
    fun superTypeStudy() {
        val tk1 = object:TypeToken<LinkedHashMap<String, Int>>() {}
        tk1.isSubtypeOf(object:TypeToken<Map<String,Int>>(){}).also { println("LinkedHashMap<String, Integer> is sub type of Map<String,Int>: $it")}
        tk1.isSubtypeOf(object:TypeToken<Map<Any,Any>>(){}).also { println("LinkedHashMap<String, Integer> is sub type of Map<Any,Abt>: $it")}
        tk1.isSubtypeOf(object:TypeToken<Map<String,Short>>(){}).also { println("LinkedHashMap<String, Integer> is sub type of Map<String,Short>: $it")}
        tk1.isSubtypeOf(object:TypeToken<Map<Int,Int>>(){}).also { println("LinkedHashMap<String, Integer> is sub type of Map<Int, Int>: $it")}
    }

    @Test
    fun getTypeFromInstanceStudy() {
        //test get type from a runtime instance

        val map = HashMap<Int,Map<String, Short>>()
        val tk = TypeToken.of(map::class.java)
        println("hash map type: ${tk.type}")

        // try resolve instance generic type
        // result: only get generic declaration
        val ktk = tk.resolveType(Map::class.java.typeParameters[0])
        val vtk = tk.resolveType(Map::class.java.typeParameters[1])
        println("map key type resolved: ${ktk.type}")
        println("map value type resolved: ${vtk.type}")
    }

    @Test
    fun typeTokenCastStudy() {
        val tk = TypeToken.of(Map::class.java)

        // what if cast to TypeToken<Any>?
        println("type token1 type: ${tk.type}")
        val tkc = tk as TypeToken<Any>
        println("\ttype token1 casted to TypeToken<Any> type: ${tkc.type}")

        // result, cast not affect TypeToken object info
    }

    @Test
    fun genericTypeDeclarationStudy() {
        // test if declared type variable is not a actual class
        // so it can not construct a typetoken for a type variable
//        val tk = getTkWithGeneric<Int>() // it should still a type variable
//        println("typetoken: $tk")
//        println("typetoken raw type: ${tk.rawType}")
    }

    fun <V> getTkWithGeneric():TypeToken<V> {return object:TypeToken<V>() {}}


}