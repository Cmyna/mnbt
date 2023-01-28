package com.myna.mnbt.exceptions

/**
 * TODO
 * Exception during conversion by reflect way (eg. ReflectiveConverter)
 */
class ConversionException(override val message: String? = null)
    :Exception() {
}