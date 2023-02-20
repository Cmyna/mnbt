package net.myna.mnbt.utils

import kotlin.random.Random

object RandomValueTool {

    // increase count
    private var c = 1
    val bitStrC:(bitsSize:Int)->()->String = { bitsSize-> {
        val rstr = Random.nextBytes(bitsSize).toString(Charsets.UTF_8)
        c += 1
        "$c$rstr"
    } }
    val intArrC:(arrSize:Int)->()->IntArray = {arrSize->{ IntArray(arrSize) { Random.nextInt()} }}
    val shortArrC:(arrSize:Int)->()->ShortArray = {arrSize->{ ShortArray(arrSize) { Random.nextInt().toShort()} }}
    val byteArrC:(arrSize:Int)->()->ByteArray = {arrSize->{ Random.nextBytes(arrSize)} }
    val longArrC:(arrSize:Int)->()->LongArray = {arrSize->{ LongArray(arrSize) { Random.nextLong()} }}
    val floatArrC:(arrSize:Int)->()->FloatArray = {arrSize->{ FloatArray(arrSize) { Random.nextFloat()} }}
    val doubleArrC:(arrSize:Int)->()->DoubleArray = {arrSize->{ DoubleArray(arrSize) { Random.nextDouble()} }}
    val bintArrC:(arrSize:Int)->()->Array<Int> = {arrSize->{ Array(arrSize) { Random.nextInt()} }}
    val bshortArrC:(arrSize:Int)->()->Array<Short> = {arrSize->{ Array(arrSize) { Random.nextInt().toShort()} }}
    val bbyteArrC:(arrSize:Int)->()->Array<Byte> = {arrSize->{ Random.nextBytes(arrSize).toTypedArray()} }
    val blongArrC:(arrSize:Int)->()->Array<Long> = {arrSize->{ Array(arrSize) { Random.nextLong()} }}
    val bfloatArrC:(arrSize:Int)->()->Array<Float> = {arrSize->{ Array(arrSize) { Random.nextFloat()} }}
    val bdoubleArrC:(arrSize:Int)->()->Array<Double> = {arrSize->{ Array(arrSize) { Random.nextDouble()} }}
}