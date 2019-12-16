package com.fnreport.mapper


import arrow.core.none
import columnar.*
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.time.LocalDate


@UseExperimental(InternalCoroutinesApi::class)
class FileAccessTest : StringSpec() {
    val columns: RowNormalizer = listOf("date", "channel", "delivered", "ret").zip(
        listOf((0 to 10), (10 to 84), (84 to 124), (124 to 164))
            .zip(
                listOf(
                    LocalDate::class,
                    String::class,
                    Float::class,
                    Float::class
                )
            )
    ).map { it by none<xform>() }.toTypedArray()
    val f20 = FixedRecordLengthFile("src/test/resources/caven20.fwf")
    val f4 = FixedRecordLengthFile("src/test/resources/caven4.fwf")

    init {
        "things"{
            val x = suspend {
                val c4 = columns reify f4
                val tmpName = File.createTempFile("fat4", ".fwf").absolutePath
                System.err.println("using " + tmpName)

                val columns1 = columns
                c4.toFwf(columns1, tmpName)
            }

            x()
            println()
        }
    }

}
