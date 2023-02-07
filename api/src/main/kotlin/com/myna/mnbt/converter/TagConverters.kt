package com.myna.mnbt.converter

import com.myna.mnbt.tag.*
import com.myna.mnbt.reflect.TypeCheckTool
import com.myna.mnbt.Tag
import com.myna.mnbt.reflect.MTypeToken

object TagConverters {



    val intTagConverter = newPrimitiveTagConverters(MTypeToken.of(Int::class.java)) {name,value->PrimitiveTag.IntTag(name, value)}
    val shortTagConverter = newPrimitiveTagConverters(MTypeToken.of(Short::class.java)) {name,value-> PrimitiveTag.ShortTag(name, value)}
    val byteTagConverter = newPrimitiveTagConverters(MTypeToken.of(Byte::class.java)) {name,value-> PrimitiveTag.ByteTag(name, value)}
    val longTagConverter = newPrimitiveTagConverters(MTypeToken.of(Long::class.java)) {name,value-> PrimitiveTag.LongTag(name, value)}
    val floatTagConverter = newPrimitiveTagConverters(MTypeToken.of(Float::class.java)) {name,value-> PrimitiveTag.FloatTag(name, value)}
    val doubleTagConverter = newPrimitiveTagConverters(MTypeToken.of(Double::class.java)) {name,value-> PrimitiveTag.DoubleTag(name, value)}
    val stringTagConverter = newPrimitiveTagConverters(MTypeToken.of(String::class.java)) {name,value-> PrimitiveTag.StringTag(name, value)}

    val byteArrayConverter = newPrimitiveArrayTagConverter(
            ByteArray::class.java) { name, arr -> ArrayTag.ByteArrayTag(name, arr) }
    val intArrayConverter = newPrimitiveArrayTagConverter(
            IntArray::class.java) { name, arr -> ArrayTag.IntArrayTag(name, arr) }
    val longArrayConverter = newPrimitiveArrayTagConverter(
            LongArray::class.java) { name, arr -> ArrayTag.LongArrayTag(name, arr) }

    val booleanConverter = BooleanConverter()



    class BooleanConverter:TagConverter<Byte> {
        override fun defaultToValueIntent(): ToValueIntent {
            return converterCallerIntent()
        }

        override fun defaultCreateTagIntent(): CreateTagIntent {
            return createTagUserIntent()
        }

        override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: CreateTagIntent): Tag<Byte>? {
            if (value is Boolean) {
                val byte = if (value) 1.toByte() else 0.toByte()
                return PrimitiveTag.ByteTag(name, byte)
            } else return null
        }

        override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ToValueIntent): Pair<String?, V>? {
            if (tag.value !is Byte) return null
            if (typeToken.type == Boolean::class.java || typeToken.type == java.lang.Boolean::class.java) {
                val value = tag.value != 0.toByte()
                return Pair(tag.name, value as V)
            } else return null
        }
    }


    /**
     * @param newTagFun a function that create required Tag
     */
    fun <NbtRelatedType:Any> newPrimitiveTagConverters
            (tagValueTypeToken: MTypeToken<NbtRelatedType>,
             newTagFun: (name: String?, value: NbtRelatedType) -> Tag<out NbtRelatedType>)
    : TagConverter<NbtRelatedType>
    = object: TagConverter<NbtRelatedType> {
        override fun defaultToValueIntent(): ToValueIntent {
            return converterCallerIntent()
        }

        override fun defaultCreateTagIntent(): CreateTagIntent {
            return object: CreateTagIntent {}
        }

        override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: CreateTagIntent): Tag<out NbtRelatedType>? {
            // check value can be Tag type T accepted or not
            if (!castable(typeToken)) return null
            // problem: need to handle primitive type with wrapper type while reflective cast not work
            // but can just cast directly lol
            return newTagFun(name, value as NbtRelatedType)
        }

        override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ToValueIntent): Pair<String?, V>? {
            if (!castable(typeToken)) return null
            return Pair(tag.name, tag.value as V)
        }

        private fun <V : Any> castable(typeToken: MTypeToken<out V>): Boolean =TypeCheckTool.isCastable(typeToken, tagValueTypeToken)

    }

    /**
     * @param newTagFun a function that create required Tag
     */
    // this fun handle create converter for primitive array tag
    @JvmStatic
    fun <T: Tag<ARR>, ARR:Any> newPrimitiveArrayTagConverter(tagValueClass: Class<ARR>, newTagFun:(name:String?, value: ARR)->T): TagConverter<ARR> {
        // check tagValueClass is Array type or not

        if (!tagValueClass.isArray) {
            throw IllegalArgumentException("Could not create Converter with no Array Tag Class! $tagValueClass")
        }
        val component = MTypeToken.of(tagValueClass.componentType)
        return object: TagConverter<ARR> {
            override fun defaultToValueIntent(): ToValueIntent {
                return converterCallerIntent()
            }

            override fun defaultCreateTagIntent(): CreateTagIntent {
                return createTagUserIntent()
            }

            override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: CreateTagIntent): T? {
                val arrComp = value::class.java.componentType?: return null
                if (!TypeCheckTool.isCastable(MTypeToken.of(arrComp), component)) return null
                val size = java.lang.reflect.Array.getLength(value)
                val arr = java.lang.reflect.Array.newInstance(component.rawType, size) as ARR
                val componentEq = arrComp==component.rawType
                // if array type totally equals, direct copy
                if (componentEq) System.arraycopy(value, 0, arr, 0, size)
                else {
                    // else copy element by element
                    for (i in IntRange(0, size-1)) {
                        val e = java.lang.reflect.Array.get(value, i)
                        java.lang.reflect.Array.set(arr, i, e)
                    }
                }
                return newTagFun(name, arr)
            }

            override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ToValueIntent): Pair<String?, V>? {
                // check tag is primitive array tag
                if (!tag.value::class.java.isArray) return null
                if (!TypeCheckTool.isCastable(component, MTypeToken.of(tag.value::class.java.componentType))) return null
                val valueComp = typeToken.componentType.rawType ?: return null
                if (!TypeCheckTool.isCastable(component, MTypeToken.of(valueComp))) return null

                val size = java.lang.reflect.Array.getLength(tag.value)
                val result = java.lang.reflect.Array.newInstance(valueComp, size)

                val componentEq = valueComp==component
                // if array type totally equals, direct copy
                if (componentEq) System.arraycopy(tag.value, 0, result, 0, size)
                else {
                    // else copy element by element
                    for (i in IntRange(0, size-1)) {
                        val e = java.lang.reflect.Array.get(tag.value, i)
                        java.lang.reflect.Array.set(result, i, e)
                    }
                }
                return Pair(tag.name, result as V)
            }
        }
    }
}

