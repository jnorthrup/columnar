@file:Suppress("UNCHECKED_CAST")

package columnar

import columnar.context.Arity
import columnar.context.Columnar
import columnar.context.Scalar
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.util.*
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

inline val <C : Vect0r<R>, reified R> C.`…`: List<R> get() = this.toList()

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


    val sequence = daySeq(min, max) - indexValues
    val indexVec = sequence.toVect0r()
    val cursor: Cursor = Cursor(indexVec.first) { iy: Int ->
        RowVec(scalars.first) { ix: Int ->
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

/**
synthesize pivot columns by key(axis) columns present.
 */
fun Cursor.pivot(
    /**
    lhs columns are unmodified from original index inclusive of
    axis and fanout columns
     */
    lhs: IntArray,
    /**
     * these will be used to synthesize columns from values, in order indexed. no dupe limitations apply.
     * using fanout columns in here is a bad idea.
     */
    axis: IntArray,
    /**
     * these will be mapped underneath the axis keys of the source column in the order specified. no dupe limitations apply.
     * using axis columns in here also is a bad idea.
     */
    fanOut: IntArray
): Cursor = let { cursr ->
    val keys: LinkedHashMap<List<Any?>, Int> =
        (this[axis] α { pai2: Vect02<Any?, () -> CoroutineContext> -> pai2.left })
            .toList()
            .distinct().mapIndexed { xIndex: Int, any -> any.toList() to xIndex }.toMap(linkedMapOf())

    val synthSize: Int = fanOut.size * keys.size
    val xsize: Int = lhs.size + synthSize

    fun whichKey(ix: Int): Int = (ix - lhs.size) / fanOut.size
    fun whichFanoutIndex(ix: Int): Int = (ix - lhs.size) % fanOut.size
    val allscalars: Array<Scalar> = cursr.scalars.toArray()

    val fanoutScalars: Array<Scalar> = fanOut.map { fanoutIx: Int ->
        allscalars[fanoutIx]
    }.toTypedArray()

    val synthScalars: List<Scalar> = keys.keys.map { list: List<Any?> ->
        val synthPrefix: String = list.mapIndexed { index: Int, any: Any? ->
            "${allscalars[axis[index]].second!!}=$any"
        }.joinToString(",", "[", "]")
        fanoutScalars.map({ (ioMemento: IOMemento, s: String?): Scalar ->
            Scalar(ioMemento, "$synthPrefix:$s")
        })
    }.flatten()
    System.err.println("--- pivot")
    cursr.first t2 { iy: Int ->
        val theRow: RowVec = cursr.second(iy)
        theRow.let { (sz: () -> Int, original: (Int) -> Pai2<Any?, () -> CoroutineContext>): RowVec ->
            RowVec(xsize.`⟲`) { ix: Int ->
                when {
                    ix < lhs.size -> {
                        original(lhs[ix])
                    }
                    else /*fanout*/ -> {
                        val theKey: List<Any?> = theRow[axis].left.toList()
                        val keyGate: Int = whichKey(ix)
                        val cellVal: Any? = if (keys[theKey] == keyGate)
                            original(fanOut[whichFanoutIndex(ix)]).first/*.also {
                                System.err.println(listOf("+",iy, ix,whichFanoutIndex(ix),fanOut[whichFanoutIndex(ix)]))}*/
                        else null

                        cellVal t2 synthScalars[ix - lhs.size].`⟲`/*.also {
                            System.err.println(listOf("++",iy,ix,theKey,keyGate,cellVal))
                        }*/
                    }
                }
            }
        }
    }
}



/**
 * grouped cursor marked for reducer
 */
inline class GCursor(val cursor: Cursor) {
    operator fun component1() = cursor

}
/**
 * reducer func
 */
operator fun Cursor. invoke(reducer: (Any?, Any?) -> Any?):Cursor =

  Cursor( first)  {iy:Int->
        val aggcell: RowVec =  second(iy)
        val al:Vect0r<*> = aggcell.left
        RowVec(aggcell.first) { ix: Int ->
            val ac = al[ix]
            val pai2 = ac as? Vect0r<*>
            val toList = pai2?.toList()
            val list = toList ?: (ac as? List<*>)
            val any1 = list?.reduce(reducer)
            val any = any1 ?: ac as Any?
            any t2 aggcell[ix].second
        }

}
fun Cursor.group(
    /**these columns will be preserved as the cluster key.
     * the remaining indexes will be aggregate
     */
    axis: SortedSet<Int>
): Cursor = let { cursr ->
    System.err.println("--- group")
    val masterScalars = cursr.scalars
    val indices = masterScalars.toArray().indices
    val axisScalars = cursr[axis].scalars
    val resultIndices = (indices - axis)
    val clusters: LinkedHashMap<List<Any?>, MutableList<Int>> = linkedMapOf()
    cursr.mapIndexed { iy: Int, row: RowVec ->
        row[axis].left.toList().let { key ->
            clusters.get<Any, MutableList<Int>>(key)
                .let { clust ->
                    if (clust != null) clust.add(iy) else clusters[key] = mutableListOf(iy)
                }
        }
    }
    logDebug { "keys:${clusters.size to clusters.keys.also { System.err.println("if this is visible without -ea we have a problem with `⟲`") }}" }
    val clusterVec: Vect0r<MutableMap.MutableEntry<List<Any?>, MutableList<Int>>> = clusters.entries.toVect0r()
    Cursor(clusterVec.size.`⟲`) { cy: Int ->
       clusterVec[cy].let { (_, clusterIndices) ->
           RowVec(indices.endInclusive.`⟲`) { ix: Int ->
               val pai21 = if (ix in axis) {
                   cursr.second(cy)[ix]
               } else (Vect0r(clusterIndices.size.`⟲`) { clusterOrdinal: Int ->
                   cursr.second(clusterIndices[clusterOrdinal])[ix].first
               } t2 { masterScalars[ix] })
               pai21
           }
       }
   }
}


fun join(vararg c:Cursor):Cursor{
    val toVect0r: Vect0r<Cursor>  = c.map { it as Cursor } .toVect0r()
    return join( toVect0r as Vect0r<Cursor>)
}
/*this duck types the first element, and keeps the binary tree mapping for later,*/
fun join(c: Vect0r<Cursor>): Cursor {
    assert(c.toList().map { it.first() }.distinct().size == 1) { "Bad Splice" }
    val sum = c.α { it: Cursor -> it.first() }.toList().sum()
    val sfirst = c.α { it: Cursor -> it.scalars.first() }.toList().sum()
    val (rvsize, rowvecX) = combine(*c.map { it.second(0) }.toArray())


    return Cursor(c[0].first) { iy: Int ->
        RowVec(rvsize, rowvecX)
    }
}