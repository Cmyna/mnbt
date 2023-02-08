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
 * @param encodePath: the mnbt url relate path
 * the path format is something like `./<tagName1>/<tagName2>/<targetTagName>`,
 * it is not allowed to have any index format path segment in path (string like `#5`).
 * Mnbt will throws [IllegalArgumentException] if there are some index format path segment in createTag process
 * @param decodePath: the mnbt url relate path, if not specify this args or args value is "" (empty string),
 * it will be regarded as same value to encode path.
 * Different from [encodePath], index format path segment is allowed in path
 */
annotation class LocateAt(val encodePath:String, val decodePath:String = "") {

    companion object {
        fun getDecodePath(annotation: LocateAt):String {
            return if (annotation.decodePath == "") annotation.encodePath
            else annotation.decodePath
        }
    }
}
