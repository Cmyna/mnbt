package study

import org.junit.jupiter.api.Test
import Tools.timeProfiling
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Proxy
import java.util.*
import kotlin.random.Random

class PerformanceProfiling {

    @Test
    fun byteArrayCopyPerformanceProfilingTest() {
        val bits = Random.Default.nextBytes(1024*1024*500) // 500mb
        val res = ByteArray(bits.size)
        var start = -System.currentTimeMillis()
        System.arraycopy(bits, 0, res, 0, bits.size)
        start += System.currentTimeMillis()
        println("time cost: $start ms")
        post(res)
        // time cost: 100-150ms copy 500mb bytes array on JDK17 (Eclipse Adoptium Temurin-17.0.3+7)
    }

    @Test
    fun tagDeserializationExtraCopySimulationTest() {
        // this test aims to simulate and analyse the extra copy performance in current Codec
        // which the tag deserialization api is return a ByteArray each call
        // so if there are some nested structure: t1->t2->t3, t3's return will be copied at least three times

        // in practices, nbt nested structure depth may from 0-7 in usual
        // eg. palette.name(String Tag) depth=7 in DataVersion1976 region file (chunk.Level.Sections.COMP.Name)

        // we decide to consider bottom tag as flat type. and regard it only copy once if we use something like buffer
        // then the redundant copy equals to depth-1
        // choose average depth as 3, data length as 128bytes*8192*3
        //      (once copy will have 8192*3 sub-copy to a big array)
        //      (three times copy will become like 8192*3->1024->32->1) (-> means merge), so it is merge 24*bytes -> merge 32 bytes -> merge 32 bytes
        // between each copy, will have a random time sleep to simulate other operation cost (1-100ms)
        // test performance difference between copy once and copy redundant times
        val toMs:(ns:Long)->Double = {ns->ns/1000.0/1000}

        // copy a big array cost
        val bigArr = Random.nextBytes(8192*3*128)
        val arr2 = ByteArray(8192*3*128)
        timeProfiling(1) {System.arraycopy(bigArr, 0, arr2, 0, bigArr.size)}
                .also {println("directly copy big array time cost: ${toMs(it.first)} ms")}


        // copy once copy cost
        val final = ByteArray(8192*3*128)
        var pointer = 0
        val repeatTime = 8192*3
        val bits = Random.nextBytes(128)
        val onceCost = timeProfiling(repeatTime) {
            System.arraycopy(bits, 0, final, pointer, bits.size)
            pointer += bits.size
            0
        }.let {
            println("copy once time cost: ${toMs(it.first)} ms")
            toMs(it.first)
        }



        // simulate depth 3 copy
        val final2 = recursiveCopy(3)
        val cost = toMs(final2.second)
        println("depth 3 copy simulation time cost: $cost ms")
        println("redundant copy cost more ${cost/onceCost} times than flat copy")
        /**
         * test result: depth3 copy with redundant copy takes 20-40 times more time than flat copy
         * tested on JDK17 (Eclipse Adoptium Temurin-17.0.3+7)
         */
    }



