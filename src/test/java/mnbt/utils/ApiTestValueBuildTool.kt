package mnbt.utils


import com.myna.mnbt.*
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.ArrayTag
import com.myna.mnbt.tag.ListTag
import com.myna.mnbt.tag.PrimitiveTag
import java.util.ArrayList
import kotlin.random.Random

/**
 * provide Codec and Convertor
 */
object ApiTestValueBuildTool {

    /**
     * function for nested nbt acceptable values with only map type as nested structure
     * each map in the whole structure will have total other flat type nbt accepted value
     * @param depth the depth want for nested map, if depth<1, then map is flat
     */
    fun nestedMapValuesPreparation(depth:Int, provideMap:()->MutableMap<String, Any>):Triple<String, Int, Map<String, Any>> {
        var rootBitsLen = TagIdPayload // add TagEnd id payload
        val root = provideMap()
        flatTagValuesPreparation().onEach { flat->
            // add tag head payload and tag value payload
            rootBitsLen += TagHeadFixPayload+flat.second+flat.first.toByteArray(Charsets.UTF_8).size
            root[flat.first] = flat.third
        }
        if (depth>1) {
            val map = nestedMapValuesPreparation(depth-1, provideMap)
            rootBitsLen += TagHeadFixPayload+map.second+map.first.toByteArray(Charsets.UTF_8).size
            root[map.first] = map.third
        }
        return Triple("map object with depth $depth", rootBitsLen, root)
    }

    fun flatMapListsPreparation(nameIn:String, listSize:Int, provideMap:()->MutableMap<String, Any>):Triple<String, Int, List<Map<String, Any>>> {
        val list = ArrayList<Map<String, Any>>()
        val name = "$nameIn List contains $listSize Map objects"
        // value will have element id and array size at the beginning
        var valueBitsLen = TagIdPayload + ArraySizePayload
        repeat(listSize) {
            val metaMap = nestedMapValuesPreparation(1, provideMap)
            list.add(metaMap.third as Map<String, Any>)
            valueBitsLen += metaMap.second
        }
        return Triple(name, valueBitsLen, list)
    }

    fun mapWithListPreparation(nameIn:String, listNum:Int):Triple<String, Int, Map<String, Any>> {
        val map = HashMap<String, Any>()
        val name = "$nameIn map contains $listNum list objects"
        // Tag_End payload
        var valueBitsLen = TagIdPayload
        repeat(listNum) {
            val key = "list in map $it"
            val list = flatMapListsPreparation(key, 3) {HashMap()}
            valueBitsLen += TagHeadFixPayload + list.first.toByteArray(Charsets.UTF_8).size + list.second
            map[list.first] = list.third
        }
        return Triple(name, valueBitsLen, map)
    }

    //
    /**
     * @param profiling value is used for profiling: which means all type of object will have same payload, tag.name will be empty for clean memory structure
     * @return Triple: first element is tag name, second one is target tag value payload(without tag head payload),
     * third one is object prepared to transfer to tag
     */
    fun flatTagValuesPreparation(profiling :Boolean=false, payloadEachType:Int=4000):List<Triple<String, Int, Any>>  {
        val list = ArrayList<Triple<String, Int, Any>>()
        val rstrC:()->String = if (profiling) ({""}) else RandomValueTool.bitStrC(5)


        val elementAdd:(list:MutableList<Triple<String, Int, Any>>, createElement:()->Triple<String, Int, Any>)->Unit = {
            list, createElement->
            if (profiling) {
                var remain = payloadEachType
                while(remain>0) {
                    val element = createElement()
                    list.add(element)
                    val strBitSize = element.first.toByteArray(Charsets.UTF_8).size
                    remain -= element.second+strBitSize
                }
            }
            else list.add(createElement())
        }

        elementAdd(list) { Triple(rstrC(), IntSizePayload, Random.nextInt() ) }
        elementAdd(list) { Triple(rstrC(), ShortSizePayload, Random.nextInt().toShort() ) }
        elementAdd(list) { Triple(rstrC(), ByteSizePayload, Random.nextBytes(1)[0] ) }
        elementAdd(list) { Triple(rstrC(), LongSizePayload, Random.nextLong() ) }
        elementAdd(list) { Triple(rstrC(), FloatSizePayload, Random.nextFloat() ) }
        elementAdd(list) { Triple(rstrC(), DoubleSizePayload, Random.nextDouble() ) }
        elementAdd(list) {
            val randomStr = if (!profiling) rstrC() else "test str"
            Triple(rstrC(), randomStr.toByteArray().size+StringSizePayload, randomStr)
        }

        //ArrayType
        //val arrSize = 1000
        elementAdd(list) {
            val rstr = rstrC()
            val arrSize = (payloadEachType-rstr.toByteArray(Charsets.UTF_8).size-ArraySizePayload)/IntSizePayload+1
            Triple(rstrC(), ArraySizePayload+arrSize*IntSizePayload, RandomValueTool.intArrC(arrSize)())
        }
        elementAdd(list) {
            val rstr = rstrC()
            val arrSize = (payloadEachType-rstr.toByteArray(Charsets.UTF_8).size-ArraySizePayload)/ByteSizePayload+1
            Triple(rstrC(), ArraySizePayload+arrSize*ByteSizePayload, RandomValueTool.byteArrC(arrSize)())
        }
        elementAdd(list) {
            val rstr = rstrC()
            val arrSize = (payloadEachType-rstr.toByteArray(Charsets.UTF_8).size-ArraySizePayload)/LongSizePayload+1
            Triple(rstrC(), ArraySizePayload+arrSize*LongSizePayload, RandomValueTool.longArrC(arrSize)())
        }
        return list

    }

