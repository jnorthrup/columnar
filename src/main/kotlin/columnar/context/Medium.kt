package columnar.context

import columnar.MappedFile
import columnar.Vect0r
import columnar.context.RecordBoundary.FixedWidth
import columnar.get
import columnar.size
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
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
        val mf: MappedFile,
        val drivers: Vect0r<CellDriver<ByteBuffer, *>>? = null
    ) : Medium() {


        fun asContextVect0r(
            indexable: Addressable.Indexable,
            fixedWidth: FixedWidth,
            state: () -> Pair<ByteBuffer, Pair<Long, Long>> = { Pair(
                ByteBuffer.allocate(
                    0
                ), Pair(-1L, -1L)) }
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
            rafchannel.map(FileChannel.MapMode.READ_WRITE, offsetToMap, sizeToMap)
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