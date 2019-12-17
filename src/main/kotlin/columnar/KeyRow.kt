package columnar

import arrow.core.some
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.time.LocalDate

typealias KeyRow = Pair<RowNormalizer, Pair<Flow<RowHandle>, Int>>

/**
 * reassign columns
 */
@ExperimentalCoroutinesApi
@JvmName("getKRVA")
operator fun KeyRow.get(vararg axis: Int): KeyRow = get(axis)

@ExperimentalCoroutinesApi
operator fun KeyRow.get(axis: IntArray): KeyRow = this.let { (cols, data) ->
    cols[axis] to
            data.let { (rows, sz) ->
                rows.map { r -> r[axis] } to sz
            }
}

@ExperimentalCoroutinesApi
suspend fun KeyRow.pivot(lhs: IntArray, axis: IntArray, vararg fanOut: Int): KeyRow =
    this.let { (nama, data) ->
        distinct(*axis).let { keys ->
            val xHash = keys.mapIndexed { xIndex, any -> any.contentDeepHashCode() to xIndex }.toMap()
            this.run {

                val (xSize, synthNames) = pivotOutputColumns(fanOut, nama, axis, keys)
                val get = nama.get(lhs)
                val synthNames1 = synthNames
                val synthMasterCopy = combine(get, synthNames1)
                synthMasterCopy to data.let { (rows, sz) ->
                    pivotRemappedValues(
                        rows,
                        lhs,
                        xHash,
                        xSize,
                        axis,
                        fanOut,
                        synthMasterCopy
                    ) to sz
                }
            }
        }
    }

@ExperimentalCoroutinesApi
suspend fun KeyRow.pivot2(lhs: IntArray, axis: IntArray, vararg fanOut: Int): RoutedRows =
    this.let { (nama, data) ->
        distinct(*axis).let { keys ->
            val xHash = keys.mapIndexed { xIndex, any -> any.contentDeepHashCode() to xIndex }.toMap()
            this.run {
                val (xSize, synthNames) = pivotOutputColumns(fanOut, nama, axis, keys)
                val synthMasterCopy = combine(nama.get(lhs), synthNames)
                synthMasterCopy to data.let { (rows, sz) ->
                    pivotRemappedValues(
                        rows,
                        lhs,
                        xHash,
                        xSize,
                        axis,
                        fanOut,
                        synthMasterCopy
                    ).map {
                        it.asSequence()
                    } to sz
                }
            }
        }
    }

@ExperimentalCoroutinesApi
suspend fun KeyRow.distinct(vararg axis: Int) =
    get(axis).let { (_, pair) ->
        pair.let { (flow1, _) ->
            flow1.toList().map(::arrayOfAnys).distinctBy { it.contentDeepHashCode() }
        }
    }

/**
 * cost of one full tablscan
 */
@ExperimentalCoroutinesApi
suspend fun KeyRow.group(vararg by: Int): KeyRow = let {
    val (columns, data) = this
    val (rows) = data
    val protoValues = (columns.indices - by.toTypedArray()).toIntArray()
    val clusters = mutableMapOf<Int, Pair<Array<Any?>, MutableList<Flow<Array<Any?>>>>>()
    rows.collect { row ->
        val key = arrayOfAnys(row.get(by))
        val keyHash = key.contentDeepHashCode()
        flowOf(row.get(protoValues)).let { f ->
            when {
                clusters.containsKey(keyHash) -> clusters[keyHash]!!.second += (f)
                else -> clusters[keyHash] = key to mutableListOf(f)
            }
        }
    }
    columns to (clusters.map { (_, cluster1) ->
        val (key, cluster) = cluster1
        assert(key.size == by.size)
        arrayOfNulls<Any?>(columns.size).also { finale ->

            by.forEachIndexed { index, i ->
                finale[i] = key[index]
            }
            val groupedRow = protoValues.map { arrayOfNulls<Any?>(cluster.size) }.let { cols ->
                for ((ix, group) in cluster.withIndex())
                    group.collectIndexed { _, row ->
                        assert(row.size == protoValues.size)
                        for ((cindex, any) in row.withIndex()) {
                            cols[cindex][ix] = (columns[cindex].third.fold({ any }, { it(any) }))
                        }
                    }
                assert(cols.size == protoValues.size)
                cols
            }
            for ((index, i) in protoValues.withIndex()) finale[i] = groupedRow[index]

        }
    }.asFlow() to clusters.size)
}

@ExperimentalCoroutinesApi
infix fun KeyRow.with(that: KeyRow): KeyRow = let { (theseCols, theseData) ->
    theseData.let { (theseRows, theseSize) ->
        that.let { (thatCols, thatData) ->
            thatData.let { (thatRows, thatSize) ->
                assert(thatSize == theseSize) { "rows must be same -- ${theseSize}!=$thatSize" }
                val unionRows = theseRows.zip(thatRows) { a, b -> combine(a, b) }
                val unionCol = combine(theseCols, thatCols)
                val unionData = unionRows to theseSize
                unionCol to unionData
            }
        }
    }
}

