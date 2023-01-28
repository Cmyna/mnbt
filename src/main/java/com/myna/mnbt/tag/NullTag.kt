package com.myna.mnbt.tag

import com.myna.mnbt.IdTagEnd
import com.myna.mnbt.Tag

/**
 * Null Tag express that it is null
 */
class NullTag(override val name: String?=null, override val value: Nothing?=null, override val id: Byte = IdTagEnd) : Tag<Nothing?>() {
    override fun equals(other: Any?): Boolean {
        return other is NullTag
    }
}