package com.myna.utils

import com.myna.utils.DelegatedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.IndexOutOfBoundsException

class AdaptedInputStream(inputStream: InputStream): DelegatedInputStream(inputStream) {

    constructor(inputStream: ByteArrayInputStream):this(inputStream as InputStream)
    constructor(byteArray: ByteArray):this(ByteArrayInputStream(byteArray) as InputStream)

    override fun readNBytes(len:Int):ByteArray {
        if (inputStream is ByteArrayInputStream) {
            val bits = ByteArray(len)
            val n = super.read(bits, 0, len)
            if (n==-1 || n<len) throw IndexOutOfBoundsException("input stream is ByteArrayInputStream, and require len($len) is over available length ($n)")
            return bits
        }
        return super.readNBytes(len)
    }
}