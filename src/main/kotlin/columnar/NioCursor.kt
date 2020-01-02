package columnar

import columnar.IOMemento.*
import columnar.context.*
import columnar.context.Arity.Companion.arityKey
import columnar.context.NioMMap.Companion.text
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

typealias NioCursor = Matrix<Tr1ple<() -> Any?, (Any?) -> Unit, Tr1ple<CellDriver<ByteBuffer, Any?>, IOMemento, Int>>>
typealias TableRoot = Pair<NioCursor, CoroutineContext>
typealias ColMeta = Pair<String, IOMemento>
typealias RowMeta = Vect0r<ColMeta>
typealias RowVec = Vect0r<Pair</*value*/Any?, /*codexes for origin, metadata and spreadsheet-like functions */ () -> CoroutineContext>>
typealias Cursor = Vect0r<RowVec>

///**
// * reorder just the columns
// */
//operator fun Cursor.get(vararg reorder: Int):Cursor = let { (a, b: (Int) -> RowVec) ->
//    a to { iy: Int ->
//        val before: RowVec = b(iy)
//        val after: RowVec = before[reorder]
//        after
//    }
//}


fun cursorOf(root: TableRoot): Cursor = root.let { (nioc: NioCursor, crt: CoroutineContext): TableRoot ->
    nioc.let { (xy, mapper) ->
        val columnar = crt[arityKey] as Columnar
        xy.let { (xsize, ysize) ->
            /*val rowVect0r: Vect0r<Vect0r<Any?>> =*/ Vect0r({ ysize }) { iy ->
            Vect0r({ xsize }) { ix ->
                mapper(intArrayOf(ix, iy)).let { (a) ->
                    a() to {
                        val cnar = crt[arityKey] as Columnar
                        //todo define spreadsheet context linkage; insert a matrix of (Any?)->Any? to crt as needed
                        // and call in a cell through here
                        EmptyCoroutineContext + Scalar(cnar.type[ix], cnar.names!![ix])
                    }
                }
            }
        }
        }
    }
}

typealias writefn<M, R> = Function2<M, R, Unit>
typealias readfn<M, R> = Function1<M, R>

/**
 * the Cursor attributes appear to be interdependent on each other's advantages.
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
        val nio = NioMMap(mf, text(columnarArity.type))
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

        for (i in 0 until dframe[1].size()) System.err.print("" + dframe[1][i] + "|")
        System.err.println()
        val shaken = (dframe[1])[0, 1, 0, 0, 0, 1, 0, 0, 1, 3, 3, 3, 3, 3]
        System.err.println("reordering: " + shaken.toList())

        val pair = shaken.α({ it: Any? -> "" + it + "____" })
        pair.toSequence().forEach { print(it) }
        val map = shaken.map { it: Any? -> "" + it + "____" }
        map.forEach { print(it) }
        val cursor: Cursor = cursorOf(byRows)
        println()
        println("" + cursor[0].toList())
        println("" + cursor[0][3, 2, 1, 0].toList())
        println("----")
        var c = 0
        val crs11: Vect0r<Vect0r<Pair<Any?, () -> CoroutineContext>>> = cursor
        println(" 0" + crs11[0..3].map { p: RowVec -> p.map(Pair<Any?, () -> CoroutineContext>::first) }.toList())
        val pair1 = cursor[0..3]
        println(" 1" + pair1.map { p: RowVec -> p α (Pair<Any?, () -> CoroutineContext>::first) }.toList())
        println(" 2" + (pair1 α { p: RowVec -> p.map(Pair<Any?, () -> CoroutineContext>::first) }).toList())
        println(" 3" + (pair1 α { p: RowVec -> p α (Pair<Any?, () -> CoroutineContext>::first) }).toList())
        println(" 4" + pair1.map { p: RowVec -> p α (Pair<Any?, () -> CoroutineContext>::first) }.toList())
        println(" 5" + (pair1 α { p: RowVec -> p.map(Pair<Any?, () -> CoroutineContext>::first) }).toList())
        println(" 6" + (pair1 α { p: RowVec -> p α (Pair<Any?, () -> CoroutineContext>::first) }).toList())
        println(" 7" + (pair1 α { p: RowVec -> p.map(Pair<Any?, () -> CoroutineContext>::first) }).toList())
        println(" 8" + (cursor[3, 2, 1, 0] α { p: RowVec -> p.map(Pair<Any?, () -> CoroutineContext>::first) }).toList())
        println("  9" + cursor.narrow())
        println(" 10" + cursor.reify())
        val vect0r1 = cursor.`…`

        println(" 20" + vect0r1)

    }
}

fun Cursor.reify() = narrow().toList()
fun Cursor.narrow() = this.map { it }.map { it.map { it.first } }
inline val <C : Vect0r<R>, reified R> C.`…` get() = this.toList()
inline infix fun <C : List<R>, O, reified R> C.`…reify`(noinline f: (R) -> O) = this.map(f)
inline infix fun <C : Vect0r<R>, O, reified R> C.`…reify`(noinline f: (R) -> O) = this.map(f)
inline infix fun <C : Vect0r<R>, O, reified R : Vect0r<O>> C.`……debug`(noinline f: (R) -> O) = this.map(f).toVect0r()

fun fourBy(nioRoot: TableRoot) = nioRoot.let { (nioCursor) ->
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
) { TableRoot(nio.values() ,   this.coroutineContext) }
