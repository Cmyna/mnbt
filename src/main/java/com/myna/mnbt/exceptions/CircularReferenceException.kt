package com.myna.mnbt.exceptions

import java.lang.RuntimeException

class CircularReferenceException(tagValue: Any):RuntimeException() {

    override val message: String? = "found circular reference in TagValue Structure\n" +
            "circular Instance: ${tagValue}, circular TagValue Type: ${tagValue::class.java}"

}