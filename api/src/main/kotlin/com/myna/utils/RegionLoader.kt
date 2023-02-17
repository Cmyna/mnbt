package com.myna.utils

import com.myna.utils.Extensions.toBytes
import com.myna.utils.Extensions.toInt
import java.io.*
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.time.Instant
import java.util.zip.*
import kotlin.math.ceil
import kotlin.properties.Delegates

class RegionLoader(private val regionFile: File) {

    private val chunksInfoMap:MutableMap<ChunkPosition, ChunkInfo> = HashMap()

    /**
     * key is the segment pointer, value is free segment length
     */
    private val freeSpaceMap:MutableMap<Int, Int> = HashMap()
    private var segEnd by Delegates.notNull<Int>()

    init {
        FileInputStream(regionFile).use { inputStream ->
            // x,z 0-31
            for (z in 0 until 32) for (x in 0 until 32) {
                val offsetData = ByteArray(3)
                inputStream.read(offsetData)
                val segmentLength = inputStream.read()
                val segmentOffset = RegionsLoader.toOffset(offsetData, 0)
                if (segmentOffset==0 || segmentLength == 0) continue
                chunksInfoMap[ChunkPosition(x, z)] = ChunkInfo(segmentOffset, segmentLength)
            }
        }
        // get free space set
        scanningFreeSpace()
    }

    private fun scanningFreeSpace() {
        // firstly sort the map by pointer index
        val sortedChunksInfo = chunksInfoMap.entries.asSequence().sortedBy { entry->
            entry.value.segmentOffset
        }.toList()
        sortedChunksInfo.scan(0) { pointer,entry->
            val freeSpace = entry.value.segmentOffset - pointer
            if (entry.value.segmentOffset!=2 && freeSpace > 0) {
                freeSpaceMap[pointer] = freeSpace
            }
            entry.value.segmentOffset + entry.value.segmentLength
        }
        val last = sortedChunksInfo.last()
        segEnd = last.value.segmentOffset+last.value.segmentLength
    }

    /**
     * return a BufferedInputStream if the chunk data exists
     * @param localChunkX the local chunk x position
     * @param localChunkZ the local chunk z position
     * @throws IllegalArgumentException if x/z posititon is not in local chunk range(0-31)
     */
    fun getChunkBinaryInputStream(localChunkX:Int, localChunkZ:Int):Pair<BufferedInputStream, Int>? {
        checkIsLocalChunkPos(localChunkX, localChunkZ)
        val chunkInfo = chunksInfoMap[ChunkPosition(localChunkX, localChunkZ)]?: return null
        val randomAccessFile = RandomAccessFile(regionFile, "r")
        randomAccessFile.seek(chunkInfo.segmentOffset.toLong()* RegionsLoader.segmentByteLength)
        val fileInputStream = FileInputStream(randomAccessFile.fd)
        // get chunk data length
        val length = fileInputStream.readNBytes(4).toInt()
        val compressMode = fileInputStream.read()
        return when(compressMode) {
            RegionsLoader.ZLIB -> InflaterInputStream(fileInputStream, Inflater())
            RegionsLoader.NO_COMPRESS -> fileInputStream
            RegionsLoader.GZIP -> GZIPInputStream(fileInputStream)
            else -> throw NotImplementedError("Invalid compress mode: $compressMode")
        }.let { Pair(BufferedInputStream(it), length) }
    }

