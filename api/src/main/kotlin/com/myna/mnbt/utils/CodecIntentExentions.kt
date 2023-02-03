package com.myna.mnbt.utils

import com.myna.mnbt.ByteSizePayload
import com.myna.mnbt.IntSizePayload
import com.myna.mnbt.codec.*
import com.myna.mnbt.core.CodecTool
import com.myna.mnbt.utils.Extensions.toBasic

object CodecIntentExentions {

    fun DecodeOnStream.getInt():Int = inputStream.readNBytes(4).toBasic(0,0)

    fun DecodeFromBytes.getInt():Int {
        val i = this.data.toBasic(this.pointer, 0)
        this.pointer += IntSizePayload
        return i
    }

    fun DecodeOnStream.getByte():Byte = inputStream.read().toByte()

    fun DecodeFromBytes.getByte():Byte = this.data[this.pointer].also {
        this.pointer += ByteSizePayload
    }

    fun DecodeOnStream.tryGetId():Byte {
        //this.inputStream.mark(1)
        val b = this.inputStream.read().toByte()
        //this.inputStream.reset()
        return b
    }

    fun DecodeHead.decodeHead(id: Byte): String? {
        val ignoreId = this.ignoreIdWhenDecoding
        return if (this is DecodeOnStream) {
            if(!ignoreId) CodecTool.checkNbtFormat(inputStream, id)
            CodecTool.readNbtName(inputStream)
        } else if (this is DecodeFromBytes) {
            if(!ignoreId) CodecTool.checkNbtFormat(data, pointer, id)
            val pair = CodecTool.readNbtName(data, pointer)
            pointer = pair.second
            pair.first
        }
        else null
    }
}