package com.myna.mnbt

/**
 * abstract class to represent Nbt object
 *
 * generic type [NbtRelatedType]: the java class type that refers to a actual nbt data type
 */
abstract class Tag<NbtRelatedType> {
    // in consideration, Tag should have limit sub-class only related to specific nbt-bin data structure
    // but I want it more extendable, thus use a generic type, problem is hard to restrict generic type
    // finally it seems impossible to restrict it in Tag class, I decide to restrict it in other interface
    // now Tag is consider as a "package" of NbtData,
    abstract val name:String?
    abstract val value: NbtRelatedType

    /**
     * the nbt data type id of a tag
     */
    abstract val id:Byte
}