    @Test
    /**
     * test the creation cost for two different interface structure:
     *
     * one is a father interface with any sub interfaces, another is an container has a list contains all sub-interfaces
     */
    fun interfacesTest() {
        // value preparation
        val bytes = ByteArray(100)
        val deque = ArrayDeque<Any>()
        val inputStream = ByteArrayInputStream(bytes)
        val outputStream = ByteArrayOutputStream()

        val times = 1000*10000

        timeProfiling(times) {
            directInherit(bytes,deque,inputStream,outputStream)
        }.also {
            println("direct inherit time cost: ${it.first/1000/1000} ms")
        }

        timeProfiling(times) {
            dynamicProxies(bytes,deque,inputStream,outputStream)
        }.also {
            println("created by dynamic proxies time cost: ${it.first/1000/1000} ms")
        }

        timeProfiling(times) {interfaceComb2(bytes,deque,inputStream,outputStream)}.also {
            println("direct inherit but encapsulated with an interface time cost: ${it.first/1000/1000} ms")
        }


        timeProfiling(times) {arrayCombination2(bytes,deque,inputStream,outputStream)}.also {
            println("combined to array inherit time cost: ${it.first/1000/1000} ms")
        }

        timeProfiling(times) {directInheritBig(bytes,deque,inputStream,outputStream)}.also {
            println("big inherit time cost: ${it.first/1000/1000} ms")
        }

        timeProfiling(times) {linkInterface(bytes,deque,inputStream,outputStream)}.also {
            println("link interface creation time cost: ${it.first/1000/1000} ms")
        }

        val directInherit = directInherit(bytes,deque,inputStream,outputStream)
        val arrayComb = arrayCombination2(bytes,deque,inputStream,outputStream)
        val linkInterface = linkInterface(bytes,deque,inputStream,outputStream)

        val dynamicProxies = dynamicProxies(bytes,deque,inputStream,outputStream).also {
            it as PR_A; it as PR_B; it as PR_C; it as PR_D
            it.bytes; it.deque
            it.inStream; it.outputStream
            it.mem1; it.mem2
            println("test read var from proxies: ${it.bytes} ${it.deque} ${it.inStream} ${it.outputStream} ${it.mem1} ${it.mem2}")
        }

        val bias = timeProfiling(times) {
            val arr = arrayOfNulls<Any>(6)
            arr[0] = true
            arr[1] = true
            arr[2] = bytes
            arr[3] = deque
            arr[4] = inputStream
            arr[5] = outputStream
            arr
        }

        timeProfiling(times) {
            val arr = arrayOfNulls<Any>(6)
            //PR_A,PR_B,PR_C,PR_D
            directInherit as PR_A
            directInherit as PR_B
            directInherit as PR_C
            directInherit as PR_D
            arr[0] = directInherit.mem1
            arr[1] = directInherit.mem2
            arr[2] = directInherit.bytes
            arr[3] = directInherit.deque
            arr[4] = directInherit.inStream
            arr[5] = directInherit.outputStream
            arr
        }.also {
            println("direct inherit access time cost ${(it.first-bias.first)/1000/1000} ms")
        }

        timeProfiling(times) {
            val arr = arrayOfNulls<Any>(6)
            //PR_A,PR_B,PR_C,PR_D
            dynamicProxies as PR_A
            dynamicProxies as PR_B
            dynamicProxies as PR_C
            dynamicProxies as PR_D
            arr[0] = dynamicProxies.mem1
            arr[1] = dynamicProxies.mem2
            arr[2] = dynamicProxies.bytes
            arr[3] = dynamicProxies.deque
            arr[4] = dynamicProxies.inStream
            arr[5] = dynamicProxies.outputStream
            arr
        }.also {
            println("dynamic proxies access time cost ${(it.first-bias.first)/1000/1000} ms")
        }

        timeProfiling(times) {
            val arr = arrayOfNulls<Any>(6)
            arr[0] = (arrayComb[0] as PR_A).mem1
            arr[1] = (arrayComb[1] as PR_B).mem2
            arr[2] = (arrayComb[3] as PR_D).bytes
            arr[3] = (arrayComb[1] as PR_B).deque
            arr[4] = (arrayComb[2] as PR_C).inStream
            arr[5] = (arrayComb[2] as PR_C).outputStream
            arr
        }.also {
            println("array combination access time cost ${(it.first-bias.first)/1000/1000} ms")
        }

        timeProfiling(times) {
            val arr = arrayOfNulls<Any>(6)
            arr[0] = linkInterface.getHasHead()!!.hasHead
            arr[1] = linkInterface.getOnStream()!!.onStream
            arr[2] = linkInterface.getBytes()!!.bytes
            arr[3] = linkInterface.getParents()!!.parents
            arr[4] = linkInterface.getOnStream()!!.inputStream
            arr[5] = linkInterface.getOnStream()!!.outputStream
            arr
        }.also {
            println("link interface access time cost ${(it.first-bias.first)/1000/1000} ms")
        }

        /**
         * result comes weired: on platform JDK17(Eclipse Adoptium Temurin-17.0.3+7), arrayCombination and arrayCombination2 is really similar
         * that arrayCombination just encapsulate array with an interface, but the final performance has three times worse
         * and direct create object implements multiple interfaces is still fastest,
         * even faster than create a large object with all those properties in all implemented interfaces
         */

    }