    /**
     * write target chunk to region file by passed in local xz position.
     * @param localChunkX the local chunk x position
     * @param localChunkZ the local chunk z position
     * @param chunkData the chunk data byte array (haven't been compressed)
     * @param compressFormat the compress format, should be [GZIP],[ZLIB] or [NO_COMPRESS]
     * @throws IllegalArgumentException if x/z posititon is not in local chunk range(0-31)
     */
    fun writeTargetChunkData(localChunkX: Int, localChunkZ: Int, chunkData:ByteArray, compressFormat:Int = RegionsLoader.ZLIB) {
        checkIsLocalChunkPos(localChunkX, localChunkZ)
        when (compressFormat) {
            RegionsLoader.GZIP, RegionsLoader.ZLIB, RegionsLoader.NO_COMPRESS -> {}
            else-> throw IllegalArgumentException("compress format should be 1-3(GZIP,ZLIB,NO_COMPRESS), but got $compressFormat")
        }
        // first compress data
        val bytesOutputStream = ByteArrayOutputStream()
        val compressStream = when(compressFormat) {
            RegionsLoader.GZIP -> GZIPOutputStream(bytesOutputStream)
            RegionsLoader.ZLIB -> DeflaterOutputStream(bytesOutputStream, Deflater())
            RegionsLoader.NO_COMPRESS -> bytesOutputStream
            else -> throw NotImplementedError()
        }
        compressStream.use {
            it.write(chunkData)
        }

        val compressedBytes = bytesOutputStream.toByteArray()
        val compressedBytesLen = compressedBytes.size
        val chunkInfo = chunksInfoMap[ChunkPosition(localChunkX, localChunkZ)]
        val originSegLen = chunkInfo?.segmentLength

        val newSegLen = ceil(compressedBytesLen.toDouble() / RegionsLoader.segmentByteLength).toInt()
        val newSegOffset = if (originSegLen==null || originSegLen<newSegLen) {
            // output to a new enough free space
            val availableEntry = freeSpaceMap.entries.find { (_,segLen)->segLen>=newSegLen }
            availableEntry?.key ?: segEnd
        } else { // output to original place
            chunkInfo.segmentOffset
        }
        val bytesOffset = newSegOffset * RegionsLoader.segmentByteLength

        // random access writing
        val access = RandomAccessFile(regionFile, "rw")
        access.seek(bytesOffset.toLong())
        FileOutputStream(access.fd).also {
            // write head
            it.write(compressedBytesLen.toBytes()) // bytes len
            it.write(compressFormat) // write compress format
            it.write(compressedBytes)
        }

        // update offset
        val chunkInfoByteOffset = RegionsLoader.getChunkOffsetTableIndex(localChunkX, localChunkZ).toLong()
//            val newSegOffset = ceil(compressedBytesLen.toDouble() / segmentByteLength).toInt()
        access.seek(chunkInfoByteOffset)
        FileOutputStream(access.fd).also {
            it.write(newSegOffset.toBytes().copyOfRange(1, 4))
            it.write(newSegLen)
        }
        chunksInfoMap[ChunkPosition(localChunkX, localChunkZ)] = ChunkInfo(newSegOffset, newSegLen)
        // update timestamp
        access.seek(chunkInfoByteOffset+ RegionsLoader.segmentByteLength)
        FileOutputStream(access.fd).use {
            it.write(Instant.now().epochSecond.toBytes())
        }

        // clean original space to free
        scanningFreeSpace()
    }

    fun getChunkSegmentLength(localChunkX: Int, localChunkZ: Int):Int? {
        return chunksInfoMap[ChunkPosition(localChunkX, localChunkZ)]?.segmentLength
    }

    private fun checkIsLocalChunkPos(x:Int, z:Int) {
        val xInLocalPos = x in 0..31
        val zInLocalPos = z in 0..31
        if (!xInLocalPos || !zInLocalPos) {
            StringBuilder("Invalid local chunk position, ").also {
                if (!xInLocalPos) it.append("x pos not in range 0-31 ($x), ")
                if (!zInLocalPos) it.append("x pos not in range 0-31 ($z), ")
                throw IllegalArgumentException(it.toString())
            }
        }
    }

    data class ChunkPosition(val x:Int, val z:Int)
    
    data class ChunkInfo(
            /**
             * the segment offset(count with segment num)
             */
            val segmentOffset:Int,
            val segmentLength:Int)
}