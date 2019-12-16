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

/*val RowBinEncoder.xinsert: Iterable<xinsert>
    get() = map { it.second.second }*/

typealias RowBinEncoder = Pair<RowNormalizer, Array<Pair<Int?, Function2<ByteBuffer, *, ByteBuffer>>>>

val RowBinEncoder.recordLen
    get() = coords.last().second

val RowBinEncoder.coords
    get() = this.let { (rowNormTriple, binWriter) ->
        var acc = 0
        binWriter.mapIndexed { ix, (hint) ->

            val size = hint ?: rowNormTriple[ix].let { (_, b) -> b.let { (a) -> a.size } }
            acc to acc + size.also { acc = it }
        }
    }

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
                val lineage = c4.first.zip(columns)
                val tmpName = File.createTempFile("fat4", ".fwf").absolutePath
                System.err.println("using " + tmpName)

                val mm4 = RandomAccessFile(tmpName, "rw")
                val rowBinEncoder: RowBinEncoder =
                    columns to binInsertionMapper[columns.map { (_, b, _) -> b.let { (_, f) -> f } }]
//                assert(listOf(xInsertLocalDate, xInsertString, xInsertFloat, xInsertFloat) == nc.xinsert)
                assert(rowBinEncoder.first.size == c4.first.size) { "row element count must be same as column count" }

                mm4.seek(0)
                mm4.setLength(0)
                val rafchannel = mm4.channel

                System.err.println("row mappings: " + c4.first)
                val rowBuf = ByteBuffer.allocateDirect(rowBinEncoder.recordLen)
                val endl = ByteBuffer.allocateDirect(1).put('\n'.toByte())
                val writeAr = arrayOf(rowBuf, endl)
                c4.f.collect {
                    rowBuf.clear().also {
                        it.duplicate().put(ByteArray(rowBinEncoder.recordLen) { ' '.toByte() })
                    }
                    val coords = rowBinEncoder.coords
                    for ((index, cellValue) in it.withIndex()) {
//                  coords[index]

                        rowBinEncoder.let { (_, b) ->
                            b[index].let { (_, d ) ->
                                val c = coords[index]
                                c.let { (start, end) ->
                                    val aligned = rowBuf.position(start).slice().apply { limit(c.size) }
                                    val d1 = (d as (ByteBuffer, Any?)->ByteBuffer)(aligned, cellValue  )
                                }
                            }
                        }
                    }
                    rafchannel.write(writeAr.apply {
                        for (bb in this) {
                            bb.rewind()
                        }
                    })
                    //byteArrayOf('\n'.toByte()))
                }
            }

            x()
            println()
        }
    }
}
