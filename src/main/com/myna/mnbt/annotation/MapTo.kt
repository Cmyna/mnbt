package com.myna.mnbt.annotation

import com.myna.mnbt.IdTagCompound
import java.lang.NullPointerException
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic


@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
/**
 * this annotation is used for class that declares relationship to a Tag object
 *
 * it can only used for check the Tag name and value type is as expected
 *
 * @param path: the path to the related tag,
 * the path format is something like `<tagName1>.<tagName2>.<targetTagName>`
 *
 * @param typeId: the related tag id
 */
annotation class MapTo(val path:String, val typeId: Byte = IdTagCompound) {


    companion object {

        /**
         * match valid tag name in nbt tags path, it should starts at a string without '.' at start, split each tag name with '.',
         * if tag name has '.', use '\\.' instead
         *
         * `eg: path "pack1.pack2.*" will match "pack1","pack2","*"`
         *
         * `eg2: path "xyz\\.xyz.abc." will match "xyz\\.xyz","abc"`
         */
        private const val packNamePattern = "([^\\.]*\\\\.)*[^\\.]*"

        /**
         * the [Regex] pattern that match first package name in path,
         * it use "^[packNamePattern]" as pattern
         * see pattern [packNamePattern] for further info
         */
        @JvmStatic
        // the result will be something like "xyz" (original: "xyz.[remain path]") or "xyz\.xyz" (original: "xyz\.xyz.[remain path]")
        // so to get the real name(eg: transfer "xyz\.xyz" to "xyz.xyz"), needs post processing
        // how regex work: denote sub-patterns A=([^\.]*) B=(\\.)   (here use character '\' presents in actual String, not text '\\' presents in java source code)
        //      the whole regex is (AB)*A
        val firstPackageNameRegex = Regex("^$packNamePattern")


        /**
         * the [Regex] pattern that match each tag name in path,
         * see pattern [packNamePattern] used in this regex
         */
        @JvmStatic
        val packageNameRegex = Regex(packNamePattern)

        fun getFirstPackageName(path:String):String {
            val res = firstPackageNameRegex.find(path,0)?.value?.let {
                val list = it.split("\\.")
                if (list.size == 1) list[0]
                else {
                    val builder = StringBuilder()
                    list.onEachIndexed { i,str->
                        if (i<list.size-1) builder.append("$str.")
                        else builder.append(str)
                    }
                    builder.toString()
                }
            }
            return res?: throw NullPointerException()
        }



    }

}
