package columnar

import columnar.IOMemento.*
import columnar.context.*
import columnar.context.Arity.Companion.arityKey
import columnar.context.NioMMap.Companion.text
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KMutableProperty2

typealias NioMeta = Tripl3<CellDriver<ByteBuffer, Any?>, IOMemento, Int>
typealias NioCursor = Matrix<Tripl3<() -> Any?, (Any?) -> Unit, NioMeta>>
typealias NioCursor1 = Matrix<KMutableProperty2<NioMeta, ByteBuffer, Any?>>
typealias TableRoot = Pai2<NioCursor, CoroutineContext>
typealias ColMeta = Pai2<String, IOMemento>
typealias RowMeta = Vect0r<ColMeta>
typealias RowVec = Vect02<Any?, () -> CoroutineContext>
fun  stringOf(it:RowVec)=it.left.toList().map {( it as? Vect0r<*>)?.toList()?.toString()?:it }.toString()


typealias   Vect02<F, S> = Vect0r<XY<F, S>>

val <F, S> Vect02<F, S>.left get() = this α Pai2<F, S>::first
val <F, S> Vect02<F, S>.right get() = this α Pai2<F, S>::second
val <F, S> Vect02<F, S>.reify get() = this α Pai2<F, S>::pair


typealias  V3ct0r<F, S, T> = Vect0r<XYZ<F, S, T>>

val <F, S, T> V3ct0r<F, S, T>.x get() = this  α (XYZ<F, S, T>::first)
val <F, S, T> V3ct0r<F, S, T>.y get() = this  α (XYZ<F, S, T>::second)
val <F, S, T> V3ct0r<F, S, T>.z get() = this  α (XYZ<F, S, T>::third)
val <F, S, T> V3ct0r<F, S, T>.r3ify get() = this  α (XYZ<F, S, T>::triple)



typealias writefn<M, R> = Function2<M, R, Unit>
typealias readfn<M, R> = Function1<M, R>

/**
 * the Cursor attributes appear to be interdependent on each other's advantages.
 *
 * if this is to be a  trait system, the functional objects need to look like a blackboard
 */
fun main() {
    val mapping = vect0rOf(
        "date" t2 IoLocalDate,
        "channel" t2 IoString,
        "delivered" t2 IoFloat,
        "ret" t2 IoFloat
    )
    val coords = vect0rOf((0 t2 10), (10 t2 84), (84 t2 124), (124 t2 164)) α { (a, b): Pai2<Int, Int> ->
        intArrayOf(a, b)
    }

    val filename = "src/test/resources/caven4.fwf"
    MappedFile(filename).use { mf ->
        val columnarArity = Columnar.of(mapping)
        val nio = NioMMap(mf, text(columnarArity.first))
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
        val shaken = (dframe[1])[0, 1, 0, 0, 0, 1, 0, 0, 1, 3, 3, 3, 3, 3]
        System.err.println("reordering: " + shaken.toList())

        val pair = shaken.α({ it: Any? -> "" + it + "____" })
        pair.toSequence().forEach { print(it) }

        val cursor: Cursor = cursorOf(byRows)
        println()
        println("" + cursor[0].toList())
        println("" + cursor[0][3, 2, 1, 0].toList())
        println("----")
        var c = 0
        val vect0r1 = cursor/*.`…`*/
        println(" 7" + (vect0r1.reify()).toList())
        println(" 2" + (vect0r1.narrow()).toList())


    }
}

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
    nio.mf.mappedByteBuffer.get().duplicate().clear().run {
        while (get() != endl);
        position()
    }
}, coords = coords)

fun indexableOf(
    nio: NioMMap,
    fixedWidth: FixedWidth,
    mappedByteBuffer: MappedByteBuffer = nio.mf.mappedByteBuffer.get()
): Indexable = Indexable(size = (nio.mf.randomAccessFile.length() / fixedWidth.recordLen)::toInt) { recordIndex ->
    val lim = { b: ByteBuffer -> b.limit(fixedWidth.recordLen) }
    val pos = { b: ByteBuffer -> b.position(recordIndex * fixedWidth.recordLen) }
    val sl = { b: ByteBuffer -> b.slice() }
    mappedByteBuffer `⟲` (lim `⚬` sl `⚬` pos)
}


fun TableRoot.name(xy: IntArray) = this.let { (_, rootContext) ->
    (rootContext[arityKey]!! as Columnar).let { cnar ->
        cnar.second!![(rootContext[Ordering.orderingKey]!! as? ColumnMajor)?.let { xy[1] } ?: xy[0]]
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
) { TableRoot(nio.values(), this.coroutineContext) }


