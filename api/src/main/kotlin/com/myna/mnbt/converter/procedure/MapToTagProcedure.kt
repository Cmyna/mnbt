package com.myna.mnbt.converter.procedure

import com.myna.mnbt.IdTagCompound
import com.myna.mnbt.Tag
import com.myna.mnbt.converter.*
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag

class MapToTagProcedure(proxy: TagConverter<Any>):HierarchicalTagCreationProcedure<ProcedureArgs, CompoundTag>(proxy) {

    private val mapTypeToken = MTypeToken.of(Map::class.java)
    private val mapValueGenericType = Map::class.java.typeParameters[1]


    override fun toCreateSubTagArgsList(procedureArgs: ProcedureArgs): List<ToSubTagArgs> {
        val value = procedureArgs.value as Map<String,Any>
        val declaredValueTypeToken = procedureArgs.typeToken.resolveType(mapValueGenericType) as MTypeToken<out Any>
        val overrideTarget = if (procedureArgs.intent is OverrideTag) procedureArgs.intent.overrideTarget else null

        return value.map { entry ->
            val overrideSubTag = if (overrideTarget?.value is Map<*,*>) (overrideTarget.value as AnyCompound)[entry.key] else null
            val subTagIntent = if (overrideSubTag!=null) overrideTagIntent(procedureArgs.intent, overrideSubTag) else procedureArgs.intent
            ToSubTagArgs(entry.key, entry.value, declaredValueTypeToken, subTagIntent)
        }
    }

    override fun getProcedureArgs(name: String?, value: Any, typeToken: MTypeToken<out Any>, intent: CreateTagIntent): ProcedureArgs {
        return ProcedureArgs(name, value, typeToken, intent)
    }

    override fun getTargetTag(subTags: List<Tag<out Any>?>, procedureArgs: ProcedureArgs): CompoundTag? {
        val map:AnyCompound = mutableMapOf()
        subTags.forEach {
            if (it==null || it.name==null) return@forEach
            map[it.name!!] = it
        }
        if (procedureArgs.intent is OverrideTag) {
            if (procedureArgs.intent.overrideTarget.id != IdTagCompound) return null
            (procedureArgs.intent.overrideTarget.value as AnyCompound).onEach {
                if (map[it.key] == null) map[it.key] = it.value
            }
        }
        return CompoundTag(procedureArgs.name, map)
    }

    override fun checkProcedureArgs(procedureArgs: ProcedureArgs): Boolean {
        val typeToken = procedureArgs.typeToken
        if (!typeToken.isSubtypeOf(mapTypeToken)) return false
        return true
    }
}

