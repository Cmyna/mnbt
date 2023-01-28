package com.myna.mnbt.tag

import com.myna.mnbt.Tag

object TagSearcher {

    /**
     * find a tag with same name and value type, if not found return null
     */
    fun findTag(tag: Tag<out Any>, path:String, id:Byte):Tag<out Any>? {
        if (match(tag, path, id)) return tag

        val accessQueue = path.split('.') // last one is target tag name
        val targetTagName = accessQueue.last()
        val parentTag = goToTargetDir(tag, accessQueue)?: return null

        val value = parentTag.value
        if (value is Iterable<*>) {
            value.onEach { element->
                if (element !is Tag<*>) return@onEach
                if (element.name == targetTagName && element.id == id) return element as Tag<out Any>
            }
        }
        return null
    }

    /**
     * @param accessQueue list of tag names, specifies the access sequences
     */
    private fun goToTargetDir(tag:Tag<out Any>, accessQueue:List<String>):Tag<out Any>? {
        var current = tag
        for (i in 0 until accessQueue.size-1) {
            val subName = accessQueue[i]
            val value = current.value
            if (value !is Iterable<*>) return null
            val subTag = value.find { element->
                element is Tag<*> && element.name==subName
            }?: return null
            current = subTag as Tag<out Any>
        }
        return current
    }


    private fun match(tag: Tag<out Any>, name:String, id:Byte):Boolean {
        return (tag.name==name && tag.id==id)
    }
}