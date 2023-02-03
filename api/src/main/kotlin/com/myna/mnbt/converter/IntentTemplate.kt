package com.myna.mnbt.converter

import java.lang.reflect.Proxy
import java.util.*

//TODO: change all intent creation called from Converter to dynamic proxy

fun nestCIntent(intent: ToValueIntent, ignoreTypeToken:Boolean): ToValueIntent {
    val interfaces = intent::class.java.interfaces.toMutableSet()
    if (ignoreTypeToken) interfaces.add(IgnoreValueTypeToken::class.java)
    else interfaces.remove(IgnoreValueTypeToken::class.java)
    interfaces.remove(StartAt::class.java)
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

