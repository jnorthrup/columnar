@file:Suppress("UNCHECKED_CAST", "NOTHING_TO_")

package cursors

import cursors.context.NormalizedRange
import cursors.context.Scalar
import cursors.io.*
import cursors.macros.join
import cursors.ml.featureRange
import cursors.ml.normalize
import vec.macros.*
import java.util.*
import kotlin.Comparator
import kotlin.coroutines.CoroutineContext

/**
 * cursor is approximately the pandas Dataframe with some shortcuts
 *
 * # composition
<compilerPlugins>

</compilerPlugins>
 *
 *  Cursor is a Pair interface, here named Pai2 (also a Tripl3 interface similarly exists)
 *
 *  Cursor and Vect0r are also both the same Pai2 typealias having  properties true for
 *
 *  `.size==.first` and `.get(i)== at .invoke(i)`
 *
 *  Cursor has RowVec which is a Vect02 of value (Any?) and ()->Context method access per column.  you can describe
 *  anything about any Cursor Value by controlling the CoroutineContext herein
 *
 * ## cursor column slices
 * cursor[0] returns a new cursor from column 0
 *
 * DSEL gotcha:
 * cursor[1][0] returns a new cursor from column 1, followed by a new cursor from new column 0 (the old column 1).
 *
 * ## multi column slices
 * `cursor[0,1]` returns a new cursor with columns 0,1 in specified order
 *
 * `cursor[1,0]` returns a new cursor with columns 1,0 in specified order
 *
 * `cursor[2,1,1,2]` returns a new cursor with columns 2,1,1,2 in specified order
 *
 * ## transforms and reducers
 *
 * these operate on all of a cursor's type-safe columns, reading as Any?
 *
 * `cursor.`∑` {reducer}`
 * `cursor.α { pure function }`
 *
 * ###  groupby processing
 * `cursor.group(0,{myreducer})`
 *
 * ## value access
 * cursor at (0) returns rowVec 0  (interchangably mentioned as y=0)
 *
 * # to access the whole cursor x,y plane use
 * `for(i in 0 until cursor.size) cursor at (i)`
 *
 * # column meta
 * `cursor.scalars` requests the type information (not the byte widths) for each column
 *maxMinTwin
 * Cursors are created from within the blackboard state of a CoroutineContext which is accessable from each value
 * by default unless specialized using `RowVec[x] at ()`   within every cursor value is a function`RowVec[i] at `
 * providing the underlying construction factors and potentially cell-specific data.  Generally these are not accessed
 * in DataFrame usecases but this forms the basis for emergent spreadsheet functions on top of cursor state.
 *
 * CoroutineContext access may yet require some caution in kotlin 1.3 around performance overhead
 *
 */
typealias Cursor = Vect0r<RowVec>

// infix fun < T : Int> Cursor.forEach(t: T) = second.invoke(t)
 infix fun < T : Int> Cursor.at(t: T) = second.invoke(t)

@Deprecated("unit testing holdover from prior codebase no longer adds clarity")
fun Cursor.reify() =
        this α RowVec::toList

@Deprecated("unit testing holdover from prior codebase no longer adds clarity")
fun Cursor.narrow() =
        (reify()) α { list: List<Pai2<*, *>> ->
            list.map(
                    Pai2<*, *>::first)
        }

@JvmName("vlike_RSequence_11")
operator fun Cursor.get(vararg index: Int) = get(index)

@JvmName("vlike_RSequence_Iterable21")
operator fun Cursor.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_RSequence_IntArray31")
operator fun Cursor.get(index: IntArray) = let { (a, fetcher) ->
    a t2 { iy: Int -> fetcher(iy)[index] }
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
    val allscalars = cursr.scalars.toArray()

    val fanoutScalars = fanOut.map { fanoutIx: Int ->
        allscalars[fanoutIx]
    }.toTypedArray()

    val synthScalars = keys.keys.map { list: List<Any?> ->
        val synthPrefix: String = list.mapIndexed { index: Int, any: Any? ->
            "${allscalars[axis[index]].second!!}=$any"
        }.joinToString(",", "[", "]")
        fanoutScalars.map { (ioMemento, s: String?): Scalar ->
            Scalar(ioMemento, "$synthPrefix:$s")
        }
    }.flatten().toTypedArray()
    System.err.println("--- pivot")
    cursr.first t2 { iy: Int ->
        val theRow: RowVec = cursr at (iy)
        theRow.let { (_: Int, original: (Int) -> Pai2<Any?, () -> CoroutineContext>): RowVec ->
            RowVec(xsize) { ix: Int ->
                when {
                    ix < lhs.size -> {
                        original(lhs[ix])
                    }
                    else /*fanout*/ -> {
                        val theKey: List<Any?> = theRow[axis].left.toList()
                        val keyGate = whichKey(ix)
                        val cellVal = if (keys[theKey] == keyGate)
                            original(fanOut[whichFanoutIndex(ix)]).first
                        else null

                        cellVal t2 { synthScalars[ix - lhs.size] }
                    }
                }
            }
        }
    }
}


