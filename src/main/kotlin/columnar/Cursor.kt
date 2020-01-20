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

inline fun Cursor.reify() =
    this α RowVec::toList

inline fun Cursor.narrow() =
    (reify()) α { list: List<Pai2<*, *>> -> list.map(Pai2<*, *>::first) }

inline val <C : Vect0r<R>, reified R> C.`…`: List<R> get() = this.toList()

val Cursor.scalars get() = toSequence().first().right α { it: () -> CoroutineContext -> runBlocking(it()) { coroutineContext[Arity.arityKey] as Scalar } }

@JvmName("vlike_RSequence_11")
inline operator fun Cursor.get(vararg index: Int) = get(index)

@JvmName("vlike_RSequence_Iterable21")
inline operator fun Cursor.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_RSequence_IntArray31")
inline operator fun Cursor.get(index: IntArray) = let { (a, fetcher) ->
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

inline fun feature_range(seq: Sequence<LocalDate>) = seq.fold(LocalDate.MAX t2 LocalDate.MIN) { (a, b), localDate ->
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
        (this[axis] α { pai2: Vect02<Any?, () -> CoroutineContext> -> pai2.left.toList() })
            .toList()
            .distinct().mapIndexed { xIndex: Int, any -> any to xIndex }.toMap(linkedMapOf())

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
 * reducer func -- operator for sum/avg/mean etc. would be nice, but we have to play nice in a type-safe language so  ∑'s just a hint  of a reducer semantic
 */
fun Cursor.`∑`(reducer: (Any?, Any?) -> Any?): Cursor =
    Cursor(first) { iy: Int ->
        val aggcell: RowVec = second(iy)
        val al: Vect0r<*> = aggcell.left
        RowVec(aggcell.first) { ix: Int ->
            val ac = al[ix]
            val toList = (ac as? Vect0r<*>)?.toList()
            val iterable = toList ?: (ac as? Iterable<*>)
            val any1 = iterable?.reduce(reducer)
            val any = any1 ?: ac
            any t2 aggcell[ix].second
        }

    }

/**
 * reducer func
 */
inline infix fun Cursor.α(noinline unaryFunctor: (Any?) -> Any?): Cursor =
    Cursor(first) { iy: Int ->
        val aggcell: RowVec = second(iy)
        (aggcell.left α unaryFunctor).zip(aggcell.right)
    }

/**
 * laziest possible groupby
 */
fun Cursor.group(
    /**these columns will be preserved as the cluster key.
     * the remaining indexes will be aggregate
     */
    vararg axis: Int
): Cursor = let { cursr ->
    val clusters = groupClusters(cursr, axis)
    val masterScalars = cursr.scalars
    val indices = 0 until masterScalars.size
    Cursor(clusters.first) { cy: Int ->
        clusters[cy].let { clusterIndices ->
            RowVec(indices.endInclusive.`⟲`) { ix: Int ->
                when (ix) {
                    in axis -> cursr.second(clusterIndices.first())[ix]
                    else -> {
                        fun accessor(clusterOrdinal: Int) = cursr.second(clusterIndices[clusterOrdinal])[ix].first
                        Vect0r(clusterIndices.size.`⟲`, ::accessor) t2 masterScalars[ix].`⟲`
                    }
                }
            }
        }
    }
}

fun Cursor.pgroup(
    /**these columns will be preserved as the cluster key.
     * the remaining indexes will be aggregate
     */
    axis: IntArray, reducer: (Any?, Any?) -> Any?
): Cursor = let { cursr ->
    val clusters = groupClusters(cursr, axis)
    val masterScalars = cursr.scalars
    Cursor(clusters.first) { cy: Int ->
        val keyRowVec:RowVec = cursr.second(cy)//first cluster index is itself
        val acc = arrayOfNulls<Any?>(masterScalars.size)
        val valueIndices = acc.indices - axis.toTypedArray()

        val cluster = clusters[cy]
        for (i in cluster) {
            val value: RowVec = cursr.second(i)
            for (valueIndex in valueIndices)
                acc[valueIndex] = reducer(acc[valueIndex], value[valueIndex])
        }
        RowVec(masterScalars.first) { ix: Int ->
            when (ix) {
                in axis -> {
                    val pai2 = keyRowVec[ix]
                    pai2.first
                }
                else -> acc[ix]
            } t2 masterScalars[ix].`⟲`
        }
    }
}

fun groupClusters(
    cursr: Cursor,
    axis: IntArray
): Vect0r<List<Int>> {
    System.err.println("--- group")

    val clusters: Map<List<Any?>, List<Int>> = linkedMapOf()
    cursr.mapIndexed { iy: Int, row: RowVec ->
        row[axis].left.toList().let { key ->
            clusters.get(key).let { clust ->
                if (clust != null) (clust as MutableList).add(iy) else (clusters as MutableMap)[key] =
                    mutableListOf(iy)
            }
        }
    }
    logDebug { "keys:${clusters.size to clusters.keys/*.also { System.err.println("if this is visible without -ea we have a problem with `⟲`") }*/}" }
    return clusters.values.toVect0r()
}
