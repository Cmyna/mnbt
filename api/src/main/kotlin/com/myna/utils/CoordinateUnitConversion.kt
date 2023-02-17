package com.myna.utils

/**
 * tools for convert coordinate unit
 */
object CoordinateUnitConversion {

    fun worldBlockPosToWorldChunkPos(i:Int):Int {
        return i shr 4 // i%16
    }

    fun worldChunkPosToWorldBlockPos(i:Int):Int {
        return i*16
    }

    fun worldChunkPosToRegionPos(i:Int):Int {
        return i shr 5 // i%32
    }

    fun regionPosToWorldChunkPos(i:Int):Int {
        return i*32
    }

    fun worldBlockPosToRegionPos(i:Int):Int {
        return i shr 9 // i%(16*32)
    }

    fun regionPosToWorldBlockPos(i:Int):Int {
        return i*16*32
    }

    fun worldChunkPosToLocalChunkPos(i:Int):Int {
        return i and 31
    }


}