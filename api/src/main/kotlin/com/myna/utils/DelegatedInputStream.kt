package com.myna.utils

import java.io.InputStream
import java.io.OutputStream

/**
 * delegator to an inputStream instance
 * because kotlin not support delegate to a class(can only delegate to an interface), so there it is
 */
open class DelegatedInputStream(val inputStream: InputStream): InputStream() {

    override fun read(): Int {
        return inputStream.read()
    }
    override fun available(): Int {
        return inputStream.available()
    }
    override fun close() {
        inputStream.close()
    }
    override fun mark(readlimit: Int) {
        inputStream.mark(readlimit)
    }
    override fun markSupported(): Boolean {
        return inputStream.markSupported()
    }
    override fun read(b: ByteArray): Int {
        return inputStream.read(b)
    }
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return inputStream.read(b, off, len)
    }
    override fun readAllBytes(): ByteArray {
        return inputStream.readAllBytes()
    }
    override fun readNBytes(len: Int): ByteArray {
        return inputStream.readNBytes(len)
    }
    override fun readNBytes(b: ByteArray?, off: Int, len: Int): Int {
        return inputStream.readNBytes(b, off, len)
    }
    override fun reset() {
        inputStream.reset()
    }
    override fun skip(n: Long): Long {
        return inputStream.skip(n)
    }
    override fun skipNBytes(n: Long) {
        inputStream.skipNBytes(n)
    }
    override fun transferTo(out: OutputStream?): Long {
        return inputStream.transferTo(out)
    }
    override fun equals(other: Any?): Boolean {
        return inputStream == other
    }
    override fun hashCode(): Int {
        return inputStream.hashCode()
    }
    override fun toString(): String {
        return inputStream.toString()
    }
}