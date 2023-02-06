package com.myna.mnbt.annotations

import java.lang.NullPointerException

const val IdTagCompound:Byte = 10

@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
/**
 * this annotation is used for class that declares relationship to a Tag object.
 * It declares the related path to Nbt Tree Structure
 *
 * @param path: the mnbt url related path
 * the path format is something like `./<tagName1>/<tagName2>/<targetTagName>`
 *
 * @param typeId: the related tag id
 */
annotation class LocateAt(val path:String, val typeId: Byte = IdTagCompound) {

}
