package com.myna.mnbt.tag

import com.myna.mnbt.Tag


object TagSearcher {

    /**
     * match valid tag name in nbt tags path, it should starts at a string without '.' at start, split each tag name with '.',
     * if tag name has '.', use '\\.' instead
     *
     * `eg: path "pack1.pack2.*" will match "pack1","pack2","*"`
     *
     * `eg2: path "xyz\\.xyz.abc." will match "xyz\\.xyz","abc"`
     */
    private val packNameRegex = Regex("(?:(?:[^\\.]*\\\\.)+[^\\.]*)|(?:(?:[^\\.]*\\\\.)*[^\\.]+)")

    fun findTag(tag:Tag<out Any>, path:Iterable<String>, id:Byte):Tag<out Any>? {
        val accessQueue = path.toList()
        val target = findTag(tag, accessQueue) ?: return null
        return if (target.id!=id) null
        else target
    }

    fun toAccessQueue(path:String):List<String> {
        val accessQueue = ArrayList<String>()
        var match = packNameRegex.find(path)
        while (match!=null) {
            accessQueue.add(match.value)
            match = match.next()
        }
        return accessQueue
    }

    /**
     * find a tag with same name and value type, if not found return null
     */
    fun findTag(tag: Tag<out Any>, path:String, id:Byte):Tag<out Any>? {
        if (match(tag, path, id)) return tag
        val accessQueue = toAccessQueue(path)
        val target = findTag(tag, accessQueue) ?: return null
        return if (target.id!=id) null
        else target
    }

    /**
     * @param accessQueue list of tag names, specifies the access sequences
     */
    private fun findTag(tag:Tag<out Any>, accessQueue:List<String>):Tag<out Any>? {
        var current = tag
        accessQueue.onEach { subName->
            val value = current.value
            if (value !is Map<*,*>) return null
            val subTag = value[subName]?: return null
//            val subTag = value.find { element->
//                element is Tag<*> && element.name==subName
//            }?: return null
            current = subTag as Tag<out Any>
        }
        return current
    }


    private fun match(tag: Tag<out Any>, name:String, id:Byte):Boolean {
        return (tag.name==name && tag.id==id)
    }
}