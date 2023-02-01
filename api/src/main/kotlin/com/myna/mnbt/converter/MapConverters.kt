package com.myna.mnbt.converter

import com.myna.mnbt.Tag
import com.myna.mnbt.exceptions.*
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag
import java.lang.reflect.Modifier
import java.util.*


object MapConverters {


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
    class MapTypeConverter(override var proxy: TagConverter<Any, ConverterCallerIntent>): HierarchicalTagConverter<AnyCompound>() {
            private val mapTypeToken = MTypeToken.of(Map::class.java)
            private val mapValueGenericType = Map::class.java.typeParameters[1]

            override fun <V:Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: ConverterCallerIntent): Tag<AnyCompound>? {
                intent as RecordParents
                if (!typeToken.isSubtypeOf(mapTypeToken)) return null
                // try to get specific type delegate if there is
                val declaredValueTypeToken = typeToken.resolveType(mapValueGenericType) as MTypeToken<out Any>
                value as Map<String, Any>
                val map: AnyCompound = mutableMapOf()
                value.onEach {
                    val subValue = it.value
                    val tag = proxy.createTag(it.key, subValue, declaredValueTypeToken, nestCIntent(intent.parents, false))?:
                    proxy.createTag(it.key, subValue, MTypeToken.of(subValue::class.java) as MTypeToken<out Any>, nestCIntent(intent.parents, true))?: return@onEach
                    //set[it.key] = tag
                    map[tag.name!!] = tag
                }
                return CompoundTag(name, map)
            }

            override fun <V:Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ConverterCallerIntent): Pair<String?, V>? {
                intent as RecordParents; intent as ToValueTypeToken
                // check tagValue is MutableMap or not
                if (tag.value !is Map<*,*>) return null
                val value = tag.value as Map<String, Tag<out Any>>
                // if ignoreTypeToken is true, toValue as default Map type
                val actualTypeTokenIn = if (intent.ignore) mapTypeToken else typeToken
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
                    // just let proxy try two kinds of type token with subTagValue
                    val res = proxy.toValue(subTag, declaredValueTypeToken, nestCIntent(intent.parents, false))?:
                    proxy.toValue(subTag, subTagValueTypeToken, nestCIntent(intent.parents, true)) ?: return@onEach
                    newMap[subTag.name!!] = res.second
                }

                //val value = toValue(tag.value as Any, typeToken, ArrayDeque())?: return null
                return Pair(tag.name, newMap as V)
            }

            private fun <V> newMapInstance(typeToken: MTypeToken<V>):V? {
                val rawType = typeToken.rawType
                // if it is interface/abstract class extends Map interface
                if (rawType.isInterface || Modifier.isAbstract(rawType.modifiers) && mapTypeToken.gToken.isSupertypeOf(rawType)) return mutableMapOf<String, Any>() as V
                // try to find empty constructor
                val constructors = typeToken.rawType.constructors
                val constructor = constructors.find { constructor->
                    constructor.parameters.isEmpty()
                }?: return null
                return constructor.newInstance() as V
            }
        }


}