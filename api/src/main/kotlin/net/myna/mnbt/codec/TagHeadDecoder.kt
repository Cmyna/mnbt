package net.myna.mnbt.codec

import net.myna.mnbt.TagIdPayload
import net.myna.mnbt.core.CodecTool

object TagHeadDecoder {

    fun decodeHead(id:Byte, intent: CodecCallerIntent):String? {
        if (intent is DecodeOnStream) {
            val name = if (intent is DecodeHead) {
                CodecTool.checkNbtFormat(intent.inputStream, id)
                CodecTool.readNbtName(intent.inputStream)
            } else null
            return name
        } else if (intent is DecodeFromBytes) {
            val name = if (intent is DecodeHead) {
                CodecTool.checkNbtFormat(intent.data, intent.pointer, id)
                intent.pointer += TagIdPayload
                val pair = CodecTool.readNbtName(intent.data, intent.pointer)
                intent.pointer = pair.second
                pair.first
            } else null
            return name
        }
        throw NotImplementedError()
    }
}