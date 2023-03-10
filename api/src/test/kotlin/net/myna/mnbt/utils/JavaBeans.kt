package net.myna.mnbt.utils

import net.myna.mnbt.Tag
import net.myna.mnbt.tag.CompoundTag
import kotlin.random.Random

open class JavaBean {
    var i:Int? = null
    var j:String? = null
    var k:Byte? = null
    var s:Short? = null
    var l:Long? = null
    var f:Float? = null
    var d:Double? = null
    var b:Boolean? = null
    var bits:ByteArray? = null
    var ints:IntArray? = null
    var longs:LongArray? = null

    constructor()

    constructor(i:Int, j:String, k:Byte,
                s:Short, l:Long, f:Float,
                d:Double, bits:ByteArray, ints:IntArray, longs:LongArray) {
        this.i=i; this.j=j; this.k=k
        this.s=s; this.l=l; this.f=f
        this.d=d; this.bits=bits
        this.ints=ints
        this.longs=longs
    }

    override fun equals(other: Any?): Boolean {
        return ApiTestTool.beanObjEqFun(this, other)
    }
}

fun randomJavaBean():JavaBean {
    val testJavaBean = JavaBean(
            Random.nextInt(), RandomValueTool.bitStrC(5)(),
            Random.nextBytes(1)[0], Random.nextInt().toShort(),
            Random.nextLong(), Random.nextFloat(), Random.nextDouble(),
            Random.nextBytes(500), RandomValueTool.intArrC(500)(),
            RandomValueTool.longArrC(500)()
    )
    testJavaBean.b = Random.nextInt() > 0
    return testJavaBean
}

class JavaBean2 {
    var int:Int? = null
    var str:String? = null
    var long:Long? = null

    fun toCompound(name:String?): CompoundTag {
        return CompoundTag(name).also { comp->
            this.int?.let {ApiTestValueBuildTool.prepareTag2("int", it)}.also { comp.add(it as Tag<out Any>)}
            this.str?.let {ApiTestValueBuildTool.prepareTag2("str", it)}.also { comp.add(it as Tag<out Any>)}
            this.long?.let {ApiTestValueBuildTool.prepareTag2("long", it)}.also { comp.add(it as Tag<out Any>)}
        }
    }
}

fun newJavaBean2():JavaBean2 {
    return JavaBean2().also {
        it.int = Random.nextInt()
        it.str = RandomValueTool.bitStrC(15)()
        it.long = Random.nextLong()
    }
}

class JavaBeanWithFlatValueList {
    var byteList:List<Byte>? = null
    var shortList:List<Short>? = null
    var intList:List<Int>? = null
    var longList:List<Long>? = null
    var floatList:List<Float>? = null
    var doubleList:List<Double>? = null
    var stringList:List<String>? = null
    var bitsList:List<ByteArray>? = null
    var intsList:List<IntArray>? = null
    var longsList:List<LongArray>? = null

    constructor()

    constructor(byteList:List<Byte>, shortList:List<Short>,
                intList:List<Int>, longList:List<Long>,
                floatList:List<Float>, doubleList:List<Double>,
                stringList:List<String>, bitsList:List<ByteArray>,
                intsList:List<IntArray>, longsList:List<LongArray>
    ) {
        this.byteList = byteList
        this.shortList = shortList
        this.intList = intList
        this.longList = longList
        this.floatList = floatList
        this.doubleList = doubleList
        this.stringList = stringList
        this.bitsList = bitsList
        this.intsList = intsList
        this.longsList = longsList
    }

    override fun equals(other: Any?): Boolean {
        return ApiTestTool.beanObjEqFun(this, other)
    }
}

class JavaBeanWithFlatValueIterable {
    var byteArray:Array<Byte>? = null
    var bytesIterable:Iterable<ByteArray>? = null
    var longArray:Array<Long>? = null
    var strIterable:Iterable<String>? = null

    override fun equals(other: Any?): Boolean {
        return ApiTestTool.beanObjEqFun(this, other)
    }
}

open class JavaBean3:JavaBean() {
    var bean3Int:Int? = null
    var bean3Long:Long? = null
    var bean3Str:String? = null

    override fun equals(other: Any?): Boolean {
        return ApiTestTool.beanObjEqFun(this, other)
    }
}

/**
 * bean with Map type contains another beans
 */
class JavaBean4:JavaBean3() {

    var beanMap:Map<String, JavaBean2>? = null
    var beanList:List<JavaBean>? = null
    var bean4Var:String? = null

    override fun equals(other: Any?): Boolean {
        return ApiTestTool.beanObjEqFun(this, other)
    }
}

fun randomJavaBean4(num:Int = 10):JavaBean4 {
    return JavaBean4().also {
        it.bean4Var = "some string"
        it.bean3Int = Random.nextInt()
        it.d = Random.nextDouble()
        it.beanMap = HashMap<String, JavaBean2>().also {
            it["bean2 1"] = newJavaBean2()
            it["bean2 2"] = newJavaBean2()
            it["bean2 3"] = newJavaBean2()
        }

        it.beanList = ArrayList<JavaBean>().also{ list->
            repeat(num) {
                list.add(randomJavaBean())
            }
        }
    }
}
