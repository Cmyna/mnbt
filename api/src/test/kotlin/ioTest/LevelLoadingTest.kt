package ioTest

import com.myna.mnbt.IdTagCompound
import com.myna.mnbt.IdTagString
import com.myna.mnbt.annotations.LinkTo
import com.myna.mnbt.reflect.MTypeToken
import mnbt.utils.TestMnbt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileInputStream
import java.util.zip.GZIPInputStream

class LevelLoadingTest {

    @LinkTo("Data", IdTagCompound)
    data class LevelClassForTest(
            val hardcore:Boolean,
            val LevelName:String,
            val BorderCenterX:Double,
            @LinkTo("Version/Name", IdTagString) val name:String,
    )

    @Test
    fun loadByMnbtTest() {
        val testData = object{
            val path = "E:\\game\\minecraftData\\littleTiles1.12.2\\saves\\超大比例别墅\\level.dat"
            val hardcore = false
            val levelName = "超大比例别墅"
            val borderCenterX = 0.0
        }
        val fileStream = FileInputStream(File(testData.path))
        val gzipStream = GZIPInputStream(fileStream)
        TestMnbt.inst.returnObjectContainsNullableProperties = false
        val level = gzipStream.use {
            TestMnbt.inst.fromStream(object: MTypeToken<LevelClassForTest>() {}, gzipStream)
        }!!
        assertEquals("", level.first)
        assertEquals(testData.hardcore, level.second.hardcore)
        assertEquals(testData.levelName, level.second.LevelName)
        assertEquals(testData.borderCenterX, level.second.BorderCenterX, Double.MIN_VALUE)

        StringBuilder().also {
            it.append("object of class LevelClassForTest {\n")
            it.append("\thardcore: ${level.second.hardcore}\n")
            it.append("\tLevelName: ${level.second.LevelName}\n")
            it.append("\tBorderCenterX: ${level.second.BorderCenterX}\n")
            it.append("\tname: ${level.second.name}\n}")
            println(it.toString())
        }
    }
}