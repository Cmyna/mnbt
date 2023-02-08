package com.myna.mnbt.converter

import com.myna.mnbt.IdTagCompound
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag
import com.myna.mnbt.Tag
import com.myna.mnbt.annotations.LocateAt
import com.myna.mnbt.converter.meta.NbtPathTool
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.reflect.ObjectInstanceHandler
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.NullPointerException
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import kotlin.collections.ArrayList

/**
 * this Converter is used for some simple POJO class,
 */
class ReflectiveConverter(override var proxy: TagConverter<Any>): HierarchicalTagConverter<AnyCompound>() {

    // Conversion to CompoundTag procedure:
    //      create returnedTag with name parameter passed in
    //      create/get dataEntryTag
    //      for each subTag, create/get subTagDataEntryTag
    //      create subTag, add it to subTagDataEntryTag
    //
    //      the compound tag structure is root=>dataEntryTag=>subTagDataEntryTag->subTag
    //          (or root->...->dataEntryTag->...->subTagDataEntryTag->subTag)
    //      (=> means directly/indirectly contains, -> means directly contains)
    override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: CreateTagIntent): Tag<AnyCompound>? {
        // parameters check
        val valueClass = value::class.java
        if (isExcluded(valueClass)) return null

        // get tag locator, else build a new one
        val (beReturnedTag, builtRoot, returnedTagPath) = if (intent is NbtTreeInfo) {
            val got = NbtPathTool.goto(intent.root, intent.createdTagRelatePath, IdTagCompound)?: CompoundTag(name)
            got as Tag<AnyCompound>
            Triple(got, intent.root, intent.createdTagRelatePath)
        } else {
            val newReturnedTag = CompoundTag(name)
            val pathString = "./"
            Triple(newReturnedTag, newReturnedTag, pathString)
        }

        // dataEntry is the tag stores the result of value conversion (object conversion)
        // if no extra tag insert(no redirect path), dataEntry == root,
        // else dataEntry is the last compound tag presented in NbtPath.getClassExtraPath
        val mapToAnn = typeToken.rawType.getAnnotation(LocateAt::class.java)
        val dataEntryRelatePath:String =  if ( mapToAnn != null) {
            NbtPathTool.combine(returnedTagPath, mapToAnn.encodePath)
        } else returnedTagPath


        val toDataEntrySequence = if (mapToAnn!=null) {
            NbtPathTool.toAccessSequence(mapToAnn.encodePath)
        } else sequenceOf()

        // build to data Entry tag
        val dataEntryTag = toDataEntrySequence.fold(beReturnedTag) { parent, childName->
            if (childName.first() == '#') {
                val pathInfo = toDataEntrySequence.toList().fold(".") { last, cur-> "$last/$cur"}
                throw IllegalArgumentException("Converter is trying to create extra tags to actual data entry by an related path $pathInfo, " +
                        "but got index format path segment $childName, which the Converter can not handle. ")
            }
            val parentValue = parent.value
            val child = (parentValue[childName]?: CompoundTag(childName)) as CompoundTag
            parentValue[childName] = child
            child
        }

        val fields = ObjectInstanceHandler.getAllFields(value::class.java)

        val fieldsInfo = fields.map { field ->
            val locateAtAnn = field.getAnnotation(LocateAt::class.java)
            if (locateAtAnn!=null) {
                val accessSeq = NbtPathTool.toAccessSequence(locateAtAnn.encodePath).toList()
                Pair(field, accessSeq)
            } else {
                Pair(field, listOf(field.name))
            }
        }

        val createSubTagsArgs = fieldsInfo.mapNotNull { (field,accessSequence)->
            val fieldTagPath = getSubTagPath(dataEntryRelatePath, accessSequence)
            val fieldIntent = buildSubTagCreationIntent(fieldTagPath, intent, builtRoot)
            val idTODO:Byte = 0
            createSubTagArgs(field, value, accessSequence, idTODO, fieldIntent)
        }


        try {
            createSubTagsArgs.onEach {
                val subTag = proxy.createTag(it.subTagName, it.value, it.fieldTypeToken, it.createSubTagIntent) //?:return@onEach
                val creationSuccess = subTag!=null
                if (!creationSuccess) return@onEach
                // build sub tree contains subtag
                val subTagContainer = it.fieldRelatePath.dropLast(1).fold(dataEntryTag) { parent, childName->
                    val parentValue = parent.value
                    val child = (parentValue[childName]?: CompoundTag(childName)) as CompoundTag
                    parentValue[childName] = child
                    child
                }
                subTagContainer.value[it.subTagName] = subTag!!
            }
            return beReturnedTag
        } catch(e:Exception) {
            if (throwExceptionInCreateTag) throw e
            else if (printStacktrace) e.printStackTrace()
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

    private fun getSubTagPath(dataEntryRelatePath: String, fieldPath: List<String>):String {
        return fieldPath.let { arrTypePath ->
            val relatedPath = NbtPathTool.toRelatedPath(*arrTypePath.toTypedArray())
            NbtPathTool.combine(dataEntryRelatePath, relatedPath)
        }
    }

    private fun buildSubTagCreationIntent(
            fieldTagPath:String, callerIntent: CreateTagIntent,
            buildRoot:Tag<out Any>):CreateTagIntent {
        return object: CreateTagIntent, RecordParents, NbtTreeInfo {
            override val createdTagRelatePath: String = fieldTagPath
            override val root: Tag<out Any> = buildRoot
            override val parents: Deque<Any> = if (callerIntent is RecordParents) callerIntent.parents else ArrayDeque()
        }
    }

    // core idea: first find constructors
    // try deserialize them with field related class as typeToken
    // if one of return is null, means total conversion failed, final result will become null
    // if can not construct instance, return null
    override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ToValueIntent): Pair<String?, V>? {
        // parameter check
        intent as RecordParents; intent as ToValueIntent
        if (tag.value !is Map<*,*>) return null
        val declaredRawType = typeToken.rawType
        if (isExcluded(declaredRawType)) return null
        if (declaredRawType == Any::class.java) return null // ignore top bounds:Object, because generic type V's upperbounds in all toValue function is Any
        // check typeToken declare is a actual class, not an interface or abstract class
        if (declaredRawType.isInterface && Modifier.isAbstract(declaredRawType.modifiers)) return null

        val instance = ObjectInstanceHandler.newInstance(declaredRawType as Class<V>, onlyJavaBean)?: return null
        val fields = ObjectInstanceHandler.getAllFields(declaredRawType)

        // try get NbtPath implementation
        val locateAtAnn = typeToken.rawType.getAnnotation(LocateAt::class.java)

        val dataEntryTag = if (locateAtAnn != null) {
            // try get data entry tag from annotation LocateAt
            val found = NbtPathTool.goto(tag, NbtPathTool.format(LocateAt.getDecodePath(locateAtAnn)))
            if (found!=null && found.id== IdTagCompound) found else tag
        } else tag
        dataEntryTag as Tag<AnyCompound>

        val fieldsWithAccessSeq = fields.associateWith { field ->
            val fieldLinkToAnn = field.getAnnotation(LocateAt::class.java)
            if (fieldLinkToAnn != null) {
                NbtPathTool.toAccessSequence(NbtPathTool.format(LocateAt.getDecodePath(fieldLinkToAnn)))
            } else sequenceOf(field.name)
        }

        try {
            fieldsWithAccessSeq.mapValues {
                val field = it.key
                val accessible = field.trySetAccessible()
                // if can not access field, and also require non nullable properties, return null
                if (!accessible) return@mapValues null
                val fieldTypeToken = MTypeToken.of(field.genericType)

                val targetTag = NbtPathTool.findTag(dataEntryTag, it.value)?: return@mapValues null
                val value = proxy.toValue(targetTag, fieldTypeToken, nestCIntent(intent, false))

                value?.second
            }.onEach { entry-> // set field into instance
                if (entry.value==null) {
                    // TODO options check refact
                    if (!returnObjectWithNullableProperties) return null
                    else return@onEach
                }
                val field = entry.key
                val accessible = field.canAccess(instance)
                val value = entry.value
                if (accessible) field.set(instance, value)
            }
        } catch (e:Exception) {
            if (printStacktrace) e.printStackTrace()
            return null
        }
        return Pair(tag.name, instance)
    }

    /**
     * determine that converter not handle these value types in createTag/toValue
     */
    private fun isExcluded(type:Class<*>):Boolean {
        return typeBlackList.any { black->
            when (black.second) {
                ExcludeStrategy.CONVARIANT -> { black.first.isAssignableFrom(type)}
                ExcludeStrategy.INVARIANT -> { black.first == type}
                ExcludeStrategy.CONTRAVIARANT -> { type.isAssignableFrom(black.first)}
            }
        }
    }

    /**
     * add type that want reflective converter ignores(return null) in createTag/toValue
     */
    fun addExcludedType(type:Class<*>, strategy: ExcludeStrategy = ExcludeStrategy.CONVARIANT) {
        if (!isExcluded(type)) this.typeBlackList.add(type to strategy)
    }

    private val typeBlackList = ArrayList<Pair<Class<*>, ExcludeStrategy>>()

    init {
        typeBlackList.addAll(listOf(
                Iterable::class.java to ExcludeStrategy.CONVARIANT,
                Map::class.java to ExcludeStrategy.CONVARIANT,
        ))
    }

    data class CreateSubTagArgs(
            val value:Any, val subTagName:String,
            val createSubTagIntent: CreateTagIntent,
            val fieldTypeToken: MTypeToken<out Any>, val subTagTargetId:Byte?,
            val fieldRelatePath: List<String>
    )

    /**
     * set reflective converter handle object with nullable properties or not.
     *
     * if value is true, object return from toValue will have some null members if converter can not handle it
     * (can not converted to field expected type or can not access field)
     */
    var returnObjectWithNullableProperties = true

    /**
     * only java bean class object will be created and return from [toValue] method
     */
    var onlyJavaBean:Boolean = false

    /**
     * output stacktrace info if exception threw in [createTag]/[toValue].
     * if [throwExceptionInCreateTag] is set to true, converter will not repeat output stacktrace
     */
    var printStacktrace:Boolean = false

    /**
     * if any exception occurs during [createTag], throw it.
     *
     * if variable is false, createTag function will capture any exceptions and return null
     */
    var throwExceptionInCreateTag = false

    enum class ExcludeStrategy {
        CONVARIANT, CONTRAVIARANT, INVARIANT
    }
}