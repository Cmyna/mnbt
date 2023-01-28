package com.myna.mnbt.core

import com.myna.mnbt.StringSizePayload
import com.myna.mnbt.utils.Extensions.toBytes
import com.myna.mnbt.utils.Extensions.toShort
import com.myna.mnbt.utils.Extensions.toString
import java.io.InputStream
import java.io.OutputStream
import java.lang.IllegalArgumentException

object CodecTool {

    /**
     * @return first element is ByteArray with wanted length, second one is pointer just after tag head(id-nameLen-name-pointer---)
     */
    private fun initEncodedBits(id:Byte, name:String, valueBitsLen: Int):Pair<ByteArray, Int> {
        val nameBits = name.toByteArray(Charsets.UTF_8)
        if (nameBits.size > 65535) throw IllegalArgumentException("name bits length(${nameBits.size}) out of short length(65535)!")
        val bytes = ByteArray(3+nameBits.size+valueBitsLen)
        bytes[0] = id
        System.arraycopy(nameBits.size.toShort().toBytes(), 0, bytes, 1, StringSizePayload)
        System.arraycopy(nameBits, 0, bytes, 3, nameBits.size)
        return Pair(bytes, 3+nameBits.size)
    }

    fun writeTagHead(id:Byte, name:String, outputStream: OutputStream) {
        val nameBits = name.toByteArray(Charsets.UTF_8)
        if (nameBits.size > 65535) throw IllegalArgumentException("name bits length(${nameBits.size}) out of short length(65535)!")
        outputStream.write(id.toInt())
        outputStream.write(nameBits.size.toShort().toBytes())
        outputStream.write(nameBits)
    }

    fun getTagBits(id:Byte, name:String, valueBits:ByteArray):ByteArray {
        val finalBits = initEncodedBits(id, name, valueBits.size)
        System.arraycopy(valueBits, 0, finalBits.first, finalBits.second, valueBits.size)
        return finalBits.first
    }

    /**
     * read nbt name and move to pointer to next part
     */
    fun readNbtName(data:ByteArray, start: Int):Pair<String, Int> {
        var pointer = start
        val len = data.toShort(pointer)
        pointer += StringSizePayload
        val name = data.toString(pointer, len.toInt())
        pointer += len
        return Pair(name, pointer)
    }

    fun readNbtName(inputStream: InputStream): String {
        val len = inputStream.readNBytes(2).toShort(0).toInt()
        val bits = inputStream.readNBytes(len)
        return bits.toString(Charsets.UTF_8)
    }

    fun checkNbtFormat(arr:ByteArray, start:Int, id:Byte) {
        if (arr[start] != id) throw IllegalArgumentException("Deserialized source id incompatible. Expect $id but got ${arr[start]}")
    }

    fun checkNbtFormat(inputStream: InputStream, id:Byte) {
        val inId = inputStream.read().toByte()
        if (inId != id) throw IllegalArgumentException("Deserialized source id incompatible. Expect $id but got $inId")
    }
}