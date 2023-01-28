package com.myna.mnbt.converter

import com.google.common.reflect.TypeToken
import com.myna.mnbt.Tag
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.AnyTagList
import com.myna.mnbt.tag.ListTag
import java.lang.reflect.Modifier

@Suppress("UnstableApiUsage")
/**
 * in usual, someone who use these converter should ensure all elements in list should be same type,
 * or some unexpected consequence will happen
 */
object ListConverters {

    /**
     * create a new list Tag converter that can convert ListTag between ArrayType object
     * ARR is another value type that may used to create tag / get value
     */
    class ArrayTypeListTagConverter(override var proxy: TagConverter<Any, ConverterCallerIntent>):
            HierarchicalTagConverter<AnyTagList>() {

        override fun <ARR:Any> createTag(name: String?, value: ARR, typeToken: MTypeToken<out ARR>, intent: ConverterCallerIntent): Tag<AnyTagList>? {
            intent as RecordParents
            // first check value is array or not
            if (!typeToken.isArray) return null
            // if typetoken is something like Array<Any>, it will get nothings
            val compType = typeToken.componentType

            val listTagContent:AnyTagList = mutableListOf()
            val size = java.lang.reflect.Array.getLength(value)
            var elementId:Byte = -1
            for (i in IntRange(0, size-1)) {
                val v = java.lang.reflect.Array.get(value, i)
                val subTag = proxy.createTag(null, v, compType, nestCIntent(intent.parents, false))?: continue
                if (elementId==(-1).toByte()) elementId = subTag.id
                listTagContent.add(subTag)
            }

            //val listTagContent = fromValue(value, typeToken, ArrayDeque())?: return null // if cast content null, return null
            return ListTag(name, listTagContent, elementId)
        }


        override fun <ARR:Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out ARR>, intent: ConverterCallerIntent): Pair<String?, ARR>? {
            intent as RecordParents
            if (tag.value !is List<*>) return null
            val value = tag.value as MutableList<out Tag<out Any>>
            if (!typeToken.isArray) return null
            val arrComp = typeToken.componentType?: return null
            val array = java.lang.reflect.Array.newInstance(arrComp.rawType, value.size)
            value.onEachIndexed { i, element ->
                val v = proxy.toValue(element, arrComp, nestCIntent(intent.parents, false))?:return@onEachIndexed
                java.lang.reflect.Array.set(array, i, v.second)
            }
            return Pair(tag.name, array as ARR)
        }

    }

    class IterableTypeConverter(override var proxy: TagConverter<Any, ConverterCallerIntent>)
        : HierarchicalTagConverter<AnyTagList>() {
        private val iterableType = MTypeToken.of(Iterable::class.java)
        private val iterableGenericType = Iterable::class.java.typeParameters[0]

        override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: ConverterCallerIntent): Tag<AnyTagList>? {
            intent as RecordParents
            // check typeToken type is list or not
            if(!typeToken.isSubtypeOf(iterableType)) return null
            value as Iterable<out Any>
            // try declared parameterized type
            val declaredElementType = typeToken.resolveType(iterableGenericType) as MTypeToken<out Any>

            //val size = value.size

            var firstElementType:Class<*>? = null
            val list:AnyTagList = mutableListOf()
            var elementId:Byte = -1
            value.onEach { element->
                // try get first element type
                if (firstElementType==null) firstElementType = element::class.java
                // try to convert value by declared parameterized type or first element actual type
                val convertedValue = proxy.createTag(null, element, declaredElementType, intent) ?:
                firstElementType?.let { proxy.createTag(null, element, MTypeToken.of(firstElementType!!) as MTypeToken<out Any>, nestCIntent(intent.parents, true)) } ?: return@onEach
                if (elementId == (-1).toByte()) elementId = convertedValue.id
                list.add(convertedValue)
            }
            return ListTag(name, list, elementId)
        }

        override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ConverterCallerIntent): Pair<String?, V>? {
            intent as RecordParents; intent as ToValueTypeToken
            // check tagValue is List
            if (tag.value !is List<*>) return null
            val value = tag.value as AnyTagList
            // if ignore typeToken, then set typeToken as default List
            val actualTypeToken = if (intent.ignore) iterableType else typeToken
            if (!actualTypeToken.isSubtypeOf(iterableType)) return null
            val declaredElementType = actualTypeToken.resolveType(iterableGenericType) as MTypeToken<out Any>
            // try get list instance from typetoken
            val list = newList(actualTypeToken)?: return null

            value.onEach { elementTagValue->
                val elementType = if(value.size>0) value.get(0).let{MTypeToken.of(it.value::class.java)} else null
                elementType as MTypeToken<out Any>
                val metaTargetElement = proxy.toValue(elementTagValue, declaredElementType, intent) ?:
                proxy.toValue(elementTagValue, elementType, nestCIntent(intent.parents, true)) ?: return@onEach
                list.add(metaTargetElement.second)
            }
            return Pair(tag.name, list as V)
        }

        fun newList(typeToken: MTypeToken<*>):MutableList<Any>? {
            val rawType = typeToken.rawType
            // if it is interface/abstract class extends List interface
            if (rawType.isInterface || Modifier.isAbstract(rawType.modifiers) && iterableType.gToken.isSupertypeOf(rawType)) return mutableListOf()
            // try find empty constructor
            val constructors = rawType.constructors
            val constructor = constructors.find { constructor->
                constructor.parameters.isEmpty()
            }?: return null
            return constructor.newInstance() as MutableList<Any>
        }

    }

}