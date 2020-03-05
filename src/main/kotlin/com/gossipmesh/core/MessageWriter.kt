package com.gossipmesh.core

import java.io.IOException

internal interface MessageWriter<T> {
    @Throws(IOException::class)
    fun writeTo(dos: T)
}