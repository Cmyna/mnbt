package net.myna.mnbt.utils

import kotlin.reflect.KProperty

object UnsupportedProperty {
    operator fun getValue(target:Any, property: KProperty<*>): Byte {
        throw NotImplementedError()
    }

}