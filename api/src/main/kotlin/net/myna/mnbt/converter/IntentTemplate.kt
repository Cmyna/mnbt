package net.myna.mnbt.converter

import net.myna.mnbt.Tag
import java.lang.reflect.Proxy
import java.util.*
import kotlin.reflect.jvm.javaGetter

fun nestCIntent(intent: ToValueIntent, ignoreTypeToken:Boolean): ToValueIntent {
    val interfaces = intent::class.java.interfaces.toMutableSet()
    if (ignoreTypeToken) interfaces.add(IgnoreValueTypeToken::class.java)
    else interfaces.remove(IgnoreValueTypeToken::class.java)
    return Proxy.newProxyInstance(intent::class.java.classLoader, interfaces.toTypedArray()) {
        _, method, args ->
        return@newProxyInstance method.invoke(intent, *args.orEmpty())
    } as ToValueIntent
}

@JvmOverloads
fun converterCallerIntent(ignoreTypeToken:Boolean = false): ToValueIntent {
    return if (ignoreTypeToken) object: IgnoreValueTypeToken,RecordParents {
        override val parents: Deque<Any> = ArrayDeque()
    } else object:RecordParents,ToValueIntent {
        override val parents: Deque<Any> = ArrayDeque()
    }
}

@JvmOverloads
fun createTagUserIntent(createTagIntent: CreateTagIntent? = null): CreateTagIntent {
    if (createTagIntent == null) return object: CreateTagIntent, RecordParents {
        override val parents: Deque<Any> = ArrayDeque()
    } else {
        val interfaces = createTagIntent::class.java.interfaces.toMutableSet()
        interfaces.add(RecordParents::class.java)
        Proxy.newProxyInstance(createTagIntent::class.java.classLoader, interfaces.toTypedArray()) { _,method,args->
            return@newProxyInstance when(method) {
                RecordParents::parents.javaGetter -> ArrayDeque<Any>()
                else -> method.invoke(createTagIntent, *args.orEmpty())
            }
        }
        TODO()
    }
}

@JvmOverloads
fun overrideTagUserIntent(targetTag: Tag<out Any>, intent: CreateTagIntent?=null): CreateTagIntent {
    if (intent == null)return object: CreateTagIntent, OverrideTag, RecordParents {
        override val overrideTarget: Tag<out Any> = targetTag
        override val parents: Deque<Any> = ArrayDeque()
    }
    val interfaces = intent::class.java.interfaces.toMutableSet()
    interfaces.add(RecordParents::class.java)
    interfaces.add(OverrideTag::class.java)
    return Proxy.newProxyInstance(intent::class.java.classLoader, interfaces.toTypedArray()) { _,method,args->
        return@newProxyInstance when(method) {
            RecordParents::parents.javaGetter -> ArrayDeque<Any>()
            OverrideTag::overrideTarget.javaGetter -> targetTag
            else -> method.invoke(intent, *args.orEmpty())
        }
    } as CreateTagIntent
}

fun overrideTagIntent(intent:CreateTagIntent, targetTag: Tag<out Any>?): CreateTagIntent {
    val interfaces = intent::class.java.interfaces.toMutableSet()
    if (targetTag==null) interfaces.remove(OverrideTag::class.java)
    return Proxy.newProxyInstance(intent::class.java.classLoader, interfaces.toTypedArray()) {
        _, method, args ->
        if (method == OverrideTag::overrideTarget.javaGetter) return@newProxyInstance targetTag
        return@newProxyInstance method.invoke(intent, *args.orEmpty())
    } as CreateTagIntent
}

