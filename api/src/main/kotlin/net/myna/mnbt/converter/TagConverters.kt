package net.myna.mnbt.converter

import net.myna.mnbt.tag.*
import net.myna.mnbt.reflect.TypeCheckTool
import net.myna.mnbt.Tag
import net.myna.mnbt.reflect.MTypeToken

object TagConverters {

    val intTagConverter = PrimitiveTagConverter(MTypeToken.of(Int::class.java)) { name, value->IntTag(name, value)}
    val shortTagConverter = PrimitiveTagConverter(MTypeToken.of(Short::class.java)) { name, value-> ShortTag(name, value)}
    val byteTagConverter = PrimitiveTagConverter(MTypeToken.of(Byte::class.java)) { name, value-> ByteTag(name, value)}
    val longTagConverter = PrimitiveTagConverter(MTypeToken.of(Long::class.java)) { name, value-> LongTag(name, value)}
    val floatTagConverter = PrimitiveTagConverter(MTypeToken.of(Float::class.java)) { name, value-> FloatTag(name, value)}
    val doubleTagConverter = PrimitiveTagConverter(MTypeToken.of(Double::class.java)) { name, value-> DoubleTag(name, value)}
    val stringTagConverter = PrimitiveTagConverter(MTypeToken.of(String::class.java)) { name, value-> StringTag(name, value)}

    val byteArrayConverter = PrimitiveArrayTagConverter(
            ByteArray::class.java) { name, arr -> ByteArrayTag(name, arr) }
    val intArrayConverter = PrimitiveArrayTagConverter(
            IntArray::class.java) { name, arr -> IntArrayTag(name, arr) }
    val longArrayConverter = PrimitiveArrayTagConverter(
            LongArray::class.java) { name, arr -> LongArrayTag(name, arr) }

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
                return ByteTag(name, byte)
            } else return null
        }

        @Suppress("UNCHECKED_CAST")
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
    class PrimitiveTagConverter<NbtRelatedType:Any>
            (private val tagValueTypeToken: MTypeToken<NbtRelatedType>,
             val newTagFun: (name: String?, value: NbtRelatedType) -> Tag<out NbtRelatedType>)
    : TagConverter<NbtRelatedType> {
        override fun defaultToValueIntent(): ToValueIntent {
            return converterCallerIntent()
        }

        override fun defaultCreateTagIntent(): CreateTagIntent {
            return object: CreateTagIntent {}
        }

        @Suppress("UNCHECKED_CAST")
        override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: CreateTagIntent): Tag<out NbtRelatedType>? {
            // check value can be Tag type T accepted or not
            if (!castable(typeToken)) return null
            // problem: need to handle primitive type with wrapper type while reflective cast not work
            // but can just cast directly lol
            return newTagFun(name, value as NbtRelatedType)
        }

        @Suppress("UNCHECKED_CAST")
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
    class PrimitiveArrayTagConverter<T: Tag<ARR>, ARR:Any>(tagValueClass: Class<ARR>, private val newTagFun:(name:String?, value: ARR)->T): TagConverter<ARR> {

        private val component: MTypeToken<out Any>

        init {
            // check tagValueClass is Array type or not
            if (!tagValueClass.isArray) {
                throw IllegalArgumentException("Could not create Converter with no Array Tag Class! $tagValueClass")
            }
            component = MTypeToken.of(tagValueClass.componentType)
        }

        override fun defaultToValueIntent(): ToValueIntent {
            return converterCallerIntent()
        }

        override fun defaultCreateTagIntent(): CreateTagIntent {
            return createTagUserIntent()
        }

        @Suppress("UNCHECKED_CAST")
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

        @Suppress("UNCHECKED_CAST")
        override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ToValueIntent): Pair<String?, V>? {
            // check tag is primitive array tag
            if (!tag.value::class.java.isArray) return null
            if (!TypeCheckTool.isCastable(component, MTypeToken.of(tag.value::class.java.componentType))) return null
            val valueComp = typeToken.componentType.rawType ?: return null
            if (!TypeCheckTool.isCastable(component, MTypeToken.of(valueComp))) return null

            val size = java.lang.reflect.Array.getLength(tag.value)
            val result = java.lang.reflect.Array.newInstance(valueComp, size)

            val componentEq = valueComp==component.rawType
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

