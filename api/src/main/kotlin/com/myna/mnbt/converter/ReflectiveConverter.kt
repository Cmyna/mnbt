package com.myna.mnbt.converter

import com.myna.mnbt.IdTagCompound
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag
import com.myna.mnbt.Tag
import com.myna.mnbt.annotations.MapTo
import com.myna.mnbt.converter.meta.NbtPath
import com.myna.mnbt.converter.meta.TagLocatorInstance
import com.myna.mnbt.exceptions.ConversionException
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.reflect.ObjectInstanceHandler
import com.myna.mnbt.tag.TagSearcher
import java.lang.Exception
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import kotlin.collections.ArrayList

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
        val callerIntent = intent as RecordParents
        val valueClass = value::class.java
        if (isExcluded(valueClass)) return null

        val rootPath = if (intent is ParentTagsInfo) intent.getRootPath() else "mnbt://$name"
        try {
            val root = CompoundTag(name) // root is the final output

            // get tag locator, else build a new one
            val tagLocatorIntent = if (intent is TagLocator) {
                val rootParentPath = rootPath.substring(0, rootPath.length-name!!.length)
                intent.linkTagAt(rootParentPath, root)
                intent
            } else {
                TagLocatorInstance(root)
            }

            // dataEntry is the tag stores the result of value conversion (object conversion)
            // if no extra tag insert(no redirect path), dataEntry == root, else dataEntry is the tag contain link end
            var fieldsPath:Map<Field, Array<String>>? = null

            var dataEntryAbsPath = rootPath // class data entry absolute path

            if (value is NbtPath) { // check value class implements NbtPath or not
                // construct target by nbt path
                val classExtraPath = value.getClassExtraPath()
                fieldsPath = value.getFieldsPaths()
                if (classExtraPath!=null && classExtraPath.isNotEmpty()) {
                    dataEntryAbsPath = tagLocatorIntent.toRelatedPath(*classExtraPath).let {
                        tagLocatorIntent.combine(rootPath, it)
                    }
                    val compounds = arrayOfNulls<CompoundTag>(classExtraPath.size)
                    //buildRootToDataEntry(classMapPath, compounds as Array<Tag<out Any>?>, tagLocatorIntent, rootPath)
                    buildRootToDataEntry(classExtraPath, compounds, tagLocatorIntent, rootPath)
                }
            }

            val fields = ObjectInstanceHandler.getAllFields(value::class.java)
            fields.onEach { field-> // try set field accessible
                val accessible = field.trySetAccessible()
                if (!accessible) return@onEach // if try set Accessible return false, it may be a static final member
                val fieldTk = MTypeToken.of(field.genericType) as MTypeToken<out Any>
                val actualValue = field.get(value) ?: return@onEach // actual value is null, skip this field

                // get field's parent absolute tag path
                val fieldParentsAbsPath = fieldsPath?.get(field)?.let { arrTypePath->
                    // if fieldsPath is not null, combine related path with data entry path
                    // remove last one in array, because that is target field tag
                    val relatedPath = tagLocatorIntent.toRelatedPath(*arrTypePath.copyOfRange(0, arrTypePath.size-1))
                    tagLocatorIntent.combine(dataEntryAbsPath, relatedPath)
                }?: dataEntryAbsPath

                // build compounds with field's parent tag absolute path
                val parentTag = tagLocatorIntent.buildPath(fieldParentsAbsPath) as CompoundTag

                // get actual name to to field tag (try get path last tag name, else use field name)
                val fieldTagName:String = fieldsPath?.get(field)?.let {
                    if (it.isEmpty()) null
                    else it.last()
                }?: field.name

                val fieldIntent = object: RecordParents, ToValueTypeToken, ParentTagsInfo, TagLocator by tagLocatorIntent {
                    override val parents: Deque<Any> = callerIntent.parents
                    override val ignore: Boolean = false
                    override fun getRootPath(): String {
                        return "$fieldParentsAbsPath/$fieldTagName"
                    }
                }

                // try let proxy handle sub tag, if null then ignore this field
                val subTag = proxy.createTag(fieldTagName, actualValue, fieldTk, fieldIntent)
                        ?:return@onEach

                parentTag.add(subTag)
            }

            // if value is NbtPath (so root will not be null), return path root tag
            return root
        }
        catch(e:Exception) {
            if (outputDebugInfo) e.printStackTrace()
            return null
        }

    }

    override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ConverterCallerIntent): Pair<String?, V>? {
        // core idea: first find constructors
        // try deserialize them with field related class as typeToken
        // if one of return is null, means total conversion failed, final result will become null
        // if can not construct instance, return null
        intent as RecordParents
        val tagValue = tag.value
        if (tagValue !is Map<*,*>) return null
        val declaredRawType = typeToken.rawType
        if (isExcluded(declaredRawType)) return null
        if (declaredRawType == Any::class.java) return null // ignore top bounds:Object, because generic type V's upperbounds in all toValue function is Any
        // check typeToken declare is a actual class, not an interface or abstract class
        if (declaredRawType.isInterface && Modifier.isAbstract(declaredRawType.modifiers)) return null

        val instance = ObjectInstanceHandler.newInstance(declaredRawType as Class<V>, onlyJavaBean)?: return null
        val fields = ObjectInstanceHandler.getAllFields(declaredRawType)

        // try get NbtPath implementation
        var classMapPath:Array<String>? = null
        var fieldsPath:Map<Field, Array<String>>? = null
        var fieldsId:Map<Field, Byte>? = null
        if (instance is NbtPath) {
            classMapPath = instance.getClassExtraPath()
            fieldsPath = instance.getFieldsPaths()
            fieldsId = instance.getFieldsTagType()
        }

        try {
            fields.associateBy { field->
                val accessible = field.trySetAccessible()
                if (!accessible && !returnObjectWithNullableProperties) return null // if can not access field, and also require non nullable properties, return null
                field
            }
                    .mapValues { entry ->
                        val field = entry.key
                        // build field path
                        val fieldPath = fieldsPath?.get(field)?.let {
                            val arr = classMapPath!!.copyOf(classMapPath.size+it.size)
                            System.arraycopy(it, 0, arr, classMapPath.size, it.size)
                            arr as Array<String>
                        }

                        val value = handleField(tag, field, intent, fieldPath, fieldsId?.get(field))
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

    /**
     * @param instance: new instance from toValue, if instance implements NbtPath, then pass in, else null
     */
    private fun handleField(sourceTag:Tag<out Any>, field: Field, intent: RecordParents, fieldPath:Array<String>?, fieldTypeId:Byte?):Pair<String?, Any>? {
        // TODO: can not handle well with collection(may have tag with same name)
        //  duplicate code: findTag(which is also used in proxy)
        // check field annotation
        val mapToAnn = field.getAnnotation(MapTo::class.java)

        val targetTag = if (fieldPath!=null) {
            val tag = TagSearcher.findTag(sourceTag, fieldPath.asIterable(), fieldTypeId!!)
            if (tag==null) {
                val pathStr = fieldPath.fold(".") { last,cur -> "$last/$cur"}
                throw ConversionException("Can not find matched Tag with name:$pathStr and nbt container type:${IdTagCompound}")
            }
            tag
        } else if (mapToAnn!=null) {
            val accessQueue = TagSearcher.toAccessQueue(mapToAnn.path)
            TagSearcher.findTag(sourceTag, accessQueue, mapToAnn.typeId) ?:
                throw ConversionException("Can not find matched Tag with name:${mapToAnn.path} and value type:${mapToAnn.typeId}")
        } else {
            (sourceTag.value as Map<String, Tag<out Any>>)[field.name]
        }
        if (targetTag==null) return null

        val fieldTypeToken = MTypeToken.of(field.genericType)
        return proxy.toValue(targetTag, fieldTypeToken, nestCIntent(intent.parents, false))
    }

    /**
     * determine that converter not handle these value types in createTag/toValue
     */
    private fun isExcluded(type:Class<*>):Boolean {
        return typeBlackList.any { black->
            black.isAssignableFrom(type)
        }
    }

    private fun buildRootToDataEntry(path: Array<String>, link:Array<CompoundTag?>, locator:TagLocator, rootPath: String) {
        var pointer = 0
        // construct remain
        var last:CompoundTag = locator.findAt(rootPath, IdTagCompound) as CompoundTag
        while (pointer < link.size) {
            val subPath = path.copyOfRange(0, pointer+1)
            val tagRelatedPath = locator.toRelatedPath(*subPath)
            val tagAbsPath = locator.combine(rootPath, tagRelatedPath)
            val tag = locator.findAt(tagAbsPath, IdTagCompound)?: CompoundTag(path[pointer])
            locator.linkTagAt(tagAbsPath, tag)
            link[pointer] = tag as CompoundTag
            last.add(tag)
            last = tag
            pointer += 1
        }
    }

    /**
     * pointer should only be used for helper function [buildFieldTagContainersHelper]
     */
    data class FieldPath(val field:Field, val path:Array<String>, var pointer: Int)
    data class FieldCompounds(val compounds: Array<CompoundTag?>)


    private fun buildFieldTagContainers(paths:Map<Field, Array<String>>, locator: TagLocator):Map<Field, Array<CompoundTag>> {
        val popLast = paths.mapValues {
            // remove bottom, because bottom is field target tag, should let proxy handle it
            if (it.value.isNotEmpty()) it.value.copyOfRange(0, it.value.size-1)
            else it.value
        }
        val pathsForHelper = popLast.map {
            FieldPath(it.key, it.value, 0)
        }
        val helperBuild = popLast.mapValues {
            FieldCompounds(arrayOfNulls(it.value.size))
        }
        buildFieldTagContainersHelper(pathsForHelper, helperBuild, locator)

        return helperBuild.mapValues {
            if (it.value.compounds.isEmpty()) return@mapValues it.value.compounds
            it.value.compounds.reduce { pre, cur ->
                // build contain chain
                if (pre!!.value[cur!!.name] == null) pre.add(cur)
                cur
            }
            it.value.compounds
        } as Map<Field, Array<CompoundTag>>
    }

    private fun buildFieldTagContainersHelper(paths:List<FieldPath>, helperBuild:Map<Field, FieldCompounds>, locator: TagLocator) {
        paths.groupBy {
            // group fields by their current top package name (at pointer)
            // if pointer over path size, group to null(no package handle)
            if (it.pointer < it.path.size) it.path[it.pointer]
            else null
        }.onEach {
            // if group is null, do nothing
            if (it.key==null) return@onEach
            // top package compound
            val compound = CompoundTag(it.key)
            it.value.onEach { fp ->
                val fieldResult = helperBuild[fp.field]!!
                // set current top package to result array
                fieldResult.compounds[fp.pointer] = compound

                // move pointer to sub path of current top package path
                fp.pointer += 1
            }

            // build sub path
            buildFieldTagContainersHelper(it.value, helperBuild, locator)
        }
    }

    /**
     * add type that want reflective converter ignores(return null) in createTag/toValue
     */
    fun addExcludedType(type:Class<*>) {
        if (!isExcluded(type)) this.typeBlackList.add(type)
    }
}