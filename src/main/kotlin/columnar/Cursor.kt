package columnar

import columnar.context.Arity
import columnar.context.Columnar
import columnar.context.Scalar
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext

typealias Cursor = Vect0r<RowVec>
fun cursorOf(root: TableRoot): Cursor = root.let { (nioc: NioCursor, crt: CoroutineContext): TableRoot ->
    nioc.let { (xy, mapper) ->
        xy.let { (xsize, ysize) ->
            /*val rowVect0r: Vect0r<Vect0r<Any?>> =*/ Vect0r({ ysize }) { iy ->
            Vect0r(xsize.`⟲`) { ix ->
                mapper(intArrayOf(ix, iy)).let { (a) ->
                    a() t2 {
                        val cnar = crt[Arity.arityKey] as Columnar
                        //todo define spreadsheet context linkage; insert a matrix of (Any?)->Any? to crt as needed
                        // and call in a cell through here
                        val name =
                            cnar.second?.get(ix) ?: throw(InstantiationError("Tableroot's Columnar has no names"))
                        val type = cnar.first[ix]
                        Scalar(type, name)
                    }
                }
            }
        }
        }
    }
}

fun Cursor.reify() =
    this α RowVec::toList

fun Cursor.narrow() =
    (reify()) α { list: List<Pai2<*, *>> -> list.map(Pai2<*, *>::first) }

inline val <C : Vect0r<R>, reified R> C.`…`:List<R> get() = this.toList()

val Cursor.scalars get() = toSequence().first().right α { it: () -> CoroutineContext -> runBlocking(it()) { coroutineContext[Arity.arityKey] as Scalar } }

@JvmName("vlike_RSequence_11")
operator fun Cursor.get(vararg index: Int) = get(index)

@JvmName("vlike_RSequence_Iterable21")
operator fun Cursor.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_RSequence_IntArray31")
operator fun Cursor.get(index: IntArray) = let { (a, fetcher) ->
    a t2 { iy: Int -> fetcher(iy)[index] }
}
fun daySeq(min: LocalDate, max: LocalDate): Sequence<LocalDate> {
    var cursor = min
    return sequence {
        while (max > cursor) {
            yield(cursor)
            cursor = cursor.plusDays(1)
        }
    }
}

fun Cursor.resample(indexcol: Int) = let {
    val curs = this[indexcol]
    val indexValues = curs.narrow().map { it: List<Any?> -> it.first() as LocalDate }.toSequence()


    val (min, max) = feature_range(indexValues)

    val scalars = this.scalars
    val rowVecSize = scalars.size


    val sequence = daySeq(min, max) - indexValues
    val indexVec = sequence.toVect0r()
    val cursor: Cursor = Cursor(indexVec.first) { iy: Int ->
        RowVec(rowVecSize) { ix: Int ->
            val any = when (ix == indexcol) {
                true -> indexVec[iy]
                else -> null
            }
            any t2 (scalars[ix] as CoroutineContext).`⟲`
        }
    }
    combine(this, cursor)
}

fun feature_range(seq: Sequence<LocalDate>) = seq.fold(LocalDate.MAX t2 LocalDate.MIN) { (a, b), localDate ->
    minOf(a, localDate) t2 maxOf(b, localDate)
}

fun Cursor.pivot(lhs: IntArray, axis: IntArray, fanOut: IntArray) = let { cursr ->

    //    val scalars = this.scalars
//    val targetScalars = this[fanOut].scalars
//    val passthru = this[lhs].scalars
    val keyMeta = this[axis].scalars
    val knames = keyMeta.α { (_, name): Pai2<IOMemento, String?> -> name }
    val keys = (this[axis] α { pai2: Vect02<Any?, () -> CoroutineContext> -> pai2.left })
        .toList()
        .distinct()
    val hashToIndex = keys.mapIndexed { xIndex, any -> any.hashCode() to xIndex }.toMap()
}