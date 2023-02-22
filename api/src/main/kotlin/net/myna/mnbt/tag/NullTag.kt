package net.myna.mnbt.tag

import net.myna.mnbt.IdTagEnd
import net.myna.mnbt.Tag
import java.util.*

/**
 * Null Tag express that it is null. It is not a valid tag in nbt protocol
 */
class NullTag private constructor(): Tag<Unit>() {
    override val id: Byte = IdTagEnd
    override val name: String?=null
    override val value:Unit get() { }
    override fun equals(other: Any?): Boolean {
        return other is NullTag
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun valueToString(parents: Deque<Tag<*>>): String {
        return "NullTag @${this.hashCode()}"
    }

    companion object {
        val inst:NullTag = NullTag()
    }
}

