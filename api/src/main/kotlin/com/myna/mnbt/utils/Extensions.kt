package com.myna.mnbt.utils

object Extensions {

    fun Short.toBytes():ByteArray {
        return ByteArray(2).also {
            it[0] = (this.toInt() shr 8 and 0xff).toByte()
            it[1] = (this.toInt() and 0xff).toByte()
        }
    }

    fun Int.toBytes():ByteArray = ByteArray(4).also {
        it[0] = ((this shr 24) and 0xff).toByte()
        it[1] = ((this shr 16) and 0xff).toByte()
        it[2] = ((this shr 8) and 0xff).toByte()
        it[3] = (this and 0xff).toByte()
    }

    fun Long.toBytes():ByteArray = ByteArray(8).also { arr->
        repeat(8) { i-> arr[i] = ((this shr (7-i)*8) and 0xff).toByte() }
    }

    fun ByteArray.toInt(start:Int):Int {
        var a = 0
        for (i in 3 downTo 0) {
            a = a or ((this[i+start].toInt() and 0xff) shl ((3-i)*8) )
        }
        return a
    }

    /**
     * @throws IndexOutOfBoundsException if Byte Array length is smaller than 4
     */
    fun ByteArray.toInt():Int = this.toInt(0)

    fun ByteArray.toShort(start:Int):Short {
        var a = 0
        a = a or (this[start+1].toInt() and 0xff)
        a = a or ((this[start].toInt() and 0xff) shl 8)
        return a.toShort()
    }

    fun ByteArray.toLong(start:Int):Long {
        var a:Long = 0;
        repeat(8) {
            a = a or ((this[start+it].toLong() and 0xff) shl ((7-it)*8))
        }
        return a
    }

    fun ByteArray.toString(start:Int, len:Int):String {
        val tmp = ByteArray(len)
        System.arraycopy(this, start, tmp, 0, len)
        return tmp.toString(Charsets.UTF_8)
    }

    fun Number.toBytes():ByteArray {
        return when (this) {
            is Int -> this.toInt().toBytes()
            is Short -> this.toShort().toBytes()
            is Byte -> byteArrayOf(this.toByte())
            is Long -> this.toLong().toBytes()
            is Float -> this.toFloat().toBits().toBytes()
            is Double -> this.toDouble().toBits().toBytes()
            else -> TODO()
        }
    }


    /**
     * this function requires a runtime check, so it needs a Number instance
     */
    fun <T:Number> ByteArray.toBasic(start:Int, inst:T):T {
        return when(inst) {
            is Int -> this.toInt(start)
            is Short -> this.toShort(start)
            is Long -> this.toLong(start)
            is Byte -> this[start]
            is Float -> this.toInt(start).let {Float.fromBits(it)}
            is Double -> this.toLong(start).let {Double.fromBits(it)}
            else -> TODO()
        } as T
    }

}