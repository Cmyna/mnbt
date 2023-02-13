package com.myna.mnbt.converter.procedure

import com.myna.mnbt.Tag
import com.myna.mnbt.converter.CreateTagIntent
import com.myna.mnbt.converter.OverrideTag
import com.myna.mnbt.converter.TagConverter
import com.myna.mnbt.converter.meta.NbtPathTool
import com.myna.mnbt.converter.overrideTagIntent
import com.myna.mnbt.reflect.MTypeToken
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.AnyTagList

// TODO: try to find more simple override nest tag when sub tag has built
/**
 * The procedure is a state object recording some temporal data when creating new nest tag (hierarchical tag)
 */
interface ToNestTagProcedure<TAG: Tag<out Any>, ARGS: ToNestTagProcedure.ToSubTagArgs>
{
    val baseArgs: BaseArgs
    val proxy: TagConverter<Any> get() = baseArgs.proxy
    val targetName:String? get() = baseArgs.targetName
    val value:Any get() = baseArgs.value
    val typeToken: MTypeToken<out Any> get() = baseArgs.typeToken
    val intent: CreateTagIntent get() = baseArgs.intent

    // the idea is there are some same procedures when creating hierarchical tag
    // firstly they need to find the element value with their name delegate to proxy to create sub tag
    // secondly they need to handle returned subTag and add it to target tag (proxy return back may be null)

    fun toSubTagArgsList():List<ARGS>
    fun checkProcedureArgs():Boolean
    fun buildTargetTag(subTags:List<Pair<Tag<out Any>?, ARGS>>):TAG?
    fun toSubTagRelatePath(args: ARGS):String

    companion object {
        fun <TAG: Tag<out Any>, ARGS: ToSubTagArgs> ToNestTagProcedure<TAG, ARGS>.procedure():TAG? {
            if (!checkProcedureArgs()) return null
            val subArgs = toSubTagArgsList()
            val proxyReturns = subArgs.map {
                val overrideIntent = this.handleOverrideIntent(it)
                if (it.value==null) return@map Pair(null, it)
                val subTag = proxy.createTag(it.name, it.value, it.typeToken, overrideIntent)
                Pair(subTag, it)
            }
            return buildTargetTag(proxyReturns)
        }

        private fun <TAG: Tag<out Any>, ARGS: ToSubTagArgs> ToNestTagProcedure<TAG, ARGS>.handleOverrideIntent(toSubTagArgs: ARGS):CreateTagIntent {
            if (this.intent is OverrideTag) {
                val path = this.toSubTagRelatePath(toSubTagArgs)
                val target = (this.intent as OverrideTag).overrideTarget
                if (target !is Tag.NestTag<*>) return this.intent
                //val subTarget = target.getElementByPath<Any>(path)
                val subTarget = NbtPathTool.goto(target, path)
                return overrideTagIntent(intent, subTarget)
            } else {
                return this.intent
            }
        }

        fun appendMissSubTag(resultMap:AnyCompound, overridden:Tag<AnyCompound>) {
            val miss = overridden.value.filter{ !resultMap.containsKey(it.key) }
            resultMap.putAll(miss)
        }

        fun tryAppendMissSubTag(resultMap: AnyCompound, intent:CreateTagIntent) {
            if (intent is OverrideTag && intent.overrideTarget?.value is Map<*,*>) {
                appendMissSubTag(resultMap, intent.overrideTarget!! as Tag<AnyCompound>)
            }
        }


    }

    class BaseArgs(
            val proxy: TagConverter<Any>,
            val targetName: String?,
            val value:Any,
            val typeToken: MTypeToken<out Any>,
            val intent: CreateTagIntent
    )

    open class ToSubTagArgs(val name:String?, val value: Any?, val typeToken: MTypeToken<out Any>, val intent: CreateTagIntent)


}

