package mnbt

import mnbt.utils.*
import mnbt.utils.ApiTestValueBuildTool.listPreparation
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

/**
 * this class is for testing utils' validation that used in Api test
 */
class TestUtilTest {

    @Test
    fun beanEqFunValidationTest() {
        val beanEq = ApiTestTool::beanObjEqFun

        val bean1 = JavaBean2()
        val bean2 = JavaBean2()
        assertTrue(beanEq(bean1, bean2)) // if all properties is null, then equal
        bean1.int = Random.nextInt()
        bean1.str = "some string"
        bean1.long = Random.nextLong()

        bean2.int = bean1.int
        bean2.long = bean1.long
        bean2.str = bean1.str
        val bean3 = JavaBean()

        // assert bean eq for bean class with nullable primitive type
        assertTrue(beanEq(bean1, bean2))
        bean2.str = null
        assertTrue(!beanEq(bean1, bean2))
        bean2.str = "some string"
        assertTrue(beanEq(bean1, bean2))

        assertTrue(!beanEq(bean1, bean3))

        // check bean eq for bean class with list<primitive> type
        val bean4 = JavaBeanWithFlatValueList()
        bean4.bitsList = listPreparation(50) {Random.nextBytes(20)}
        bean4.intList = listPreparation(17) {Random.nextInt()}
        val bean5 = JavaBeanWithFlatValueList()
        bean5.bitsList = ArrayList<ByteArray>().also { newList-> bean4.bitsList!!.onEach { origin->
            newList.add(ByteArray(origin.size).also {System.arraycopy(origin, 0, it, 0, origin.size)})
        } }
        assertTrue(!beanEq(bean4,bean5))
        assertTrue(!beanEq(bean4,bean1))
        bean5.intList = ArrayList<Int>().also { newList-> bean4.intList!!.onEach { i->
            newList.add(i)
        }}
        assertTrue(beanEq(bean4,bean5))

        // test Array<*> and Iterable<*>
        val bean6 = JavaBeanWithFlatValueIterable()
        bean6.byteArray = Array(500) {Random.nextBytes(1)[0]}
        bean6.bytesIterable = Array(10) {Random.nextBytes(Random.nextInt(20,40))}.asIterable()
        bean6.strIterable = ArrayList<String>().also {list-> repeat(58) {list.add(RandomValueTool.bitStrC(15)())} }
        bean6.longArray = LongArray(200) {Random.nextLong()}.toTypedArray()
        val bean7 = JavaBeanWithFlatValueIterable()
        bean7.byteArray = bean6.byteArray
        bean7.bytesIterable = LinkedList<ByteArray>().also {list -> bean6.bytesIterable!!.onEach {
            val newBits = ByteArray(it.size) {i -> it[i]}
            list.add(newBits)
        }}
        bean7.longArray = Array(200) {i->bean6.longArray!![i]}
        bean7.strIterable = mutableListOf<String>().also { list->
            bean6.strIterable!!.onEach { list.add(it) }
        }

        assertTrue(beanEq(bean6, bean7))
        bean7.byteArray = Array(500) { i-> bean6.byteArray!![i]}
        assertTrue(beanEq(bean6, bean7))
        val b:Byte = bean7.byteArray!![25]
        bean7.byteArray!![25] = (1.toByte()+b).toByte()
        assertTrue(!beanEq(bean6, bean7))
        bean7.byteArray = bean6.byteArray

        // test bean inherited from another bean
        val bean8 = JavaBean3()
        val bean9 = JavaBean3()
        assertTrue(beanEq(bean8, bean9))
        bean8.ints = RandomValueTool.intArrC(100)()
        bean8.bean3Str = "some str"
        bean9.ints = bean8.ints
        bean9.bean3Str = bean8.bean3Str
        assertTrue(beanEq(bean8, bean9))
        bean9.b = false
        assertTrue(!beanEq(bean8, bean9))
        bean9.b = null
        assertTrue(beanEq(bean8, bean9))
        bean9.bean3Int = 555
        assertTrue(!beanEq(bean8, bean9))
    }
}