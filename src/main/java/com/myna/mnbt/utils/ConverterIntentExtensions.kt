package com.myna.mnbt.utils

import com.myna.mnbt.converter.StartAt
import com.myna.mnbt.converter.ToValueIntent
import java.lang.reflect.Proxy

/**
 * return a new object implements all previous intent's interface with StartAt interface,
 * all properties getter method from original intent keeps same,
 * properties from StartAt intent return parameters passed in this function.
 *
 * eg: [StartAt.path] return [tagName]
 *
 * [StartAt.tagTypeId] return [id]
 */
fun ToValueIntent.addStartAtIntent(tagName:String, id:Byte):ToValueIntent {
    return Proxy.newProxyInstance(
            this::class.java.classLoader,
            this::class.java.interfaces + StartAt::class.java
    ) { _,method,vargs->
        when (method.name) {
            "getPath" -> return@newProxyInstance tagName
            "getTagTypeId" -> return@newProxyInstance id
            else -> { method.invoke(this, *vargs.orEmpty())}
        }
    } as ToValueIntent
}