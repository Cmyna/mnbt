package com.myna.mnbt.converter.meta

import com.myna.mnbt.converter.ReflectiveConverter
import com.myna.mnbt.converter.TagConverter

/**
 * this interface is used in reflective conversion that [ReflectiveConverter]
 * can reconstruct tag structure from class TypeToken with its field passed in [TagConverter.createTag]/[TagConverter.toValue] function
 *
 * if one class want to remap to another tag structure (not follow by its class name & field name),
 * it should implement this interface.
 *
 *
 */
interface NbtPath {
    /**
     * return related Tag path to the class implement this interface
     *
     * eg: if result is `["tag1","tag2","tag3"]`, Converter will find tag1.tag2.tag3
     * starts at root tag passed in [TagConverter.toValue] method; else if result is empty array,
     * converter will regard tag passed in [TagConverter.toValue] as tag of target class
     *
     * if it is happen in [TagConverter.createTag] method, it will create the structure like:
     * `root.tag1.tag2.tag3` where root is the root compound tag with name parameter "root" passed in method;
     * it result is empty array, then tag named "root" will be regard as target tag for the class object
     */
    fun getClassNbtPath():Array<String>

    /**
     * return a map recording field name->related path.
     * The related path is path related to class related path from [getClassNbtPath].
     *
     * eg: if a field related path is ["subTag1","subTag2"], where class related path is ["tag1", "tag2"],
     * then reflective converter will try find tag1.tag2.subTag1.subTag2 in the root tag passed in
     */
    fun getFieldsPaths():Map<String, Array<String>>

    fun getFieldsTagType():Map<String, Byte>
}