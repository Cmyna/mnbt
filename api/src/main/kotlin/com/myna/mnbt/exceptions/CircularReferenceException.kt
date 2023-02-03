package com.myna.mnbt.exceptions

import java.lang.RuntimeException

/**
 * @param value object that holds itself directly or indirectly
 */
class CircularReferenceException(value: Any):RuntimeException() {

    override val message: String? = "found circular reference \n" +
            "circular reference Instance: ${value}, circular object Type: ${value::class.java}"

}