package columnar

import columnar.IOMemento.*
import columnar.context.*
import columnar.context.NioMMap.Companion.text
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer

typealias NioCursor = Matrix<Triple<() -> Any?, (Any?) -> Unit, Triple<CellDriver<ByteBuffer, Any?>, IOMemento, Int>>>

typealias writefn<M, R> = Function2<M, R, Unit>
typealias readfn<M, R> = Function1<M, R>


/**
 * the Cursos attributes appear to be interdependent on each other's advantages.
 *
 * if this is to be a  trait system, the functional objects need to look like a blackboard
 */
suspend fun main() {
    val mapping = listOf(
        "date" to IoLocalDate,
        "channel" to IoString,
        "delivered" to IoFloat,
        "ret" to IoFloat
    )
    val coords = vect0rOf((0 to 10), (10 to 84), (84 to 124), (124 to 164)).map {
        it.toList().toIntArray()
    }.toVect0r()

    val filename = "src/test/resources/caven4.fwf"
    MappedFile(filename).use { mf ->
        val columnarArity = Columnar.of(mapping)
        val nio = NioMMap(mf, text(*columnarArity.type.toArray()))
        val fixedWidth = fixedWidthOf(nio, coords)
        val indexable = indexableOf(nio, fixedWidth)
        fromFwf(RowMajor(), fixedWidth, indexable, nio, columnarArity).also { nioCursor: NioCursor ->
            fourBy(nioCursor )

        }

        fromFwf(ColumnMajor(), fixedWidth, indexable, nio, columnarArity).also { nioCursor ->
            fourBy(nioCursor )

        }

    }
}

private fun fourBy(
    nioCursor: NioCursor
) {
    System.err.println(nioCursor[3, 3].first())
    System.err.println(nioCursor[0, 0].first())
    System.err.println(nioCursor[1, 1].first())
    System.err.println(nioCursor[0, 0].first())
    System.err.println(nioCursor[0, 1].first())
    System.err.println(nioCursor[0, 2].first())
}

private fun fixedWidthOf(
    nio: NioMMap,
    coords: Vect0r<IntArray>
): FixedWidth {
    val defaulteol = { '\n'.toByte() }
    val fixedWidth = FixedWidth(endl = defaulteol, recordLen = defaulteol() `→` { endl: Byte ->
        nio.mf.mappedByteBuffer.duplicate().clear().run {
            while (get() != endl);
            position()
        }
    }, coords = coords)
    return fixedWidth
}

private fun indexableOf(
    nio: NioMMap,
    fixedWidth: FixedWidth,
    mappedByteBuffer: MappedByteBuffer = nio.mf.mappedByteBuffer
): Indexable {
    val indexable =
        Indexable(size = (nio.mf.randomAccessFile.length() / fixedWidth.recordLen)::toInt) { recordIndex ->
            val lim = { b: ByteBuffer -> fixedWidth.recordLen.let(b::limit) }
            val pos = { b: ByteBuffer -> b.position(recordIndex * fixedWidth.recordLen) }
            val sl = { b: ByteBuffer -> b.slice() }
            mappedByteBuffer.`⟲`(lim `○` sl `○` pos)
        }
    return indexable
}


/**
 * this builds a context and launches a cursor in the given NioMMap frame of reference
 */
fun fromFwf(
    ordering: Ordering,
    fixedWidth: FixedWidth,
    indexable: Indexable,
    nio: NioMMap,
    columnarArity: Columnar
) = runBlocking(
    ordering +
            fixedWidth +
            indexable +
            nio +
            columnarArity
) {
    nio.values()
}