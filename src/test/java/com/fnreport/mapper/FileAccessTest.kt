package com.fnreport.mapper


import columnar.*
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

val RowBinEncoder.xinsert: Iterable<xinsert>
    get() = map { it.second.second }

val RowBinEncoder.recordLen get() = this.map { it.second.first.second }.max() ?: 0 + 1
 @UseExperimental(InternalCoroutinesApi::class)
class FileAccessTest : StringSpec() {
    val columns: RowTxtDecoder = listOf("date", "channel", "delivered", "ret").zip(
        listOf((0 to 10), (10 to 84), (84 to 124), (124 to 164))
            .zip(
                listOf(
                    dateMapper,
                    stringMapper,
                    floatMapper,
                    floatMapper
                )
            )
    ).toTypedArray()
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
                val nc: RowBinEncoder = columns.inverse()
                assert(listOf(xInsertLocalDate, xInsertString, xInsertFloat, xInsertFloat) == nc.xinsert)
                assert(nc.size == c4.first.size) { "row element count must be same as column count" }

                val randomAccessFile = mm4/*.randomAccessFile*/
                randomAccessFile.seek(0)
                randomAccessFile.setLength(0)
                val rafchannel = randomAccessFile.channel

                System.err.println("row mappings: " + nc.map { (a, b) -> a to b.let { (c, d) -> c } })
                val rowBuf = ByteBuffer.allocateDirect(nc.recordLen )
                val endl = ByteBuffer.allocateDirect(1).put('\n'.toByte())
                val writeAr = arrayOf(rowBuf, endl)
                c4.f.collect {
                    rowBuf.clear().also {
                        it.duplicate().put(ByteArray(nc.recordLen) { ' '.toByte() })
                    }
                    for ((index, cellValue) in it.withIndex()) {
                        nc[index].let { (_, b) ->
                            b.let { (c, d) ->
                                c.let { (start, end) ->
                                    val d1 = d(rowBuf.position(start).slice().apply { limit(c.size) }, cellValue)
                                }
                            }
                        }
                    }
                    rafchannel.write(writeAr.apply {
                        for (bb in this) {
                            bb.rewind()
                        }} )
                        //byteArrayOf('\n'.toByte()))
                }
            }

            x ()
            println ()
        }
    }
 }
