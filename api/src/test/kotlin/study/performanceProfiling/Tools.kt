package study.performanceProfiling

object Tools {

    /**
     * time profiling ensures that all object from fn() is created on heap (by holding these objects so that escape analysis will ignore them)
     * also there is an duplicate operation to measure set object to array time cost and reduce this bias
     */
    fun timeProfiling(times:Int, fn:()->Any):Pair<Long, Collection<Any>> {
        val arr = arrayOfNulls<Any>(times)
        var pointer = 0
        var time:Long = 0
        time -= System.nanoTime()
        repeat(times) {
            val res = fn()
            arr[pointer] = res
            pointer += 1
        }
        time += System.nanoTime()

        val arr2 = arrayOfNulls<Any>(times)
        pointer = 0
        var bias:Long = -System.nanoTime()
        repeat(times) {
            arr[pointer] = arr[pointer]
            pointer += 1
        }
        bias += System.nanoTime()
        //println("static operation cost: ${staticCost/1000.0f/1000}ms")
        val collection = setOf(arr, arr2)
        return Pair(time-bias, collection)
    }

    fun toMs(ns:Long):Float {
        return ns/1000.0f/1000
    }
}