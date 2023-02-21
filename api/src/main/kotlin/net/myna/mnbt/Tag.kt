package net.myna.mnbt

import net.myna.mnbt.utils.SnbtTools
import java.util.*


typealias AnyTag = Tag<out Any>
/**
 * abstract class to represent Nbt object
 *
 * generic type [NbtRelatedType]: the java class type that refers to a actual nbt data type
 */
abstract class Tag<NbtRelatedType> {
    // in consideration, Tag should have limit subclass only related to specific nbt-bin data structure,
    // but I want it more extendable, thus use a generic type, problem is hard to restrict generic type
    // finally it seems impossible to restrict it in Tag class, I decide to restrict it in other interface
    // now Tag is consider as a "package" of NbtData,
    abstract val name:String?
    abstract val value: NbtRelatedType

    /**
     * the nbt data type id of a tag
     */
    abstract val id:Byte

    abstract fun valueToString(parents: Deque<Tag<*>>):String

    override fun toString(): String {
        val escapedName = if (this.name==null) "null" else SnbtTools.escape(this.name!!)
        val deque: Deque<Tag<*>> = ArrayDeque()
        return "{\"$escapedName\":${valueToString(deque)}}"
    }

    abstract class NestTag<NbtRelatedType>:Tag<NbtRelatedType>() {
        abstract fun <V> getElementByPath(pathSegment:String): Tag<out V>?

        override fun toString(): String {
            val escapedName = if (this.name==null) "null" else SnbtTools.escape(this.name!!)
            val deque: Deque<Tag<*>> = ArrayDeque()
            return "{\"$escapedName\":${valueToString(deque)}}"
        }
    }

    companion object {
        fun isTagAndEqName(tag: Tag<*>, other: Any?):Boolean {
            if (other==null) return false
            if (other !is Tag<*>) return false
            if (tag.name != other.name) return false
            return true
        }
    }
}





