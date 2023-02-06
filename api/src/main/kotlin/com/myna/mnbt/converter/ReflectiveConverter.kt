package com.myna.mnbt.converter

import com.myna.mnbt.IdTagCompound
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag
import com.myna.mnbt.Tag
import com.myna.mnbt.annotations.LocateAt
import com.myna.mnbt.converter.meta.NbtPathTool
import com.myna.mnbt.converter.meta.TagLocatorInstance
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.reflect.ObjectInstanceHandler
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.NullPointerException
import java.lang.RuntimeException
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

    data class CreateSubTagArgs(
            val value:Any, val subTagName:String,
            val createSubTagIntent: CreateTagIntent,
            val fieldTypeToken: MTypeToken<out Any>, val subTagTargetId:Byte?,
            val fieldRelatePath: List<String>
    )

    data class CleanUpStartTag(val pointer:CompoundTag, val targetName:String)

    // Conversion to CompoundTag procedure:
    //      create rootTag with name parameter passed in
    //      create dataEntryTag
    //      for each subTag, create subTagDataEntryTag
    //      create subTag, add it to subTagDataEntryTag
    //
    //      the compound tag structure is root=>dataEntryTag=>subTagDataEntryTag->subTag
    //          (or root->...->dataEntryTag->...->subTagDataEntryTag->subTag)
    //      (=> means directly/indirectly contains, -> means directly contains)
    // TODO: refact temporal design that name is null will rebuild TagLocator

    // TODO: throw appropriate exception when [LocateAt] specify wrong id
    //  always forgetting add id value in @LocateAt annotation, find better way solve it
    override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: ConverterCallerIntent): Tag<AnyCompound>? {
        // parameters check
        val callerIntent = intent as RecordParents
        val valueClass = value::class.java
        if (isExcluded(valueClass)) return null


        var cleanUpStart:CleanUpStartTag? = null

        // create root tag and get TagLocator intent
        val rootContainerPath = if (intent is ParentTagsInfo) intent.rootContainerPath else "mnbt://"
        val rootPath = if (name != null) NbtPathTool.appendSubDir(rootContainerPath, name) else "mnbt://#/"
        // get tag locator, else build a new one
        val (root,tagLocatorIntent) = if (name != null && intent is TagLocator) {
            // TODO: findAt return check, rootContainer[name] tag type check
            val rootContainer = NbtPathTool.findTag(intent.root, rootContainerPath, IdTagCompound) as CompoundTag
            val gotRoot = (rootContainer[name]?:CompoundTag(name)) as CompoundTag
            if (rootContainer[name]==null) {
                rootContainer.add(gotRoot)
                cleanUpStart = CleanUpStartTag(rootContainer, gotRoot.name!!)
            }
            Pair(gotRoot, intent)
        } else {
            val newRoot = CompoundTag(name)
            val newIntent = TagLocatorInstance(newRoot)
            Pair(newRoot, newIntent)
        }

        // dataEntry is the tag stores the result of value conversion (object conversion)
        // if no extra tag insert(no redirect path), dataEntry == root,
        // else dataEntry is the last compound tag presented in NbtPath.getClassExtraPath
        // temporal: if name is null, set it as '#'
        val mapToAnn = typeToken.rawType.getAnnotation(LocateAt::class.java)
        val dataEntryAbsPath:String =  if ( mapToAnn != null) {
            // type id not match
            if (mapToAnn.typeId != IdTagCompound) throw IllegalArgumentException()
            NbtPathTool.combine(rootPath, mapToAnn.path)
        } else rootPath


        val extraEntryLink = if (mapToAnn!=null) {
            NbtPathTool.toAccessSequence(mapToAnn.path)
        } else null

        // build to data Entry tag

        val dataEntryTag = extraEntryLink?.let {
            it.fold(root) { parent,childName->
                val child = (parent[childName]?: CompoundTag(childName)) as CompoundTag
                if (parent[childName]==null) {
                    if (cleanUpStart==null) {
                        cleanUpStart = CleanUpStartTag(parent, child.name!!)
                    }
                    parent.add(child)
                }
                child
            }
        }?: root

        val fields = ObjectInstanceHandler.getAllFields(value::class.java)

        val fieldsInfo = fields.map { field ->
            val locateAtAnn = field.getAnnotation(LocateAt::class.java)
            if (locateAtAnn!=null) {
                val accessSeq = NbtPathTool.toAccessSequence(locateAtAnn.path).toList()
                Triple(field, accessSeq, locateAtAnn.typeId)
            } else {
                Triple(field, listOf(field.name), null)
            }
        }

        val createSubTagsArgs = fieldsInfo.mapNotNull { (field,accessSequence,targetId)->
            val fieldTagContainerPath = getSubTagContainerPath(dataEntryAbsPath, accessSequence)
            val fieldIntent = buildSubTagCreationIntent(fieldTagContainerPath, callerIntent, tagLocatorIntent)
            createSubTagArgs(field, value, accessSequence, targetId, fieldIntent)
        }


        try {
            createSubTagsArgs.onEach {
                val subTag = proxy.createTag(it.subTagName, it.value, it.fieldTypeToken, it.createSubTagIntent) //?:return@onEach
                val creationSuccess = subTag!=null && !(it.subTagTargetId!=null && subTag.id!=it.subTagTargetId)
                if (!creationSuccess) return@onEach
                // build sub tree contains subtag
                val subTagContainer = it.fieldRelatePath.dropLast(1).fold(dataEntryTag) { parent, childName->
                    val child = (parent[childName]?: CompoundTag(childName)) as CompoundTag
                    if (parent[childName]==null) {
                        parent.add(child)
                    }
                    child
                }
                subTagContainer.add(subTag!!)
            }
            return root
        } catch(e:Exception) {
            if (outputDebugInfo) e.printStackTrace()
            // clean field container tag
            cleanUpStart?.let {
                it.pointer.value.remove(it.targetName)
            }
            return null
        }
    }

    private fun createSubTagArgs(
            field: Field, value:Any,
            fieldRelatedPath:List<String>, targetTagId:Byte?,
            createSubTagIntent:CreateTagIntent
    ):CreateSubTagArgs? {
        try {
            val accessible = field.trySetAccessible()
            if (!accessible) return null // if try set Accessible return false, it may be a static final member
            val fieldValue = field.get(value) ?: return null // actual value is null, skip this field

            val subTagName = if (fieldRelatedPath.isNotEmpty()) fieldRelatedPath.last() else field.name
            val fieldTk = MTypeToken.of(field.genericType) as MTypeToken<out Any>
            return CreateSubTagArgs(fieldValue, subTagName, createSubTagIntent, fieldTk, targetTagId, fieldRelatedPath)
        } catch (e:Exception) { when (e) {
            is SecurityException,
            is IllegalAccessException,
            is IllegalArgumentException,
            is NullPointerException -> return null
            else -> throw e
        } }
    }

    private fun getSubTagContainerPath(dataEntryAbsPath:String, fieldPath: List<String>?):String {
        // get field's container tag absolute path
        // if fieldsPath is not null, combine related path with data entry path
        return fieldPath?.let { arrTypePath->
            // remove last one in array, because that is target field tag which should let proxy create it
            val relatedPath = NbtPathTool.toRelatedPath(*arrTypePath.dropLast(1).toTypedArray())
            NbtPathTool.combine(dataEntryAbsPath, relatedPath)
        }?: dataEntryAbsPath
    }

    private fun buildSubTagCreationIntent(fieldTagContainerPath:String, createTagIntent: RecordParents, tagLocator: TagLocator):CreateTagIntent {
        return object: RecordParents, ParentTagsInfo, TagLocator by tagLocator {
            override val parents: Deque<Any> = createTagIntent.parents
            override val rootContainerPath = fieldTagContainerPath
        }
    }

    // core idea: first find constructors
    // try deserialize them with field related class as typeToken
    // if one of return is null, means total conversion failed, final result will become null
    // if can not construct instance, return null
    override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ConverterCallerIntent): Pair<String?, V>? {
        // parameter check
        intent as RecordParents; intent as ToValueIntent
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
        val mapToAnn = typeToken.rawType.getAnnotation(LocateAt::class.java)
        val classPath: String?
        val fieldsId:Map<Field, Byte>? = null
        if ( mapToAnn != null) {
            // type id not match
            if (mapToAnn.typeId != IdTagCompound) throw IllegalArgumentException()
            classPath = NbtPathTool.combine("mnbt://", NbtPathTool.format(mapToAnn.path))
        } else classPath = "mnbt://${tag.name?:"#"}"

        try {
            fields.associateWith { field-> // build field path
                val fieldLinkToAnn = field.getAnnotation(LocateAt::class.java)
                val fieldPath = when {
                    fieldLinkToAnn != null -> {
                        NbtPathTool.combine(classPath, NbtPathTool.format(fieldLinkToAnn.path))
                    }
                    else -> NbtPathTool.appendSubDir(classPath, field.name)
                }
                fieldPath
            }.mapValues {
                val field = it.key
                val fieldPath = it.value
                val accessible = field.trySetAccessible()
                // if can not access field, and also require non nullable properties, return null
                if (!accessible && !returnObjectWithNullableProperties) return null

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
     * @param fieldPath
     */
    private fun handleField(sourceTag:Tag<out Any>, field: Field, intent: ToValueIntent, fieldPath:String?, fieldTypeId:Byte?):Pair<String?, Any>? {

        // TODO: null check is a kind of mess
        val targetTag = if (fieldPath!=null) {
            val accessQueue = NbtPathTool.toAccessSequence(fieldPath)
            NbtPathTool.findTag(sourceTag, accessQueue, fieldTypeId)
                    ?: return null
                    //?: throw ConversionException("Can not find matched Tag with name:$fieldPath and value type:$fieldTypeId")
        } else {
            (sourceTag.value as Map<String, Tag<out Any>>)[field.name]
        }
        if (targetTag==null) return null

        val fieldTypeToken = MTypeToken.of(field.genericType)
        return proxy.toValue(targetTag, fieldTypeToken, nestCIntent(intent, false))
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