package columnar

import columnar.IOMemento.*
import columnar.context.*
import columnar.context.Arity.Companion.arityKey
import columnar.context.NioMMap.Companion.text
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import kotlin.coroutines.CoroutineContext

typealias NioCursor = Matrix<Triple<() -> Any?, (Any?) -> Unit, Triple<CellDriver<ByteBuffer, Any?>, IOMemento, Int>>>

typealias writefn<M, R> = Function2<M, R, Unit>
typealias readfn<M, R> = Function1<M, R>


/**
 * the Cursos attributes appear to be interdependent on each other's advantages.
 *
 * if this is to be a  trait system, the functional objects need to look like a blackboard
 */
fun main() {
    val mapping = listOf(
        "date" to IoLocalDate,
        "channel" to IoString,
        "delivered" to IoFloat,
        "ret" to IoFloat
    )
    val coords = vect0rOf((0 to 10), (10 to 84), (84 to 124), (124 to 164)) α { (a, b): Pair<Int, Int> ->
        intArrayOf(a, b)
    }

    val filename = "src/test/resources/caven4.fwf"
    MappedFile(filename).use { mf ->
        val columnarArity = Columnar.of(mapping)
        val nio = NioMMap(mf, text(*columnarArity.type.toArray()))
        val fixedWidth = fixedWidthOf(nio, coords, '\n'::toByte)
        val indexable = indexableOf(nio, fixedWidth)
        val byRows: TableRoot = fromFwf(RowMajor(), fixedWidth, indexable, nio, columnarArity) `→`
                { it: TableRoot -> it.also { fourBy(it) } }

        fromFwf(ColumnMajor(), fixedWidth, indexable, nio, columnarArity).also { nioCursor ->
            fourBy(nioCursor)
        }



        val (a, b) = byRows.first
        val dframe: Vect0r<Vect0r<Any?>> =
            Vect0r({ a[1] }) { iy -> Vect0r({ a[0] }) { ix -> byRows.first[ix, iy].first() } }

        for (i in 0 until dframe[1].size) System.err.print("" + dframe[1][i] + "|")
        System.err.println()
        val shaken = dframe[1][0, 1, 0, 0, 0, 1, 0, 0, 1, 3, 3, 3, 3, 3]
        System.err.println("reordering: " + shaken.toList())

        val pair = shaken α { it: Any? -> "" + it + "____" }
        pair.toSequence().forEach { println(it) }
        val map = shaken.map { it: Any? -> "" + it + "____" }
        map.forEach { println(it) }
    }
}

private fun fourBy(nioRoot: TableRoot) = nioRoot.let { (nioCursor) ->
    System.err.println("|" + nioCursor[3, 3].first() + "|")
    System.err.println("|" + nioCursor[0, 0].first() + "|")
    System.err.println("|" + nioCursor[1, 1].first() + "|")
    System.err.println("|" + nioCursor[0, 0].first() + "|")
    System.err.println("|" + nioCursor[0, 1].first() + "|")
    System.err.println("|" + nioCursor[0, 2].first() + "|")
}

fun fixedWidthOf(
    nio: NioMMap,
    coords: Vect0r<IntArray>,
    defaulteol: () -> Byte = '\n'::toByte
) = FixedWidth(recordLen = defaulteol() `→` { endl: Byte ->
    nio.mf.mappedByteBuffer.duplicate().clear().run {
        while (get() != endl);
        position()
    }
}, coords = coords)

fun indexableOf(
    nio: NioMMap,
    fixedWidth: FixedWidth,
    mappedByteBuffer: MappedByteBuffer = nio.mf.mappedByteBuffer
): Indexable = Indexable(size = (nio.mf.randomAccessFile.length() / fixedWidth.recordLen)::toInt) { recordIndex ->
    val lim = { b: ByteBuffer -> b.limit(fixedWidth.recordLen) }
    val pos = { b: ByteBuffer -> b.position(recordIndex * fixedWidth.recordLen) }
    val sl = { b: ByteBuffer -> b.slice() }
    mappedByteBuffer `⟲` (lim `⚬` sl `⚬` pos)
}

typealias TableRoot = Pair<NioCursor, CoroutineContext>

fun TableRoot.name(xy: IntArray) = this.let { (_, rootContext) ->
    (rootContext[arityKey]!! as Columnar).let { cnar ->
        cnar.names!![(rootContext[Ordering.orderingKey]!! as? ColumnMajor)?.let { xy[1] } ?: xy[0]]
    }
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
): TableRoot = runBlocking(
    ordering +
            fixedWidth +
            indexable +
            nio +
            columnarArity
) { nio.values() to this.coroutineContext }
