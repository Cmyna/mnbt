package net.myna.mnbt.converter.procedure

import net.myna.mnbt.Tag
import net.myna.mnbt.converter.CreateTagIntent
import net.myna.mnbt.converter.OverrideTag
import net.myna.mnbt.converter.meta.NbtPathTool
import net.myna.mnbt.converter.overrideTagIntent
import net.myna.mnbt.tag.AnyCompound
import net.myna.mnbt.tag.AnyTagList
import net.myna.mnbt.tag.UnknownList

object ToNestTagProcedure {

    fun handleOverrideTargetIntent(toTargetTagPath:String, intent: CreateTagIntent): CreateTagIntent {
        if (intent is OverrideTag) {
            val target = intent.overrideTarget
            if (target !is Tag.NestTag<*>) return intent
            val subTarget = NbtPathTool.goto(target, toTargetTagPath)
            return overrideTagIntent(intent, subTarget)
        } else return intent
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

    fun tryAppendMissSubTag(resultList: AnyTagList, intent: CreateTagIntent) {
        if (intent is OverrideTag) {
            val target = intent.overrideTarget
            if (target?.value !is UnknownList) return
            val overriddenList = target.value as AnyTagList
            val remain = overriddenList.subList(resultList.size, overriddenList.size)
            resultList.addAll(remain)
        }
    }


}

