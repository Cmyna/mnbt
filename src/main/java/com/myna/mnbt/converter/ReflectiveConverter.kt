package com.myna.mnbt.converter

import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag
import com.myna.mnbt.Tag
import com.myna.mnbt.annotations.MapTo
import com.myna.mnbt.exceptions.ConversionException
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.reflect.ObjectInstanceHandler
import com.myna.mnbt.tag.TagSearcher
import java.lang.Exception
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * this Converter is used for some simple POJO class,
 */
class ReflectiveConverter(override var proxy: TagConverter<Any, ConverterCallerIntent>): HierarchicalTagConverter<AnyCompound>() {

    private val typeBlackList = ArrayList<Class<*>>()

    init {
        typeBlackList.addAll(listOf(
                Iterable::class.java,
                Map::class.java,
        ))
    }

    /**
     * set reflective converter handle object with nullable properties or not.
     *
     * if value is true, object return from toValue will have some null members if converter can not handle it
     * (can not converted to field expected type or can not access field)
     */
    var returnObjectWithNullableProperties = true

    var onlyJavaBean:Boolean = false

    var outputDebugInfo:Boolean = false

    override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: ConverterCallerIntent): Tag<AnyCompound>? {
        intent as RecordParents
        if (isExcluded(value::class.java)) return null
        try {
            val fields = ObjectInstanceHandler.getAllFields(value::class.java)
            val compTag = CompoundTag(name)
            fields.onEach { field-> // try set field accessible
                val accessible = field.trySetAccessible()
                if (!accessible) return@onEach // if try set Accessible return false, it may be a static final member
                val fieldTk = MTypeToken.of(field.genericType) as MTypeToken<out Any>
                val actualValue = field.get(value) ?: return@onEach // actual value is null, skip this field
                // try let proxy handle sub tag, if null then ignore this field
                val subTag = proxy.createTag(field.name, actualValue, fieldTk, nestCIntent(intent.parents, false))
                        ?:return@onEach
                compTag.add(subTag)
            }
            return compTag
        }
        catch(e:Exception) {
            if (outputDebugInfo) e.printStackTrace()
            return null
        }

    }

    override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ConverterCallerIntent): Pair<String?, V>? {
        intent as RecordParents
        val tagValue = tag.value
        if (tagValue !is Collection<*>) return null
        // check typeToken declare is a actual class, not an interface or abstract class
        val declaredRawType = typeToken.rawType
        if (isExcluded(declaredRawType)) return null
        if (declaredRawType == Any::class.java) return null // ignore top bounds:Object, because generic type V's upperbounds in all toValue function is Any
        if (declaredRawType.isInterface && Modifier.isAbstract(declaredRawType.modifiers)) return null
        // core idea: first find constructors
        // try deserialize them with field related class as typeToken
        // if one of return is null, means total conversion failed, final result will become null
        // if can not construct instance, return null
        val instance = ObjectInstanceHandler.newInstance(declaredRawType as Class<V>, onlyJavaBean)?: return null
        val fields = ObjectInstanceHandler.getAllFields(declaredRawType)
        try {
            fields.associateBy { field->
                val accessible = field.trySetAccessible()
                if (!accessible && !returnObjectWithNullableProperties) return null // if can not access field, and also require non nullable properties, return null
                field
            }
                    .mapValues { entry ->
                        val value = handleField(tag, entry.key, intent)
                        // if null and require non nullable properties, it means failed conversion, return null from 'toValue' function
                        if (!returnObjectWithNullableProperties && value==null) return null
                        else if (value == null) return@mapValues null // if nullable properties, then set value to null
                        value.second
                    }.onEach { entry-> // set field into instance
                        if (entry.value==null) return@onEach
                        val field = entry.key
                        val accessible = field.canAccess(instance)
                        val value = entry.value
                        if (accessible) field.set(instance, value)
                    }
        } catch (exc:Exception) {
            if (outputDebugInfo) exc.printStackTrace()
            return null
        }
        return Pair(tag.name, instance)
    }

    private fun handleField(sourceTag:Tag<out Any>, field: Field, intent: RecordParents):Pair<String?, Any>? {
        // TODO: can not handle well with collection(may have tag with same name)
        //  duplicate code: findTag(which is also used in proxy)
        val fieldName = field.name
        // check field annotation
        val mapToAnn = field.getAnnotation(MapTo::class.java)

        if (mapToAnn != null) {
            val regex = Regex("^(\\S|\\s)+\\.")
        }

        // go to path target tag
        val startedTag = if (mapToAnn!=null) {
            TagSearcher.findTag(sourceTag, mapToAnn.path, mapToAnn.typeId) ?: throw ConversionException(
                    "Can not find matched Tag with name:${mapToAnn.path} and value type:${mapToAnn.typeId}"
            )
        } else {
            (sourceTag.value as Collection<Tag<out Any>>).find {tag->tag.name==fieldName}
        }
        if (startedTag==null) return null

        val fieldTypeToken = MTypeToken.of(field.genericType)
        return proxy.toValue(startedTag, fieldTypeToken, nestCIntent(intent.parents, false))
    }

    /**
     * determine that converter not handle these value types in createTag/toValue
     */
    private fun isExcluded(type:Class<*>):Boolean {
        return typeBlackList.any { black->
            black.isAssignableFrom(type)
        }
    }

    /**
     * add type that want reflective converter ignores(return null) in createTag/toValue
     */
    fun addExcludedType(type:Class<*>) {
        if (!isExcluded(type)) this.typeBlackList.add(type)
    }
}