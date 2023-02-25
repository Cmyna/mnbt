package net.myna.mnbt.converter

import net.myna.mnbt.IdTagCompound
import net.myna.mnbt.tag.AnyCompound
import net.myna.mnbt.tag.CompoundTag
import net.myna.mnbt.Tag
import net.myna.mnbt.annotations.Ignore
import net.myna.mnbt.annotations.IgnoreFromTag
import net.myna.mnbt.annotations.LocateAt
import net.myna.mnbt.utils.NbtPathTool
import net.myna.mnbt.converter.procedure.ToNestTagProcedure
import net.myna.mnbt.reflect.MTypeToken
import net.myna.mnbt.reflect.ObjectInstanceHandler
import net.myna.mnbt.tag.UnknownCompound
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.NullPointerException
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.jvm.javaGetter

/**
 * this Converter is used for some simple POJO class,
 */
class ReflectiveConverter(override var proxy: TagConverter<Any>): HierarchicalTagConverter<AnyCompound>() {

    // Conversion to CompoundTag procedure:
    //      create returnedTag with name parameter passed in
    //      from returnedTag build contains tree to dataEntryTag
    //      create subTag
    //      for each subTag, from dataEntryTag build contains tree to subTagDataEntryTag, add subTag to subTagDataEntryTag
    //
    //      the compound tag structure is root=>dataEntryTag=>subTagDataEntryTag->subTag
    //          (or root->...->dataEntryTag->...->subTagDataEntryTag->subTag)
    //      ( '=>' means directly/indirectly contains, '->' means directly contains)
    @Suppress("UNCHECKED_CAST")
    override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: CreateTagIntent): Tag<AnyCompound>? {
        // return ReflectiveToTagProcedure(ToNestTagProcedure.BaseArgs(proxy, name, value, typeToken, intent)).procedure()
        // parameters check
        val valueClass = value::class.java
        if (isExcluded(valueClass)) return null

        // get tag locator, else build a new one
        val (functionReturnedTag, subTreeRoot, returnedTagPath) = if (intent is BuiltCompoundSubTree) {
            val got = NbtPathTool.findTag(intent.root, intent.createdTagRelatePath, IdTagCompound)?: CompoundTag(name)
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
        val classLocateAtMeta = typeToken.rawType.getAnnotation(LocateAt::class.java)
        val targetToDataEntryPath:String = if (classLocateAtMeta!=null) {
            NbtPathTool.toRelatedPath(classLocateAtMeta.toTagPath)
        } else "./"
        val builtRootToDataEntryPath:String = NbtPathTool.combine(returnedTagPath, targetToDataEntryPath)

        val toDataEntrySequence = if (classLocateAtMeta!=null) {
            NbtPathTool.toAccessSequence(classLocateAtMeta.toTagPath)
        } else sequenceOf()

        // build to data Entry tag
        val dataEntryTag = toDataEntrySequence.fold(functionReturnedTag) { parent, childName->
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


        // fields' info: fields' compound tags access sequence from data entry tag
        val fieldsInfo = fields.mapNotNull { field ->
            val fieldLocateAtMeta = field.getAnnotation(LocateAt::class.java)
            if (Ignore.ignoreToTag(field)) return@mapNotNull null

            if (fieldLocateAtMeta!=null) {
                val accessSeq = NbtPathTool.toAccessSequence(fieldLocateAtMeta.toTagPath).toList()
                Pair(field, accessSeq)
            } else {
                Pair(field, listOf(field.name))
            }
        }

        val createSubTagsArgs = fieldsInfo.mapNotNull { (field,accessSequence)->
            val fieldTagPath = getSubTagPath(builtRootToDataEntryPath, accessSequence)
            val fieldIntent = buildSubTagCreationIntent(
                    fieldTagPath,
                    intent,
                    NbtPathTool.append(targetToDataEntryPath, accessSequence.last()),
                    subTreeRoot)
            val idTODO:Byte = 0
            createSubTagArgs(field, value, accessSequence, fieldTagPath, idTODO, fieldIntent)
        }


        try {
            createSubTagsArgs.onEach {
                val subTag = proxy.createTag(it.name, it.value, it.typeToken, it.intent) //?:return@onEach
                val creationSuccess = subTag!=null
                if (!creationSuccess) return@onEach
                // build sub tree contains subtag
                val subTagContainer = it.fieldRelatePath.dropLast(1).fold(dataEntryTag) { parent, childName->
                    val parentValue = parent.value
                    val child = (parentValue[childName]?: CompoundTag(childName)) as Tag<AnyCompound>
                    parentValue[childName] = child
                    child
                }
                subTagContainer.value[it.name] = subTag!!
            }

            if (intent is OverrideTag && intent.overrideTarget?.value is UnknownCompound) {
                val overriddenDataEntryTag = NbtPathTool.findTag(intent.overrideTarget!!, targetToDataEntryPath, IdTagCompound)
                if (overriddenDataEntryTag?.value is UnknownCompound) {
                    ToNestTagProcedure.appendMissSubTag(dataEntryTag.value, overriddenDataEntryTag as Tag<AnyCompound>)
                }
            }
            return functionReturnedTag
        } catch(e:Exception) {
            if (throwExceptionInCreateTag) throw e
            else if (printStacktrace) e.printStackTrace()
            return null
        }
    }


    private fun createSubTagArgs(
            field: Field, value:Any,
            fieldRelatedPath:List<String>, fieldRelatedPathStr: String, targetTagId:Byte?,
            createSubTagIntent:CreateTagIntent
    ):CreateSubTagArgs? {
        try {
            val accessible = field.trySetAccessible()
            if (!accessible) return null // if try set Accessible return false, it may be a static final member
            val fieldValue = field.get(value) ?: return null // actual value is null, skip this field

            val subTagName = if (fieldRelatedPath.isNotEmpty()) fieldRelatedPath.last() else field.name
            val fieldTk = MTypeToken.of(field.genericType) as MTypeToken<out Any>
            return CreateSubTagArgs(fieldValue, subTagName, createSubTagIntent, fieldTk, targetTagId, fieldRelatedPath, fieldRelatedPathStr)
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
            subTreeRootToSubTagPath:String, callerIntent: CreateTagIntent,
            returnedTagToSubTagPath:String, buildRoot:Tag<out Any>):CreateTagIntent {
        val overrideTarget = if (callerIntent is OverrideTag && callerIntent.overrideTarget!=null) {
            NbtPathTool.findTag(callerIntent.overrideTarget!!, returnedTagToSubTagPath)
        } else null
        return Proxy.newProxyInstance(callerIntent::class.java.classLoader, callerIntent::class.java.interfaces) {
            _,method,args ->
            return@newProxyInstance when(method) {
                BuiltCompoundSubTree::createdTagRelatePath.javaGetter -> subTreeRootToSubTagPath
                BuiltCompoundSubTree::root.javaGetter -> buildRoot
                OverrideTag::overrideTarget.javaGetter -> overrideTarget
                else ->method.invoke(callerIntent, *args.orEmpty())
            }
        } as CreateTagIntent
    }

    // core idea: first find constructors
    // try to deserialize them with field related class as typeToken
    // if one of return is null, means total conversion failed, final result will become null
    // if can not construct instance, return null
    @Suppress("UNCHECKED_CAST")
    override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ToValueIntent): Pair<String?, V>? {
        // parameter check
        intent as RecordParents
        if (tag.value !is Map<*,*>) return null
        val declaredRawType = typeToken.rawType
        if (isExcluded(declaredRawType)) return null

        val instance = ObjectInstanceHandler.newInstance(declaredRawType as Class<V>, onlyJavaBean)?: return null
        val fields = ObjectInstanceHandler.getAllFields(declaredRawType)

        val classLocateAtMeta = typeToken.rawType.getAnnotation(LocateAt::class.java)

        val dataEntryTag = if (classLocateAtMeta != null) {
            // try to get data entry tag from annotation LocateAt
            val found = NbtPathTool.findTag(tag, NbtPathTool.format(LocateAt.getDecodePath(classLocateAtMeta)))
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
                // if can not access field, and also require non-nullable properties, return null
                if (!accessible) return@mapValues null
                // try ignoreFromTag
                val ignoreFromTag = Ignore.tryGetIgnoreFromTag(field)
                if (ignoreFromTag != null) return@mapValues IgnoreFromTag.tryProvide(ignoreFromTag.fieldValueProvider, field)
                val fieldTypeToken = MTypeToken.of(field.genericType)

                val fieldTag = NbtPathTool.findTag(dataEntryTag, it.value)?: return@mapValues null
                val value = proxy.toValue(fieldTag, fieldTypeToken, nestCIntent(intent, false))

                value?.second
            }.onEach { entry-> // set field into instance
                if (entry.value==null) {
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
        // if it is abstract class or interface, it is excluded
        if (type.isInterface || Modifier.isAbstract(type.modifiers)) return true
        return typeBlackList.any { black->
            when (black.second) {
                ExcludeStrategy.CONVARIANT -> { black.first.isAssignableFrom(type) }
                ExcludeStrategy.INVARIANT -> { black.first == type }
                ExcludeStrategy.CONTRAVIARANT -> { type.isAssignableFrom(black.first) }
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
                Any::class.java to ExcludeStrategy.INVARIANT,
        ))
    }

    class CreateSubTagArgs(
            val value:Any, val name:String, val intent: CreateTagIntent, val typeToken: MTypeToken<out Any>,
            val subTagTargetId:Byte?, val fieldRelatePath: List<String>, val fieldRelatedPathStr: String
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