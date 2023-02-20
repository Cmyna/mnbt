package net.myna.utils

import java.io.*
import java.lang.IllegalArgumentException

/**
 * a helper class for loading from/writing to an Region
 */
class RegionsLoader {

    var regionDir: File

    constructor(regionsPath: String) {
        this.regionDir = File(regionsPath)
        checkIsDir()
    }

    constructor(regionDir: File) {
        this.regionDir = regionDir
        checkIsDir()
    }

    private fun checkIsDir() {
        if (!this.regionDir.isDirectory) throw IllegalArgumentException("the path is not an directory!")
    }

    private fun getRegionFile(regionX: Int, regionZ: Int): File? {
        return regionDir.listFiles()?.find { file -> file.name == "r.$regionX.$regionZ.mca" }
    }

    fun getRegionLoaderByRegionPos(regionX: Int, regionZ: Int): RegionLoader? {
        return getRegionFile(regionX, regionZ)?.let { RegionLoader(it) }
    }

    companion object {
        const val segmentByteLength = 4096

        const val ZLIB:Int = 2
        const val GZIP:Int = 1
        const val NO_COMPRESS:Int = 3

        fun getChunkOffsetTableIndex(localChunkX: Int, localChunkZ:Int):Int {
            return ((localChunkX and 31) + (localChunkZ and 31) * 32) * 4
        }

        fun toOffset(data:ByteArray, start:Int = 0):Int {
            if (data.size < start+3) {
                throw IllegalArgumentException("The data passed in can not convert to offset: " +
                        "byte array length no enough(byte array starts at $start, but length only${data.size}, where offset data takes 3 bytes)")
            }
            return (data[start].toUInt() shl 16) or (data[start+1].toUInt() shl 8) or (data[start+2].toUInt())
        }

        private fun Byte.toUInt():Int {
            return this.toInt() and 0xff
        }
    }

}