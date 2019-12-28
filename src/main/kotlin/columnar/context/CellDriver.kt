package columnar.context

import columnar.*
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.CoroutineContext.Key

  class NioMapper(vararg val drivers: CellDriver<ByteBuffer, *> ): CoroutineContext.Element {
    companion object {
        fun text(vararg m: IOMemento) = CellDriver.Companion.Tokenized.mapped.get( *m) `•` ::NioMapper
        fun binary(vararg m: IOMemento) = CellDriver.Companion.Fixed.mapped.get(*m) `•` ::NioMapper
        val cellmapperKey = object : Key<NioMapper > {}
    }

      override val key: Key<*>  = cellmapperKey
  }

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
                 *
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
                        bb2ba `•` btoa `•` trim,
                        xInsertString
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

        class Fixed<B, R>(val bound: Int, read: readfn<B, R>, write: writefn<B, R>) : CellDriver<B, R>(read, write) {
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