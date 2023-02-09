package com.myna.mnbt.converter

import com.myna.mnbt.Tag
import java.lang.reflect.Proxy
import java.util.*

fun nestCIntent(intent: ToValueIntent, ignoreTypeToken:Boolean): ToValueIntent {
    val interfaces = intent::class.java.interfaces.toMutableSet()
    if (ignoreTypeToken) interfaces.add(IgnoreValueTypeToken::class.java)
    else interfaces.remove(IgnoreValueTypeToken::class.java)
    return Proxy.newProxyInstance(intent::class.java.classLoader, interfaces.toTypedArray()) {
        _, method, args ->
        return@newProxyInstance method.invoke(intent, *args.orEmpty())
    } as ToValueIntent
}

fun converterCallerIntent(ignoreTypeToken:Boolean = false): ToValueIntent {
    return if (ignoreTypeToken) object: IgnoreValueTypeToken,RecordParents {
        override val parents: Deque<Any> = ArrayDeque()
    } else object:RecordParents,ToValueIntent {
        override val parents: Deque<Any> = ArrayDeque()
    }
}

fun createTagUserIntent(): CreateTagIntent {
    return object: CreateTagIntent, RecordParents {
        override val parents: Deque<Any> = ArrayDeque()
    }
}

fun overrideTagUserIntent(targetTag: Tag<out Any>): CreateTagIntent {
    return object: CreateTagIntent, OverrideTag, RecordParents {
        override val overrideTarget: Tag<out Any> = targetTag
        override val parents: Deque<Any> = ArrayDeque()
    }
}

