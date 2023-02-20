package net.myna.mnbt.presets

/**
 * this object aims to provide format options when deserializing array meta in nbt bin data.
 * because there is no clear definition of format (in wiki it just says it is Tag_Int format),
 * while if it is really a Tag_Int value format, which means negative number is valid.
 *
 * array meta: 4 bytes length to store array length in nbt
 */
object BitsArrayLengthGetter {

    /**
     * default function for getting array length from bits
     */
    fun defaultToInt(data:ByteArray, start:Int):Int {
        var a = 0
        for (i in 3 downTo 0) {
            a = a or ((data[i+start].toInt() and 0xff) shl ((3-i)*8) )
        }
        return a
    }

    /**
     *  regard bits as signed int format, but set it to zero if it is negative
     */
    fun floorNegToInt(data:ByteArray, start:Int):Int {
        val a = defaultToInt(data, start)
        return if (a>=0) a else 0
    }

    /**
     * regard bits format as unsignedInt, except first bit (sign bit in signed Int format)
     */
    fun unsignedToIntWithoutFirstBit(data:ByteArray, start:Int):Int {
        var a = 0
        for (i in 3 downTo 1) {
            a = a or ((data[i+start].toInt() and 0xff) shl ((3-i)*8) )
        }
        // ignore first bit of data
        a = a or ((data[start].toInt() and 0x7f) shl 24)
        return a
    }

 }