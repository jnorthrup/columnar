package com.gossipmesh.core

import java.io.IOException
import java.io.OutputStream

internal class FiniteByteArrayOutputStream(private val buffer: ByteArray) : OutputStream() {
    private var currentIndex = 0

    @Throws(IOException::class)
    override fun write(b: Int) {
        if (currentIndex < buffer.size) {
            buffer[currentIndex++] = b.toByte()
        } else {
            throw FiniteByteArrayOverflowException()
        }
    }

    fun position(): Int {
        return currentIndex
    }

    private class FiniteByteArrayOverflowException : IOException() {
        @Synchronized
        override fun fillInStackTrace(): Throwable {
            return this
        }
    }

}