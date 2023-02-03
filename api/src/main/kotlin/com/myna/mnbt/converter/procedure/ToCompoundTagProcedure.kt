package com.myna.mnbt.converter.procedure

import com.myna.mnbt.tag.CompoundTag
import java.lang.Exception

abstract class ToCompoundTagProcedure {

    abstract fun toDataEntryTag(root: CompoundTag):CompoundTag

    abstract fun toSubTagDataEntryTag(dataEntryTag:CompoundTag):CompoundTag

    abstract fun listDispatchValue(value:Any):Map<String, Any>

    fun procedure(rootName:String?):CompoundTag? {
        try {
            val root = CompoundTag(rootName)
            val dataEntryTag = toDataEntryTag(root)
            return root
        } catch (e:Exception) {
            e.printStackTrace()
            return null
        }
    }
}