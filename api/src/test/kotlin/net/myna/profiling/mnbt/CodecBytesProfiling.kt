package net.myna.profiling.mnbt

import net.myna.mnbt.Mnbt
import org.junit.jupiter.api.Test
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.GZIPInputStream

class CodecBytesProfiling {

    val file = this::class.java.getResource("/nbt/level.dat").toURI().let { File(it) }

    class Holder(var obj:Any?)

    @Test
    fun decodeTest() {
        val mnbt = Mnbt()
        val bytes = BufferedInputStream(GZIPInputStream(FileInputStream(file))).readAllBytes()
        val holder = Holder(null)
        repeat(1000) {
            val tag = mnbt.decode(bytes, 0)
            holder.obj = tag
        }
        println(holder)
    }
}