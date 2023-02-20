package net.myna.utils

import net.myna.Tools
import net.myna.Tools.cleanDir
import net.myna.mnbt.AnyTag
import net.myna.mnbt.Mnbt
import net.myna.mnbt.converter.meta.NbtPathTool
import net.myna.mnbt.tag.CompoundTag
import net.myna.mnbt.tag.ListTag
import net.myna.utils.Extensions.toBytes
import net.myna.utils.RegionLoader
import net.myna.utils.RegionsLoader
import net.myna.utils.RegionsLoader.Companion.NO_COMPRESS
import net.myna.utils.RegionsLoader.Companion.ZLIB
import net.myna.mnbt.utils.ApiTestValueBuildTool
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class RegionsLoaderTest {

    @Test
    fun getChunkDataOffsetTest() {
        assertEquals(0, RegionsLoader.getChunkOffsetTableIndex(0, 0))
        assertEquals(4*16, RegionsLoader.getChunkOffsetTableIndex(16, 0))
        assertEquals(4*(3+32*2), RegionsLoader.getChunkOffsetTableIndex(3, 2))
        assertEquals(4*(32-5), RegionsLoader.getChunkOffsetTableIndex(-5, 0))
        assertEquals(4*(32-31 + 32*(32-7)), RegionsLoader.getChunkOffsetTableIndex(-31, -7))
    }

    @Test
    fun testBytesToOffset() {
        val toBytes:(i:Int)->ByteArray = { i->
            i.toBytes().copyOfRange(1, 4)
        }

        val assertion:(i:Int)->Unit = { i->
            assertTrue(i>=0)
            val bytes = toBytes(i)
            assertEquals(i, RegionsLoader.toOffset(bytes, 0))
        }

        // data between 0-
        assertion(0)
        assertion(25)
        assertion(51137)
        assertion(1988834)
    }

    @Test
    fun testLoadChunk() {
        val regionDir = File(this::class.java.getResource("/nbt_data/regions").toURI())
        val regionLoader = RegionsLoader(regionDir).getRegionLoaderByRegionPos(3, 4)
        // get chunk input stream [16,3]
        val (inputStream, bytesLength) = regionLoader!!.getChunkBinaryInputStream(testCLX, testCLZ)!!
        // try use mnbt to deserialize it
        val mnbt = Mnbt()
        val tag = inputStream.use {
            mnbt.decode(it)
        }
        // assert some data in tag
        assertNotNull(tag)
        assertNotNull((tag as CompoundTag)["Level"])
        assertEquals(4, (NbtPathTool.goto(tag, "./Level/Sections") as ListTag<AnyTag>).value.size)
    }

    @Test
    fun testOverrideChunk() {
        Tools.withTestDirScene { scene ->
            // copy region file
            val testRegion = scene.resolve("r.3.4.mca")
            regionFile.copyTo(testRegion)
            val mnbt = Mnbt()

            run {
                // load test region
                val regionLoader = RegionLoader(testRegion)
                // load test chunk
                val chunk = decodeTargetTag(testCLX, testCLZ, regionLoader)
                // try simple override(not over segment length)
                val comp = CompoundTag("test override tag")
                comp.add(ApiTestValueBuildTool.prepareTag2("test int tag", 5150))
                chunk.add(comp)
                val bytes = mnbt.encode(chunk)
                regionLoader.writeTargetChunkData(testCLX, testCLZ, bytes, ZLIB)
            }

            run {
                // a new region loader load overridden chunk
                val regionLoader = RegionLoader(testRegion)
                val (inputStream, bytesLength) = regionLoader.getChunkBinaryInputStream(testCLX, testCLZ)!!
                val tag = inputStream.use {
                    mnbt.decode(it)
                } as CompoundTag
                assertTrue(tag["test override tag"] is CompoundTag)
                assertEquals(5150, (tag["test override tag"] as CompoundTag)["test int tag"]?.value)

            }
        }
    }

    @Test
    fun testOverrideChunk2() {
        Tools.withTestDirScene { scene ->
            val testRegion = scene.resolve("r.3.4.mca")
            regionFile.copyTo(testRegion)

            val mnbt = Mnbt()

            var freeSegPointer = 0
            var freeSegLen = 0
            val freeSpaceMapField = RegionLoader::class.java.declaredFields.find{ it.name=="freeSpaceMap" }!!
            freeSpaceMapField.trySetAccessible()
            val newX = 0; val newZ = 0

            run {
                val regionLoader = RegionLoader(testRegion)
                // now it is empty
                val rFreeSpaceMap = freeSpaceMapField.get(regionLoader) as Map<Int, Int>
                assertTrue(rFreeSpaceMap.isEmpty())

                val tag = decodeTargetTag(testCLX, testCLZ, regionLoader)
                // try override over original segment capacity
                val comp = CompoundTag("test override tag")
                tag.add(comp)
                val bytes = mnbt.encode(tag)
                regionLoader.writeTargetChunkData(testCLX, testCLZ, bytes, NO_COMPRESS) // just let it not compress

                //reflective way to get freeSegPointer
                assertTrue(rFreeSpaceMap.size==1)
                rFreeSpaceMap.toList().first().also {
                    freeSegPointer = it.first
                    freeSegLen = it.second
                }
                //println("$freeSegPointer $freeSegLen")
            }

            run {
                val regionLoader = RegionLoader(testRegion)
                val tag = decodeTargetTag(testCLX, testCLZ, regionLoader)
                assertTrue(tag["Level"] is CompoundTag)
                assertTrue(tag["test override tag"] is CompoundTag)

                // trying to write to only one free space

                (tag["test override tag"] as CompoundTag).add(ApiTestValueBuildTool.prepareTag2("test string tag", "test string value"))
                val bytes = mnbt.encode(tag)
                regionLoader.writeTargetChunkData(newX, newZ, bytes, ZLIB)
                // reflective way check chunk info segment offset
                val chunksInfoMap = regionLoader::class.java.declaredFields.find { it.name=="chunksInfoMap"}!!.let {
                    it.trySetAccessible()
                    it.get(regionLoader)
                } as Map<RegionLoader.ChunkPosition, RegionLoader.ChunkInfo>
                // RegionLoader has updated to this free space
                val info = chunksInfoMap[RegionLoader.ChunkPosition(newX, newZ)]
                assertEquals(freeSegPointer, info?.segmentOffset)
                assertEquals(freeSegLen, info?.segmentLength)
            }

            run {
                val chunk = decodeTargetTag(newX, newZ, testRegion)
                val testOverrideComp = chunk["test override tag"] as CompoundTag
                assertEquals("test string value", testOverrideComp["test string tag"]?.value)
            }
        }
    }

    private fun decodeTargetTag(localChunkX:Int, localChunkZ:Int, regionLoader: RegionLoader):CompoundTag {
        val mnbt = Mnbt()
        val (inputStream, bytesLength) = regionLoader.getChunkBinaryInputStream(localChunkX, localChunkZ)!!
        return inputStream.use {
            mnbt.decode(it)
        } as CompoundTag
    }

    private fun decodeTargetTag(localChunkX:Int, localChunkZ:Int, testRegionFile:File):CompoundTag {
        return decodeTargetTag(localChunkX, localChunkZ, RegionLoader(testRegionFile))
    }

//    inline fun withTestDirScene(func:(sceneDir:File)->Unit) {
//        val sceneDir = this::class.java.getResource("").toURI().resolve("test_scene").let {
//            println(it)
//            File(it)
//        }
//        cleanDir(sceneDir)
//        if (!sceneDir.isDirectory) sceneDir.mkdir()
//        try {
//            func(sceneDir)
//        } catch (e:Exception) {
//            e.printStackTrace()
//        }
//        // clean directory
//        cleanDir(sceneDir)
//    }

    private val regionFile = File(this::class.java.getResource("/nbt_data/regions/r.3.4.mca").toURI())
    private val testCLX = 16
    private val testCLZ = 3
}