package net.myna.mnbt.tag

import net.myna.mnbt.IdTagByteArray
import net.myna.mnbt.IdTagIntArray
import net.myna.mnbt.IdTagLongArray
import net.myna.mnbt.Tag
import net.myna.mnbt.utils.SnbtTools
import java.util.*

@Suppress("EqualsOrHashCode")
class IntArrayTag(override val name: String?, override val value: IntArray, override val id: Byte = IdTagIntArray) : Tag<IntArray>() {
    override fun equals(other: Any?): Boolean = TagTools.reflectiveArrayTagEq(this, other)
    override fun valueToString(parents: Deque<Tag<*>>): String {
        return SnbtTools.sequenceToString(value.asSequence())
    }
}
@Suppress("EqualsOrHashCode")
class ByteArrayTag(override val name: String?, override val value: ByteArray, override val id: Byte = IdTagByteArray) : Tag<ByteArray>() {
    override fun equals(other: Any?): Boolean = TagTools.reflectiveArrayTagEq(this, other)
    override fun valueToString(parents: Deque<Tag<*>>): String {
        return SnbtTools.sequenceToString(value.asSequence())
    }
}
@Suppress("EqualsOrHashCode")
class LongArrayTag(override val name: String?, override val value: LongArray, override val id: Byte = IdTagLongArray) : Tag<LongArray>() {
    override fun equals(other: Any?): Boolean = TagTools.reflectiveArrayTagEq(this, other)
    override fun valueToString(parents: Deque<Tag<*>>): String {
        return SnbtTools.sequenceToString(value.asSequence())
    }
}