@ExperimentalCoroutinesApi
@FlowPreview
suspend infix fun KeyRow.resample(indexcol: Int) = this[indexcol].let { (_, b) ->
    val (c, _) = b
    val indexValues = c.toList().mapNotNull {
        (it.first() as? LocalDate?)
    }
    val min = indexValues.min()!!
    val max = indexValues.max()!!
    var size = 0
    val empties = (daySeq(min, max) - indexValues).mapIndexed { index, localDate ->
        size = index
        arrayOfNulls<Any?>(first.size).also { row ->
            row[indexcol] = localDate
        }
    }.asFlow()

    let {
        val (rowNormalizer, data) = this
        val (rows, rowCount) = data

        rowNormalizer to (flowOf(rows, empties).flattenConcat() to rowCount + size)
    }
}

operator fun KeyRow.invoke(t: xform): KeyRow = this.let { (a, b) ->
    a.map { (c, e, d) ->
        c to e by (d.fold({ t }, { dprime: xform ->
            { rowval: Any? ->
                t(dprime(rowval))
            }
        })).some()
    }.toTypedArray() to b
}

suspend fun KeyRow.toFwf(tmpName: String): RowBinEncoder {
    val rowBinEncoder: RowBinEncoder = RowBinEncoder(first)

    RandomAccessFile(tmpName, "rw").use { mm4 ->
        mm4.seek(0)
        mm4.setLength(0)
        val rafchannel = mm4.channel

        System.err.println("before row mappings: " + let { (a, _) ->
            arrayOfAnys(a as Array<Any?>).contentDeepToString()
        })
        val coords = rowBinEncoder.coords
        System.err.println("row mappings: " + coords.contentDeepToString())
        val rowBuf = ByteBuffer.allocateDirect(rowBinEncoder.recordLen)
        val endl = ByteBuffer.allocateDirect(1).put('\n'.toByte())
        val writeAr = arrayOf(rowBuf, endl)
        f.collect {
            rowBuf.clear().also {
                it.duplicate().put(ByteArray(rowBinEncoder.recordLen) { ' '.toByte() })
            }
            for ((index, cellValue) in it.withIndex()) {
                //                  coords[index]

                rowBinEncoder.let { (_, b) ->
                    b[index].let { (_, d: Function2<ByteBuffer, *, ByteBuffer>) ->
                        val c = coords[index]
                        c.let { (start, _) ->
                            val aligned = rowBuf.position(start).slice().apply { limit(c.size) }
                            (d as (ByteBuffer, Any?) -> ByteBuffer)(aligned, cellValue)
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
    return rowBinEncoder
}

/**
 *
-XX:MaxDirectMemorySize=<size>
[SO Article]( https://stackoverflow.com/questions/50499238/bytebuffer-allocatedirect-and-xmx ) */

@ExperimentalCoroutinesApi
suspend fun KeyRow.toFwf2(tmpName: String): RowBinEncoder {
    val rowBinEncoder: RowBinEncoder = RowBinEncoder(first)


    val recordLen = rowBinEncoder.recordLen
    val rowsize: Long = (recordLen + 1L)
    val filesize = rowsize * this.let { (_, b) -> b.let { (_, sz) -> sz } }.toLong()
    val windowSize = (Int.MAX_VALUE.toLong() - (Int.MAX_VALUE.toLong() % rowsize))
    val coords = rowBinEncoder.coords
    var window = 0L to 0L
    lateinit var buf: ByteBuffer

    RandomAccessFile(tmpName, "rw").use { mm4 ->
        mm4.seek(0)
        mm4.setLength(filesize)
          mm4.channel.use {rafchannel->
              System.err.println("before row mappings: " + let { (a, _) ->
                  arrayOfAnys(a as Array<Any?>).contentDeepToString()
              })
              System.err.println("row mappings: " + coords.contentDeepToString())
              fun remap(window: Pair<Long, Long>) = window.let { (offsetToMap, sizeToMap) ->
                  rafchannel.map(FileChannel.MapMode.READ_WRITE, offsetToMap, sizeToMap)
              }
              f.collectIndexed { ix, rowFlow ->
                  val lix = ix.toLong()
                  val seekTo = rowsize * lix
                  if (seekTo >= window.second) {
                      val l = seekTo
                      window = l to minOf(filesize-seekTo,  (windowSize))
                      buf = remap(window)
                  }
                  val rowBuf = buf.position(seekTo.toInt() - window.first.toInt()).slice()
                  for ((index, cellValue) in rowFlow.withIndex()) {
                      rowBinEncoder.let { (_, b) ->
                          b[index].let { (_, d) ->
                              coords[index].let { (start) ->
                                  (d)(rowBuf.position(start)/*.slice()*/, cellValue)
                              }
                          }
                      }
                  }
                  rowBuf.put(recordLen.toInt(), '\n'.toByte())
                  if(  ix.rem(100_000) ==0 )
                      System.err.println(
                          listOf(
                              ix,window,rowFlow.let(::arrayOfAnys ).contentDeepToString()

                          )

                      )
              }
          }


    }
    return rowBinEncoder
}

fun RowBinEncoder(rowmeta: RowNormalizer): RowBinEncoder =
    rowmeta.let { columns1 -> columns1 to binInsertionMapper[columns1.map { (_, b, _) -> b.let { (_, f) -> f } }] }
