package profiling.mnbt

import Tools.withTestDirScene
import net.myna.mnbt.Mnbt
import net.myna.mnbt.tag.CompoundTag
import net.myna.utils.RegionLoader
import net.myna.utils.RegionsLoader
import org.junit.jupiter.api.Test
import java.io.File

@Suppress("SameParameterValue")
class RegionIOProfiling {

    val mnbt = Mnbt()
    @Test
    fun withRegionLoader() {
        withTestDirScene { scene ->
            val testRegion = scene.resolve("r.3.4.mca")
            regionFile.copyTo(testRegion)
            val regionLoader = RegionLoader(testRegion)
            repeat(500) {
                val chunk = decodeTargetTag(testCLX, testCLZ, regionLoader)
                val bytes = mnbt.encode(chunk)
                regionLoader.writeTargetChunkData(testCLX, testCLZ, bytes, RegionsLoader.ZLIB)
            }
        }
    }
    private fun decodeTargetTag(localChunkX:Int, localChunkZ:Int, regionLoader: RegionLoader): CompoundTag {
        val mnbt = Mnbt()
        val (inputStream, _) = regionLoader.getChunkBinaryInputStream(localChunkX, localChunkZ)!!
        return inputStream.use {
            mnbt.decode(it)
        } as CompoundTag
    }


    private val regionFile = File(this::class.java.getResource("/nbt_data/regions/r.3.4.mca").toURI())
    private val testCLX = 16
    private val testCLZ = 3
}