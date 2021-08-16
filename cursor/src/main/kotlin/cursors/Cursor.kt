@file:Suppress("UNCHECKED_CAST", "NOTHING_TO_")

package cursors

import cursors.context.NormalizedRange
import cursors.context.Scalar
import cursors.context.Scalar.Companion.Scalar
import cursors.io.*
import cursors.macros.join
import vec.macros.*
import vec.macros.Vect02_.left
import vec.macros.Vect02_.right
import vec.ml.featureRange
import vec.ml.normalize
import vec.util.rem
import java.util.*
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
 * `cursor[0]` returns a new cursor from column 0
 *
 * DSEL gotcha:
 * `cursor[1][0]` returns a new cursor from column 1, followed by a new cursor from new column 0 (the old column 1).
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
 * * `cursor.`∑` {reducer}`
 * * `cursor.α { pure function }`
 *
 * ###  groupby processing
 * * `cursor.group(0,{myreducer})`
 *
 * ## value access
 * * cursor at (0) returns rowVec 0  (interchangably mentioned as y=0)
 *
 * # to access the whole cursor x,y plane use
 * * `for(i in 0 until cursor.size) cursor at (i)`
 *
 * # column meta
 * * `cursor.scalars` requests the type information (not the byte widths) for each column
 *
 *
 * maxMinTwin:
 *   Cursors are created from within the blackboard state of a CoroutineContext which is accessable from each value
 *   by default unless specialized using `RowVec`[`x] at ()`   within every cursor value is a function`RowVec`[`i] at `
 *   providing the underlying construction factors and potentially cell-specific data.  Generally these are not accessed
 *   in DataFrame usecases but this forms the basis for emergent spreadsheet functions on top of cursor state.
 *
 *
 */
typealias Cursor = Vect0r<RowVec>

// infix fun < T : Int> Cursor.forEach(t: T) = second.invoke(t)
infix fun <T : Int> Cursor.at(t: T) = second.invoke(t)

@Deprecated("unit testing holdover from prior codebase no longer adds clarity")
fun Cursor.reify() = combine(this)

@Deprecated("unit testing holdover from prior codebase no longer adds clarity")
fun Cursor.narrow() = combine(this).left

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
    fanOut: IntArray,
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
        xsize t2 { ix: Int ->
            when {
                ix < lhs.size -> {
                    theRow.second(lhs[ix])
                }
                else /*fanout*/ -> {
                    val theKey = theRow[axis].left.toList() //expressly for toString and equality tests
                    val keyGate = whichKey(ix)
                    val cellVal = (keyGate == keys[theKey]) % fanOut[whichFanoutIndex(ix)].let {
                        theRow.second(it).first
                    }



                    cellVal t2 { synthScalars[ix - lhs.size] }
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
    comparator: Comparator<List<Any?>> = Comparator(::cmpAny),
): Cursor = combine(
    (keyClusters(
        axis,
        comparator.run { TreeMap(comparator) }) `→` MutableMap<List<Any?>, MutableList<Int>>::values α
            (IntArray::toVect0r `⚬` MutableList<Int>::toIntArray))
).let {
    Cursor(it.size) { iy: Int ->
        val ix2 = it[iy]
        this at ix2
    }
}

@JvmName("NormalizeF1")
fun <T : Float> Cursor.normalizeFloatColumn(colName: String) = run {
    val ptype = IOMemento.IoFloat
    val maxMinTwin: Tw1n<Float> = Float.POSITIVE_INFINITY t2 Float.NEGATIVE_INFINITY
    inner_normalize(colName, maxMinTwin, ptype)
}

@JvmName("NormalizeD1")
fun <T : Double> Cursor.normalizeDoubleColumn(colName: String) = run {
    val ptype = IOMemento.IoDouble
    val maxMinTwin: Tw1n<Double> = Double.POSITIVE_INFINITY t2 Double.NEGATIVE_INFINITY
    inner_normalize<Double>(colName, maxMinTwin, ptype)
}

@JvmName("NormalizeF2")
fun <T : Float> Cursor.inner_normalize(colName: String, maxMinTwin: Tw1n<T>, ptype: IOMemento) = run {
    val colIdx = colIdx[colName][0]
    val colCurs = this[colIdx]
    val seq = this.let { curs ->
        sequence {
            for (iy in 0 until curs.size) {
                yield((colCurs at iy).left[0] as T)
            }
        }
    }

    val normalizedRange = featureRange<Float>(seq.asIterable(), maxMinTwin as Tw1n<Float>)

    val ctx = (Scalar(ptype, "normalized:$colName") + NormalizedRange(normalizedRange)).`⟲`

    join(this[-colName], this[colName].let { c ->
        c.size t2 { iy: Int ->
            val row = (c at iy)
            RowVec(row.size) { ix: Int ->
                val (v) = row[ix]
                normalizedRange.normalize(v as T) t2 ctx
            }
        }
    })

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

    val normalizedRange = featureRange<Double>(seq.asIterable(), maxMinTwin as Tw1n<Double>)

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

/**
 * returns cursor with x reversed
 */
fun Cursor.mirror(): Cursor = Cursor(first) { y: Int ->
    second(y).let { (xsz, fn) ->
        xsz t2 { x: Int -> fn(xsz - x) }
    }
}

/**
 * "left/right" is blocked by precedence magic in Vect02_ so we have to treat cursors with a perturbed set of keywords like f1rst and whatnot.
 *
 *  - Pai2 uses unaryMinus to make an array.
 *  Cursor should not be reifying anything but unaryMinus as vect0r is plenty good and cheap
 */
operator fun Cursor.unaryMinus(): Vect0r<Vect0r<Any?>> = size t2 { x: Int -> second(x).left }

/** positionally preserves the objects of type t as v<v<T?>>
 */
inline infix operator fun <reified T> Cursor.div(t: Class<T>): Vect0r<Vect0r<T?>> =
    -this α { outer -> outer α { inner -> inner as? T } }

/**  flattens cursor to Vect0r of T? preserving order/position */
inline infix operator fun <reified T> Cursor.rem(t: Class<T>) = combine(this / t)

/**
 * "left/right" is blocked by precedence magic in Vect02_ so we have to treat cursors with a perturbed set of keywords like f1rst and whatnot.
 *
 */
inline val Cursor.meta get() = size t2 { x: Int -> second(x).right }
