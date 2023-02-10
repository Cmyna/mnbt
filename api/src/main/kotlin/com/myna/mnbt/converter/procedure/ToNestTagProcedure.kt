package com.myna.mnbt.converter.procedure

import com.myna.mnbt.Tag
import com.myna.mnbt.converter.CreateTagIntent
import com.myna.mnbt.converter.TagConverter
import com.myna.mnbt.reflect.MTypeToken

abstract class HierarchicalTagCreationProcedure
    <A: ProcedureArgs, TAG: Tag<out Any>>
    (val proxy: TagConverter<out Any>) {
    // the idea is there are some same procedures when creating hierarchical tag
    // firstly they need to find the element value with their name delegate to proxy to create sub tag
    // secondly they need to handle returned subTag and add it to target tag (proxy return back may be null)

    abstract fun toCreateSubTagArgsList(procedureArgs: A):List<ToSubTagArgs>
    abstract fun getProcedureArgs(name:String?, value:Any, typeToken: MTypeToken<out Any>, intent: CreateTagIntent):A
    abstract fun checkProcedureArgs(ProcedureArgs: A):Boolean
    abstract fun getTargetTag(subTags:List<Tag<out Any>?>, procedureArgs: A):TAG?

    fun procedure(name:String?, value:Any, typeToken: MTypeToken<out Any>, intent: CreateTagIntent): TAG? {
        val createTagArgs = getProcedureArgs(name, value, typeToken, intent)
        if (!checkProcedureArgs(createTagArgs)) return null
        val subArgs = toCreateSubTagArgsList(createTagArgs)
        val proxyReturns = subArgs.map { proxy.createTag(it.name, it.value, it.typeToken, it.intent) }
        return getTargetTag(proxyReturns, createTagArgs)
    }



}

open class ProcedureArgs(val name:String?, val value:Any, val typeToken: MTypeToken<out Any>, val intent: CreateTagIntent)

open class ToSubTagArgs(val name:String, val value: Any, val typeToken: MTypeToken<out Any>, val intent: CreateTagIntent)