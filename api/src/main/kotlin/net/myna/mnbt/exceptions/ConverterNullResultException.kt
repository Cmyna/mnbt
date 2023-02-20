package net.myna.mnbt.exceptions

import net.myna.mnbt.Tag

class ConverterNullResultException(): Exception() {
    override var message: String = ""

    constructor(value:Any):this() {
        if (value is Tag<*>) this.message = "Converter can not handle tag with value type: ${if (value.value==null)value.value else value.value!!::class.java}"
        else this.message = "Converter can not handle value ${value::class.java}"
    }
}