    @Test
    /**
     * test performance of assign property to hash map and assign property to a class
     */
    fun hashMapCostAndClassAssignCost() {
        val outputStream = ByteArrayOutputStream()
        val deque = ArrayDeque<Any>()
        val times = 1000*10000

        // map cost
        timeProfiling(times) {
            val map = HashMap<String, Any>()
            map["HAS_HEAD"] = true
            map["ON_STREAM"] = true
            map["OUTPUT_STREAM"] = outputStream
            map["PARENTS"] = deque
            map
        }.also {println("map cost: ${it.first/1000/1000} ms")}

        // obj cost
        timeProfiling(times) {
            MockSerializationIntent(true, true, outputStream, deque)
        }.also {println("obj cost: ${it.first/1000/1000} ms")}

        // obj2 cost
        timeProfiling(times) {
            MockIntent2(true, true, outputStream, deque)
        }.also {println("obj2 cost: ${it.first/1000/1000} ms")}
    }



    @Test
    /**
     * test the performance of using anonymous object/local object, compared with object from normal class
     */
    fun anonymousObjPerformanceTest() {
        val outputStream = ByteArrayOutputStream()
        val deque = ArrayDeque<Any>()
        timeProfiling(1000*10000) {MockIntent2(true, true, outputStream, deque)}
                .also {println("normal object cost: ${it.first/1000/1000} ms")}
        timeProfiling(1000*10000) {
            object: IMockIntent {
                override val hasHead = true
                override val onStream = true
                override val outputStream = outputStream
                override val parents = deque
            }
        }.also {println("anonymous object cost: ${it.first/1000/1000} ms")}
    }




    private fun directInherit(bytes:ByteArray, deque:Deque<Any>, inputStream: InputStream, outputStream: OutputStream):PR_TOP {
        return object: PR_A,PR_B,PR_C,PR_D {
            override val mem1: Boolean = true
            override val mem2: Boolean = true
            override val deque: Deque<Any> = deque
            override val inStream: InputStream = inputStream
            override val outputStream: OutputStream = outputStream
            override val bytes: ByteArray = bytes
        }
    }

    private fun dynamicProxies(bytes:ByteArray, deque:Deque<Any>, inputStream: InputStream, outputStream: OutputStream):PR_TOP {
        return Proxy.newProxyInstance(
                this::class.java.classLoader,
                arrayOf(PR_A::class.java,PR_B::class.java,PR_C::class.java,PR_D::class.java)
        ) { _,method,_->
            when (method.name) {
                "getMem1" -> return@newProxyInstance true
                "getMem2" -> return@newProxyInstance true
                "getDeque" -> return@newProxyInstance deque
                "getInStream" -> return@newProxyInstance inputStream
                "getOutputStream" -> return@newProxyInstance outputStream
                "getBytes" -> return@newProxyInstance bytes
                else -> { }
            }
        } as PR_TOP
    }

    private fun interfaceComb2(bytes:ByteArray, deque:Deque<Any>, inputStream: InputStream, outputStream: OutputStream):PR_TOP {
        return object: PR_COMB2 {
            override val mem1: Boolean = true
            override val mem2: Boolean = true
            override val deque: Deque<Any> = deque
            override val inStream: InputStream = inputStream
            override val outputStream: OutputStream = outputStream
            override val bytes: ByteArray = bytes
        }
    }

    private fun directInheritBig(bytes:ByteArray, deque:Deque<Any>, inputStream: InputStream, outputStream: OutputStream):PR_TOP {
        return object: PR_BIG {
            override val mem1: Boolean = true
            override val mem2: Boolean = true
            override val deque: Deque<Any> = deque
            override val inStream: InputStream = inputStream
            override val outputStream: OutputStream = outputStream
            override val bytes: ByteArray = bytes
        }
    }

    private fun arrayCombination2(bytes:ByteArray, deque:Deque<Any>, inputStream: InputStream, outputStream: OutputStream):Array<PR_TOP> {
        return arrayOf(
                object:PR_A { override val mem1: Boolean = true },
                object:PR_B {
                    override val mem2: Boolean = true
                    override val deque: Deque<Any> = deque
                },
                object:PR_C {
                    override val inStream: InputStream = inputStream
                    override val outputStream: OutputStream = outputStream
                },
                object:PR_D {
                    override val bytes: ByteArray = bytes
                }
        )
    }

