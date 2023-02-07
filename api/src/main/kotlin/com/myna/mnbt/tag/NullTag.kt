package com.myna.mnbt.tag

import com.myna.mnbt.IdTagEnd
import com.myna.mnbt.Tag
import java.lang.NullPointerException
import java.lang.RuntimeException
import kotlin.reflect.KProperty

/**
 * Null Tag express that it is null
 */
class NullTag: Tag<Unit>() {
    override val id: Byte = IdTagEnd
    override val name: String?=null
    override val value:Unit get() { }
    override fun equals(other: Any?): Boolean {
        return other is NullTag
    }
}

