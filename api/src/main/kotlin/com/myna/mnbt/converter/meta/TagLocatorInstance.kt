package com.myna.mnbt.converter.meta

import com.myna.mnbt.Tag
import com.myna.mnbt.converter.TagLocator

class TagLocatorInstance
    (private val root:Tag<out Any>)
    :TagLocator {

    override fun findAt(absolutePath: String, id:Byte): Tag<out Any>? {
        TODO("Not yet implemented")
    }

    override fun linkTagAt(absolutePath: String, tag: Tag<out Any>): Boolean {
        TODO("Not yet implemented")
    }
}