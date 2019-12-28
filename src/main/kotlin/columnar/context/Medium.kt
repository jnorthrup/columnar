package columnar.context

import columnar.*
import columnar.context.RecordBoundary.FixedWidth
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.time.Instant
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.math.min

sealed class Medium : CoroutineContext.Element {
    override val key: CoroutineContext.Key<Medium> get() = mediumKey
    abstract val seek: (Int) -> Unit
    abstract val size: () -> Long
    abstract val recordLen: () -> Int

    companion object {
        val mediumKey = object :
            CoroutineContext.Key<Medium> {}
    }

    class NioMMap(
        val mf: MappedFile
    ) : Medium() {
        var drivers: Array<CellDriver<ByteBuffer, Any?>>? = null

        companion object {
            fun text(vararg m: IOMemento) =
                CellDriver.Companion.Tokenized.mapped.get(*m) as Array<CellDriver<ByteBuffer, Any?>>

            fun binary(vararg m: IOMemento) =
                CellDriver.Companion.Fixed.mapped.get(*m) as Array<CellDriver<ByteBuffer, Any?>>

            /**
             * CellDriver functions to read and write primitive  state instances to more persistent tiers.
             *
             * struct level abstractions exist without coroutineContext representation.  the structs must be assembled in user space
             * and passed into the context-based machinery for various transforms
             */
            open class CellDriver<B, R>(
                open val read: readfn<B, R>,
                open val write: writefn<B, R>
            ) {
                companion object {
                    class Tokenized<B, R>(read: readfn<B, R>, write: writefn<B, R>) : CellDriver<B, R>(read, write) {
                        companion object {
                            /**coroutineContext derived map of Medium access drivers
                             */

                            val mapped = mapOf(
                                IOMemento.IoInt to Tokenized(
                                    bb2ba `•` btoa `•` trim * String::toInt,
                                    { a, b -> a.putInt(b as Int) }),
                                IOMemento.IoLong to Tokenized(
                                    (bb2ba `•` btoa `•` trim * String::toLong),
                                    { a, b -> a.putLong(b as Long) }),
                                IOMemento.IoFloat to Tokenized(
                                    bb2ba `•` btoa `•` trim `•` String::toFloat,
                                    { a, b -> a.putFloat(b) }),
                                IOMemento.IoDouble to Tokenized(
                                    bb2ba `•` btoa `•` trim `•` String::toDouble,
                                    { a, b -> a.putDouble(b) }),
                                IOMemento.IoString to Tokenized(
                                    bb2ba `•` btoa `•` trim, xInsertString
                                ),
                                IOMemento.IoLocalDate to Tokenized(
                                    bb2ba `•` btoa `•` trim `•` dateMapper,
                                    { a, b -> a.putLong(b.toEpochDay()) }),
                                IOMemento.IoInstant to Tokenized(
                                    bb2ba `•` btoa `•` trim `•` instantMapper,
                                    { a, b -> a.putLong(b.toEpochMilli()) })
                            )
                        }
                    }

                    class Fixed<B, R>(val bound: Int, read: readfn<B, R>, write: writefn<B, R>) :
                        CellDriver<B, R>(read, write) {
                        companion object {
                            /**coroutineContext derived map of Medium access drivers
                             *
                             */
                            val mapped = mapOf(
                                IOMemento.IoInt to Fixed(4, ByteBuffer::getInt, { a, b -> a.putInt(b);Unit }),
                                IOMemento.IoLong to Fixed(8, ByteBuffer::getLong, { a, b -> a.putLong(b);Unit }),
                                IOMemento.IoFloat to Fixed(4, ByteBuffer::getFloat, { a, b -> a.putFloat(b);Unit }),
                                IOMemento.IoDouble to Fixed(8, ByteBuffer::getDouble, { a, b -> a.putDouble(b);Unit }),
                                IOMemento.IoLocalDate to Fixed(
                                    8,
                                    { it.long `•` LocalDate::ofEpochDay },
                                    { a, b: LocalDate -> a.putLong(b.toEpochDay()) }),
                                IOMemento.IoInstant to Fixed(
                                    8,
                                    { it.long `•` Instant::ofEpochMilli },
                                    { a, b: Instant -> a.putLong(b.toEpochMilli()) }),
                                IOMemento.IoString to /*Array-like has no constant bound. */ Tokenized.mapped[IOMemento.IoString]!!
                            )
                        }
                    }
                }
            }
        }

        fun asContextVect0r(
            indexable: Addressable.Indexable,
            fixedWidth: FixedWidth,
            state: () -> Pair<ByteBuffer, Pair<Long, Long>> = {
                Pair(
                    ByteBuffer.allocate(
                        0
                    ), Pair(-1L, -1L)
                )
            }
        ) = Vect0r(indexable.size, { ix ->
            translateMapping(
                ix,
                fixedWidth.recordLen,
                state()
            )
        })

        suspend fun asSequence(): Sequence<ByteBuffer> {
            val indexable = coroutineContext[Addressable.addressableKey]
            val fixedWidth = coroutineContext[RecordBoundary.boundaryKey]

            var state = Pair(ByteBuffer.allocate(0), Pair(-1L, -1L))
            val cvec = asContextVect0r(
                indexable as Addressable.Indexable,
                fixedWidth as FixedWidth,
                { -> state })
            return sequence {
                for (ix in 0 until cvec.size) {
                    state = cvec[ix]
                    yield(state.first)
                }
            }
        }

        /**
         * seek to record offset
         */
        override val seek: (Int) -> Unit = {
            mf.mappedByteBuffer.position(it * recordLen()).slice().limit(recordLen())
        }
        override val size = { mf.randomAccessFile.length() }
        override val recordLen = {
            mf.mappedByteBuffer.duplicate().clear().run {
                run {
                    while (get() != '\n'.toByte());
                    position()
                }
            }
        }
        val windowSize by lazy { Int.MAX_VALUE.toLong() - (Int.MAX_VALUE.toLong() % recordLen()) }


        fun remap(
            rafchannel: FileChannel, window: Pair<Long, Long>
        ) = window.let { (offsetToMap: Long, sizeToMap: Long) ->
            rafchannel.map(mf.mapMode, offsetToMap, sizeToMap)
        }

        /**
         * any seek on a large volume (over MAXINT size) need to be sure there is a mapped extent.
         * this will perform necessary mapping changes to an existing context state.
         *
         * this will also use the context buffer to prepare a rowbuf slice
         *
         * @return
         */
        fun translateMapping(
            rowIndex: Int,
            rowsize: Int, state: Pair<ByteBuffer, Pair<Long, Long>>
        ): Pair<ByteBuffer, Pair<Long, Long>> {
            var (buf1, window1) = state
            val lix = rowIndex.toLong()
            val seekTo = rowsize * lix
            if (seekTo >= window1.second) {
                val recordOffset0 = seekTo
                window1 = recordOffset0 to min(size() - seekTo, windowSize)
                buf1 = remap(mf.channel, window1)
            }
            val rowBuf = buf1.position(seekTo.toInt() - window1.first.toInt()).slice().limit(recordLen())
            return Pair(rowBuf, window1)
        }

    }


    class Kxio : Medium() {
        override val seek: (Int) -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        override val size: () -> Long
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        override val recordLen: () -> Int
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    }

}