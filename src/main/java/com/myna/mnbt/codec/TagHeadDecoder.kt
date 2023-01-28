package com.myna.mnbt.codec

import com.myna.mnbt.TagIdPayload
import com.myna.mnbt.core.CodecTool

object TagHeadDecoder {

    fun decodeHead(id:Byte, intent: CodecCallerIntent):String? {
        intent as DecodeHead
        if (intent is DecodeOnStream) {
            val name = if (intent.decodeHead) {
                CodecTool.checkNbtFormat(intent.inputStream, id)
                CodecTool.readNbtName(intent.inputStream)
            } else null
            return name
        } else if (intent is DecodeFromBytes) {
            val name = if (intent.decodeHead) {
                CodecTool.checkNbtFormat(intent.data, intent.pointer, id)
                intent.pointer += TagIdPayload
                val pair = CodecTool.readNbtName(intent.data, intent.pointer)
                intent.pointer = pair.second
                pair.first
            } else null
            return name
        }
        TODO()
    }
}