    private fun linkInterface(bytes:ByteArray, deque:Deque<Any>, inputStream: InputStream, outputStream: OutputStream):L_TOP {
        val base = object:L_TOP{}
        val hasHead = object:L_TOP by base, L_HasHead {
            override val hasHead: Boolean = true
            override fun delegate(): L_TOP = base
        }
        val bytes = object:L_TOP by hasHead, L_Bytes {
            override val bytes: ByteArray = bytes
            override fun delegate(): L_TOP = hasHead
        }
        val onStream = object:L_TOP by bytes, L_OnStream {
            override val onStream: Boolean = true
            override val outputStream: OutputStream = outputStream
            override val inputStream: InputStream = inputStream
            override fun delegate(): L_TOP = bytes
        }
        val parents = object:L_TOP by onStream, L_Parents {
            override val parents: Deque<Any> = deque
            override fun delegate(): L_TOP = onStream
        }
        return parents
    }

    private fun recursiveCopy(depth:Int):Pair<ByteArray, Long> {
        if (depth==0) return Pair(Random.nextBytes(128), 0)
        if (depth==1) {
            var start = -System.nanoTime()
            val bits = ByteArray(128*24)
            var pointer = 0
            repeat(24) {
                val sub = recursiveCopy(0)
                System.arraycopy(sub.first, 0, bits, pointer, sub.first.size)
                pointer += sub.first.size
            }
            start += System.nanoTime()
            return Pair(bits, start)
        }
        if (depth>=2) {
            var size = 128*24
            repeat(depth-1) {size*=32}
            var start = -System.nanoTime()
            val bits = ByteArray(size)
            var pointer = 0
            repeat(32) {
                val sub = recursiveCopy(depth-1)
                System.arraycopy(sub.first, 0, bits, pointer, sub.first.size)
                pointer += sub.first.size
                start += sub.second // copy inside function time cost
            }
            start += System.nanoTime()
            return Pair(bits, start)
        }
        throw NotImplementedError()
    }



    private fun post(bits:ByteArray) {
        val builder = StringBuilder()
        builder.append("print some values in bytes prevent some unexpected optimization:")
        repeat(20) {
            builder.append("${bits[Random.nextInt(0, bits.size)]}")
        }
        println(builder.toString())
    }

}

interface PR_TOP
interface PR_A:PR_TOP {
    val mem1:Boolean
}
interface PR_B:PR_TOP {
    val mem2:Boolean
    val deque: Deque<Any>
}
interface PR_C:PR_TOP {
    val inStream: InputStream
    val outputStream:OutputStream
}
interface PR_D:PR_TOP {
    val bytes:ByteArray
}
interface PR_BIG:PR_TOP {
    val mem1:Boolean
    val mem2:Boolean
    val deque: Deque<Any>
    val inStream: InputStream
    val outputStream:OutputStream
    val bytes:ByteArray
}
interface T_COMB {
    val array:Array<PR_TOP>
}
interface PR_COMB2:PR_A,PR_B,PR_C,PR_D

private class MockSerializationIntent(
        val hasHead:Any,
        val onStream:Any,
        val outputStream:Any,
        val parents:Any,
)

private class MockIntent2(
        val hasHead:Boolean,
        val onStream:Boolean,
        val outputStream:OutputStream,
        val parents:Deque<Any>,
)

interface IMockIntent {
    val hasHead:Boolean
    val onStream:Boolean
    val outputStream:OutputStream
    val parents:Deque<Any>
}

interface L_TOP {
    fun delegate():L_TOP {
        return this
    }
}
interface L_HasHead:L_TOP {
    val hasHead:Boolean
}
interface L_OnStream:L_TOP {
    val onStream:Boolean
    val outputStream:OutputStream
    val inputStream:InputStream
}
interface L_Parents:L_TOP {
    val parents:Deque<Any>
}
interface L_Bytes:L_TOP {
    val bytes:ByteArray
}

fun L_TOP.getHasHead():L_HasHead? {
    if (this !is L_HasHead) {
        if (this.delegate()==this) return null
        return this.delegate().getHasHead()
    }
    return this
}

fun L_TOP.getOnStream():L_OnStream? {
    if (this !is L_OnStream) {
        if (this.delegate()==this) return null
        return this.delegate().getOnStream()
    }
    return this
}

fun L_TOP.getParents():L_Parents? {
    if (this !is L_Parents) {
        if (this.delegate()==this) return null
        return this.delegate().getParents()
    }
    return this
}

fun L_TOP.getBytes():L_Bytes? {
    if (this !is L_Bytes) {
        if (this.delegate()==this) return null
        return this.delegate().getBytes()
    }
    return this
}
