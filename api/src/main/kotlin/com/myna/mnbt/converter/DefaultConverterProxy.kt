package com.myna.mnbt.converter

import com.myna.mnbt.defaultTreeDepthLimit
import com.myna.mnbt.exceptions.MaxNbtTreeDepthException
import com.myna.mnbt.Tag
import com.myna.mnbt.exceptions.CircularReferenceException
import com.myna.mnbt.reflect.MTypeToken
import kotlin.collections.ArrayDeque

@Suppress("UnstableApiUsage")
/**
 * default proxy for delegatorConverter.
 * it use gson like searching rules, store delegate to a list,
 * so that for delegate at front will have higher priority to handle a type require
 * (if there are other delegate at end can handle same type)
 */
class DefaultConverterProxy : HierarchicalTagConverter<Any>() {
    override var proxy: TagConverter<Any> = this

    private val delegateList = ArrayDeque<TagConverter<Any>>()

    var throwCircularReferenceException = false

    /**
     * register converter with bottom priority (if no other converter register last after)
     */
    fun registerToLast(delegate: TagConverter<Any>):Boolean {
        delegateList.add(delegate as TagConverter<Any>)
        return true
    }

    /**
     * register converter with top priority (if no other converter register first after)
     */
    fun registerToFirst(delegate: TagConverter<Any>):Boolean {
        delegateList.addFirst(delegate as TagConverter<Any>)
        return true
    }

    override fun <V : Any> createTag(name: String?, value: V, typeToken: MTypeToken<out V>, intent: CreateTagIntent): Tag<out Any>? {
        intent as RecordParents
        // try restrict type token if it is top bound(Object)
        val restrictedTypeToken = if (typeToken.rawType==Any::class.java) MTypeToken.of(value::class.java) else typeToken
        // check depth
        if (intent.parents.size > defaultTreeDepthLimit-1) throw MaxNbtTreeDepthException(intent.parents.size)
        // check circular reference
        val circularRef = intent.parents.find { value===it }
        if (circularRef != null) {
            if (throwCircularReferenceException) throw CircularReferenceException(value)
            else return null
        }

        intent.parents.push(value)
        var tag: Tag<out Any>? = null
        for (delegate in delegateList) {
            tag = delegate.createTag(name, value, restrictedTypeToken, intent)
            if (tag != null) break
        }
        intent.parents.pop()
        return tag
    }

    override fun <V : Any> toValue(tag: Tag<out Any>, typeToken: MTypeToken<out V>, intent: ToValueIntent): Pair<String?, V>? {
        // restrict typeToken to tag.value::class.java if typeToken.rawType is Any
        // so unexpectedly the code below also restrict the type variable... although it is what I want
        // TODO: although guava auto warp no-bounds type variable to Any::class.java, make the code more clearly
        val restrictedTypeToken = if (typeToken.rawType==Any::class.java) MTypeToken.of(tag.value::class.java) else typeToken
        restrictedTypeToken as MTypeToken<V>
        for (delegate in delegateList) {
            return delegate.toValue(tag, restrictedTypeToken, intent) ?: continue
        }
        return null
    }
}