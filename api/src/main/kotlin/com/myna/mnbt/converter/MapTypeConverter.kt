package com.myna.mnbt.converter

import com.myna.mnbt.IdTagCompound
import com.myna.mnbt.Tag
import com.myna.mnbt.exceptions.*
import com.myna.mnbt.converter.procedure.ToNestTagProcedure
import com.myna.mnbt.converter.procedure.ToNestTagProcedure.Companion.procedure
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag
import java.lang.reflect.Modifier
import java.util.*


/**
 * map type converter from this function can only handle limit type.
 * if input TypeToken is Map<String, Any>, then it can handle those value type in map which can directly derive to
 * eg: String,Int,Long primitive type and their wrapper type. ArrayType, etc.
 *
 * or if you set TypeToken more accurately, like Map<String, Array<Int>>, then converter can directly handle it.
 * if the type token declaration have nest Map type like Map<String, SomTypeDef<...,Map<String, Any>>>,
 * and converter proxy ensures all map type handle in this converter, then it is expected to handle inner parrameterized Map type like above.
 *
 * if want to use this converter the create map from Tag/TagValue, it also can only handle enum map types, but not all sub-classes of map
 */
class MapTypeConverter(override var proxy: TagConverter<Any>): HierarchicalTagConverter<AnyCompound>() {

    override fun <V:Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: CreateTagIntent): Tag<AnyCompound>? {
        return MapToTagProcedure(ToNestTagProcedure.BaseArgs(proxy, name, value, typeToken, intent)).procedure()
    }

    override fun <V:Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ToValueIntent): Pair<String?, V>? {
        intent as RecordParents; intent as ToValueIntent
        // check tagValue is MutableMap or not
        if (tag.value !is Map<*,*>) return null
        val value = tag.value as Map<String, Tag<out Any>>
        // if ignoreTypeToken is true, toValue as default Map type
        val actualTypeTokenIn = if (intent is IgnoreValueTypeToken) mapTypeToken else typeToken
        // check type token is subtype of map or not
        if (!actualTypeTokenIn.isSubtypeOf(mapTypeToken)) return null
        // try get fix delegate from typeToken declaration
        val declaredValueTypeToken = actualTypeTokenIn.resolveType(mapValueGenericType) as MTypeToken<out Any>
        val newMap = newMapInstance(actualTypeTokenIn)?: return null //TODO: may better calling ways to get map instance
        newMap as MutableMap<String, Any>

        value.onEach { entry->
            val subTag = entry.value
            checkNotNull(subTag.name)
            val subTagValueTypeToken = MTypeToken.of(subTag.value::class.java) as MTypeToken<out Any>
            // because proxy will auto restrict to tag.value type if type token is type variable, so we do not need to handle it
            val res = proxy.toValue(subTag, declaredValueTypeToken, nestCIntent(intent, false))?: return@onEach
            newMap[subTag.name!!] = res.second
        }

        //val value = toValue(tag.value as Any, typeToken, ArrayDeque())?: return null
        return Pair(tag.name, newMap as V)
    }

    private fun <V> newMapInstance(typeToken: MTypeToken<V>):V? {
        val rawType = typeToken.rawType
        // if it is interface/abstract class extends Map interface
        if (rawType.isInterface || Modifier.isAbstract(rawType.modifiers) && mapTypeToken.isSupertypeOf(rawType)) return mutableMapOf<String, Any>() as V
        // try to find empty constructor
        val constructors = typeToken.rawType.constructors
        val constructor = constructors.find { constructor->
            constructor.parameters.isEmpty()
        }?: return null
        return constructor.newInstance() as V
    }

    class MapToTagProcedure(override val baseArgs: ToNestTagProcedure.BaseArgs)
        : ToNestTagProcedure<CompoundTag, ToNestTagProcedure.ToSubTagArgs> {

        override fun toSubTagRelatePath(args: ToNestTagProcedure.ToSubTagArgs): String {
            return "./${args.name!!}"
        }

        override fun toSubTagArgsList(): List<ToNestTagProcedure.ToSubTagArgs> {
            val value = this.value as Map<String,Any>
            val declaredValueTypeToken = this.typeToken.resolveType(mapValueGenericType) as MTypeToken<out Any>
            return value.map { entry ->
                ToNestTagProcedure.ToSubTagArgs(entry.key, entry.value, declaredValueTypeToken, this.intent)
            }
        }

        override fun buildTargetTag(subTags: List<Pair<Tag<out Any>?,ToNestTagProcedure.ToSubTagArgs>>): CompoundTag? {
            val map:AnyCompound = mutableMapOf()
            subTags.forEach { pair->
                val tag = pair.first
                tag?.name?.let { map[it] = tag }
            }
            // try append miss Tag
            ToNestTagProcedure.tryAppendMissSubTag(map, this.intent)
            return CompoundTag(this.targetName, map)
        }

        override fun checkProcedureArgs(): Boolean {
            if (!this.typeToken.isSubtypeOf(mapTypeToken)) return false
            return true
        }
    }

    companion object {
        private val mapTypeToken = MTypeToken.of(Map::class.java)
        private val mapValueGenericType = Map::class.java.typeParameters[1]
    }
}