/**
 * this is a helper for comparing keys.
 */
 fun cmpAny(o1: List<Any?>, o2: List<Any?>): Int =
        o1.joinToString(0.toChar().toString()).compareTo(o2.joinToString(0.toChar().toString()))

 fun Cursor.ordered(
        axis: IntArray,
        comparator: Comparator<List<Any?>> = Comparator(::cmpAny)
): Cursor = combine(
        (keyClusters(
                axis,
                comparator.run { TreeMap(comparator) }) `→` MutableMap<List<Any?>, MutableList<Int>>::values α
                (IntArray::toVect0r `⚬` MutableList<Int>::toIntArray)).toVect0r()).let {
    Cursor(it.size) { iy: Int ->
        val ix2 = it[iy]
        this at ix2
    }
}

@JvmName("NormalizeF1")
fun <T : Float> Cursor.normalizeFloatColumn(colName: String) = run {
    val ptype = IOMemento.IoFloat
    val maxMinTwin: Tw1n<Float> = Float.POSITIVE_INFINITY t2 Float.NEGATIVE_INFINITY
    inner_normalize<Float>(colName, maxMinTwin, ptype)
}

@JvmName("NormalizeD1")
fun <T : Double> Cursor.normalizeDoubleColumn(colName: String) = run {
    val ptype = IOMemento.IoDouble
    val maxMinTwin: Tw1n<Double> = Double.POSITIVE_INFINITY t2 Double.NEGATIVE_INFINITY
    inner_normalize<Double>(colName, maxMinTwin, ptype)
}

@JvmName("NormalizeF2")

fun <T : Float> Cursor.inner_normalize(colName: String, maxMinTwin: Tw1n<T>, ptype: IOMemento): Cursor {
    val colIdx = colIdx[colName][0]
    val colCurs = this[colIdx]
    val seq = this.let { curs ->
        sequence {
            for (iy in 0 until curs.size) {
                val any = (colCurs at iy).left[0]
                any.also { }
                yield(any as T)
            }
        }
    }

    val normalizedRange = featureRange<Float>(seq, maxMinTwin as Tw1n<Float>)

    val ctx = (Scalar(ptype, "normalized:$colName") + NormalizedRange(normalizedRange)).`⟲`

    val nprices = join(this[-colName], this[colName].let { c ->
        c.size t2 { iy: Int ->
            val row = (c at iy)
            RowVec(row.size) { ix: Int ->
                val (v) = row[ix]
                normalizedRange.normalize(v as T) t2 ctx
            }
        }
    })
    return nprices
}


@JvmName("NormalizeD2")

fun <T : Double> Cursor.inner_normalize(colName: String, maxMinTwin: Tw1n<T>, ptype: IOMemento): Cursor {
    val colIdx = colIdx[colName][0]
    val colCurs = this[colIdx]
    val seq = this.let { curs ->
        sequence {
            for (iy in 0 until curs.size) {
                val any = (colCurs at iy).left[0]
                any.also { }
                yield(any as T)
            }
        }
    }

    val normalizedRange = featureRange<Double>(seq, maxMinTwin as Tw1n<Double>)

    val ctx = { Scalar(ptype, "normalized:$colName") + NormalizedRange(normalizedRange) }

    val nprices = join(this[-colName], this[colName].let { c ->
        c.size t2 { iy: Int ->
            val row = (c at iy)
            RowVec(row.size) { ix: Int ->
                val (v) = row[ix]
                normalizedRange.normalize(v as T) t2 ctx
            }
        }
    })
    return nprices
}
