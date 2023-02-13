package com.myna.mnbt.converter

import com.myna.mnbt.Tag
import com.myna.mnbt.converter.procedure.ToNestTagProcedure
import com.myna.mnbt.converter.procedure.ToNestTagProcedure.Companion.procedure
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

    var completeOverride: Boolean = true

    /**
     * create a new list Tag converter that can convert ListTag between ArrayType object
     * ARR is another value type that may used to create tag / get value
     */
    class ArrayTypeListTagConverter(override var proxy: TagConverter<Any>):
            HierarchicalTagConverter<AnyTagList>() {


        override fun <ARR:Any> createTag(name: String?, value: ARR, typeToken: MTypeToken<out ARR>, intent: CreateTagIntent): Tag<AnyTagList>? {
            return ArrayToTagProcedure(ToNestTagProcedure.BaseArgs(proxy, name, value, typeToken, intent)).procedure()
        }


        override fun <ARR:Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out ARR>, intent: ToValueIntent): Pair<String?, ARR>? {
            intent as RecordParents; intent as ToValueIntent
            if (tag.value !is List<*>) return null
            val value = tag.value as MutableList<out Tag<out Any>>
            if (!typeToken.isArray) return null
            val arrComp = typeToken.componentType?: return null
            val array = java.lang.reflect.Array.newInstance(arrComp.rawType, value.size)
            value.onEachIndexed { i, element ->
                val v = proxy.toValue(element, arrComp, nestCIntent(intent, false))?:return@onEachIndexed
                java.lang.reflect.Array.set(array, i, v.second)
            }
            return Pair(tag.name, array as ARR)
        }



    }

    class IterableTypeConverter(override var proxy: TagConverter<Any>)
        : HierarchicalTagConverter<AnyTagList>() {


        override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: CreateTagIntent): Tag<AnyTagList>? {
            return IterableToTagProcedure(ToNestTagProcedure.BaseArgs(proxy, name, value, typeToken, intent)).procedure()
        }

        override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ToValueIntent): Pair<String?, V>? {
            intent as RecordParents; intent as ToValueIntent
            // check tagValue is List
            if (tag.value !is List<*>) return null
            val value = tag.value as AnyTagList
            // if ignore typeToken, then set typeToken as default List
            val actualTypeToken = if (intent is IgnoreValueTypeToken) iterableType else typeToken
            if (!actualTypeToken.isSubtypeOf(iterableType)) return null
            val declaredElementType = actualTypeToken.resolveType(iterableGenericType) as MTypeToken<out Any>
            // try get list instance from typetoken
            val list = newList(actualTypeToken)?: return null

            value.onEach { elementTagValue->
                val elementType = if(value.size>0) value.get(0).let{MTypeToken.of(it.value::class.java)} else null
                elementType as MTypeToken<out Any>
                val metaTargetElement = proxy.toValue(elementTagValue, declaredElementType, intent) ?:
                proxy.toValue(elementTagValue, elementType, nestCIntent(intent, true)) ?: return@onEach
                list.add(metaTargetElement.second)
            }
            return Pair(tag.name, list as V)
        }

        fun newList(typeToken: MTypeToken<*>):MutableList<Any>? {
            val rawType = typeToken.rawType
            // if it is interface/abstract class extends List interface
            if (rawType.isInterface || Modifier.isAbstract(rawType.modifiers) && iterableType.isSupertypeOf(rawType)) return mutableListOf()
            // try find empty constructor
            val constructors = rawType.constructors
            val constructor = constructors.find { constructor->
                constructor.parameters.isEmpty()
            }?: return null
            return constructor.newInstance() as MutableList<Any>
        }

    }

    open class ArrayToTagProcedure(override val baseArgs: ToNestTagProcedure.BaseArgs)
        : ToNestTagProcedure<ListTag<Any>, ListSubTagArgs> {

        override fun toSubTagArgsList(): List<ListSubTagArgs> {
            val size = java.lang.reflect.Array.getLength(value)
            val compType = typeToken.componentType
            val list = ArrayList<ListSubTagArgs>()
            for (i in IntRange(0, size-1)) {
                val v = java.lang.reflect.Array.get(value, i)
                list.add(ListSubTagArgs(i, null, v, compType, intent))
            }
            return list
        }

        override fun checkProcedureArgs(): Boolean {
            if (!typeToken.isArray) return false
            return true
        }

        override fun buildTargetTag(subTags: List<Pair<Tag<out Any>?, ListSubTagArgs>>): ListTag<Any> {
            val overrideTarget = if (intent is OverrideTag) (intent as OverrideTag).overrideTarget else null
            val isListTag = overrideTarget is Tag.NestTag && overrideTarget.value is List<*>
            val overrideTargetList = if (isListTag) overrideTarget!!.value as List<Tag<out Any>> else null



            val elementId:Byte = subTags.firstOrNull()?.first?.id ?: overrideTargetList?.firstOrNull()?.id ?: ListTag.unknownElementId
            val listTagContent:AnyTagList = mutableListOf()
            subTags.onEachIndexed { i,pair->
                val tag = pair.first
                val addedTag = tag ?: if (!completeOverride) overrideTargetList?.getOrNull(i) else null
                if (addedTag != null) listTagContent.add(addedTag)
            }
            // append un-overridden if overridePartOfList
            if (!completeOverride && overrideTargetList!=null && overrideTargetList.size > listTagContent.size) {
                val remain = overrideTargetList.subList(listTagContent.size, overrideTargetList.size)
                listTagContent.addAll(remain)
            }
            return ListTag(this.targetName, listTagContent, elementId)
        }

        override fun toSubTagRelatePath(args: ListSubTagArgs): String {
            return "#${args.index}"
        }
    }

    class IterableToTagProcedure(baseArgs: ToNestTagProcedure.BaseArgs):ArrayToTagProcedure(baseArgs) {
        override fun toSubTagArgsList(): List<ListSubTagArgs> {
            val declaredElementType = typeToken.resolveType(iterableGenericType) as MTypeToken<out Any>
            return (value as Iterable<*>).mapIndexed { i,sv ->
                ListSubTagArgs(i, null, sv, declaredElementType, intent)
            }
        }

        override fun checkProcedureArgs(): Boolean {
            if(!typeToken.isSubtypeOf(iterableType)) return false
            return true
        }
    }


    class ListSubTagArgs(val index:Int,
                         name:String?, value:Any?, typeToken: MTypeToken<out Any>, intent:CreateTagIntent)
        : ToNestTagProcedure.ToSubTagArgs(name, value, typeToken, intent)

    private val iterableType = MTypeToken.of(Iterable::class.java)
    private val iterableGenericType = Iterable::class.java.typeParameters[0]

}