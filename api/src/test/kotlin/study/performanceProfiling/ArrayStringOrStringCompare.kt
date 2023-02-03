package study.performanceProfiling

import org.junit.jupiter.api.Test
import kotlin.random.Random

class ArrayStringOrStringCompare {

    data class ListString(val list:ArrayList<String>)

    private val chars = "abcdefghijklmnopqrstuvwxyz"

    private fun randomString():String {
        val builder = StringBuilder()
        repeat(10) {
            builder.append(chars[Random.nextInt(0, chars.length)])
        }
        return builder.toString()
    }

    @Test
    fun test() {
        val data = Array<String>(10000) {randomString()}

        val stringList = ArrayList<String>()
        Tools.timeProfiling(1) {
            data.onEach { stringList.add(it) }
        }.also { println("each element added to list cost: ${Tools.toMs(it.first)} ms") }

        Tools.timeProfiling(1) {
            var arr:Array<String?> = Array(0) {""}
            data.onEach {
                val newArr = arrayOfNulls<String>(arr.size+1)
                System.arraycopy(arr, 0, newArr, 0, arr.size)
                arr = newArr
                arr[arr.size-1] = it
            }
            arr
        }.also { println("each element copy to new array cost: ${Tools.toMs(it.first)} ms")}

        Tools.timeProfiling(1) {
            var str = ""
            data.onEach {
                str = "$str/$it"
            }
            str
        }.also { println("array string to long string cost: ${Tools.toMs(it.first)} ms")}

        Tools.timeProfiling(1) {
            var builder = java.lang.StringBuilder()
            data.onEach {
                builder.append(it)
                builder.append('/')
            }
            builder.toString()
        }.also { println("array string to long string by builder cost: ${Tools.toMs(it.first)} ms")}
    }
}