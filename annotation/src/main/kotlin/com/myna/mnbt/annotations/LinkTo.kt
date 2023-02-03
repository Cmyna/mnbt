package com.myna.mnbt.annotations

import java.lang.NullPointerException

const val IdTagCompound:Byte = 10

@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
/**
 * this annotation is used for class that declares relationship to a Tag object
 *
 * it can only used for check the Tag name and value type is as expected
 *
 * @param path: the mnbt url related path
 * the path format is something like `./<tagName1>/<tagName2>/<targetTagName>`
 *
 * @param typeId: the related tag id
 */
annotation class LinkTo(val path:String, val typeId: Byte = IdTagCompound) {

}