    fun <V> listPreparation(num:Int, valueProvider:()->V):List<V> {
        return ArrayList<V>().also { list->
            repeat(num) {
                list.add(valueProvider())
            }
        }
    }

    fun <V:Any> iterableToListTag(name:String?, id:Byte, iterable:Iterable<V>, tagCreation:(v:V)->Tag<out Any>): ListTag<out Any> {
        return ListTag<Any>(id, name).also { listTag->
            iterable.onEach {value->
                listTag.add(tagCreation(value) as Tag<Any>)
            }
        }
    }

    fun <V:Any> prepareTag2(name:String, value:V):Tag<out Any> = prepareTag(value, name)

    fun <V:Any> prepareTag(value:V, name:String? = null):Tag<out Any> {
        return when(value) {
            is Boolean -> PrimitiveTag.ByteTag(name, if (value) 1 else 0)
            is Byte -> PrimitiveTag.ByteTag(name, value)
            is Short -> PrimitiveTag.ShortTag(name, value)
            is Int -> PrimitiveTag.IntTag(name, value)
            is Long -> PrimitiveTag.LongTag(name, value)
            is Float -> PrimitiveTag.FloatTag(name, value)
            is Double -> PrimitiveTag.DoubleTag(name, value)
            is String -> PrimitiveTag.StringTag(name, value)
            is ByteArray -> ArrayTag.ByteArrayTag(name, value)
            is IntArray -> ArrayTag.IntArrayTag(name, value)
            is LongArray -> ArrayTag.LongArrayTag(name, value)
            is Array<*> -> {
                val comp = value::class.java.componentType
                when (comp) {
                    Byte::class.java,java.lang.Byte::class.java -> ArrayTag.ByteArrayTag(name, (value as Array<Byte>).toByteArray()) as Tag<V>
                    Int::class.java,java.lang.Integer::class.java -> ArrayTag.IntArrayTag(name, (value as Array<Int>).toIntArray()) as Tag<V>
                    Long::class.java,java.lang.Long::class.java -> ArrayTag.LongArrayTag(name, (value as Array<Long>).toLongArray()) as Tag<V>
                    else -> TODO()
                }
            }
            else -> TODO()
        }
    }

    fun flatValueArraysPreparation(size:Int):List<Triple<String, Int, Array<Any>>> {
        val list = ArrayList<Triple<String, Int, Array<Any>>>()
        val rstrC = RandomValueTool.bitStrC(5)
        val arrSize = 200
        val listVFixPayload = ArraySizePayload+TagIdPayload
        Array<Any>(size) {Random.nextInt()}.also { list.add(Triple(rstrC(), IntSizePayload*size+listVFixPayload, it)) }
        Array<Any>(size) {Random.nextInt().toShort()}.also { list.add(Triple(rstrC(), ShortSizePayload*size+listVFixPayload, it)) }
        Array<Any>(size) {Random.nextBytes(1)[0]}.also { list.add(Triple(rstrC(), ByteSizePayload*size+listVFixPayload, it)) }
        Array<Any>(size) {Random.nextFloat()}.also { list.add(Triple(rstrC(), FloatSizePayload*size+listVFixPayload, it)) }
        Array<Any>(size) {Random.nextDouble()}.also { list.add(Triple(rstrC(), DoubleSizePayload*size+listVFixPayload, it)) }
        Array<Any>(size) { RandomValueTool.intArrC(arrSize)()}.also { list.add(Triple(rstrC(), (IntSizePayload*arrSize+ArraySizePayload)*size+listVFixPayload, it)) }
        Array<Any>(size) { RandomValueTool.byteArrC(arrSize)()}.also { list.add(Triple(rstrC(), (ByteSizePayload*arrSize+ArraySizePayload)*size+listVFixPayload, it)) }
        Array<Any>(size) { RandomValueTool.longArrC(arrSize)()}.also { list.add(Triple(rstrC(), (LongSizePayload*arrSize+ArraySizePayload)*size+listVFixPayload, it)) }
        return list
    }

    fun flatValueListsPreparation(size:Int):List<Triple<String, Int, List<Any>>> {
        val arrList = flatValueArraysPreparation(size)
        val listlist = ArrayList<Triple<String, Int, List<Any>>>()
        arrList.onEach {
            listlist.add(Triple(it.first,it.second, it.third.toList()))
        }
        return listlist
    }


    /**
     * @param profiling value is used for profiling: which means all type of object will have same payload, tag.name will be empty for clean memory structure
     */
    fun flatTagsPreparation(profiling:Boolean=false):List<Tag<out Any>> {
        val list = flatTagValuesPreparation(profiling)
        val tagList = ArrayList<Tag<out Any>>()
        list.onEach {
            val tag = TestMnbt.inst.tConverterProxy.createTag(it.first, it.third, MTypeToken.of(it.third::class.java) as MTypeToken<Any>)!!
            tagList.add(tag)
        }
        return tagList
    }

    fun flatValuesListTagsPreparations(size:Int):List<Tag<MutableList<out Any>>> {
        val listOfArrays = flatValueArraysPreparation(size)
        val tagList = ArrayList<Tag<MutableList<out Any>>>()
        listOfArrays.onEach {
            val listTag = TestMnbt.inst.tConverterProxy.createTag(it.first, it.third, object:MTypeToken<Array<Any>>() {}) as Tag<MutableList<out Any>>
            tagList.add(listTag)
        }
        return tagList
    }


}