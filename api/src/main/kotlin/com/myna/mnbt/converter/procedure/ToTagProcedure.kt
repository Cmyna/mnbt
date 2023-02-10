package com.myna.mnbt.converter.procedure

import com.myna.mnbt.Tag
import com.myna.mnbt.converter.CreateTagIntent
import com.myna.mnbt.converter.TagConverter
import com.myna.mnbt.reflect.MTypeToken

interface ToTagProcedure<TAG: Tag<out Any>> {

    val baseArgs:BaseArgs
    val targetName:String? get() = baseArgs.targetName
    val value:Any get() = baseArgs.value
    val typeToken: MTypeToken<out Any> get() = baseArgs.typeToken
    val intent: CreateTagIntent get() = baseArgs.intent




    companion object {

        fun <TAG: Tag<out Any>> ToTagProcedure<TAG>.procedure():TAG? {
            TODO()
        }
    }

    class BaseArgs(
            val targetName: String?,
            val value:Any,
            val typeToken: MTypeToken<out Any>,
            val intent: CreateTagIntent
    )
}