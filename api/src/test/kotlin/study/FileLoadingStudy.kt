package study

import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileInputStream

class FileLoadingStudy {

    @Test
    fun testLoadRegion() {
        val file = File(this::class.java.getResource("/nbt_data/regions/r.3.4.mca").toURI())
        FileInputStream(file).use {
            val bytes = ByteArray(1024)
            it.read(bytes)
            val str = bytes.fold("[") {last, cur-> "$last,$cur"}
            println(str)
        }
    }
}