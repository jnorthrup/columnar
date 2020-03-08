@file:Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")

package columnar

import columnar.context.*
import columnar.context.RowMajor.Companion.indexableOf
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlin.math.sqrt


inline class MonoVal(val cell: Pai2<Any?, () -> CoroutineContext>) {
    inline fun ctxt(): CoroutineContext = cell.second()
    inline val m: Any? get() = cell.first

}

inline class MonoRow(val row: Vect02<Any?, () -> CoroutineContext>) : Vect0r<MonoVal> {
    override inline val first: Int get() = row.first
    override inline val second: (Int) -> MonoVal get() = { ix: Int -> MonoVal(row.second(ix)) }
}

inline class MonoCursor(val curs: Cursor) : Vect0r<MonoRow> {
    override inline val first: Int get() = curs.first
    override inline val second: (Int) -> MonoRow
        get() = { iy: Int ->
            MonoRow(curs.second(iy))
        }
}
/**
 * cursor is approximately the pandas Dataframe with some shortcuts
 *
 * # composition
 *
 *  Cursor is a Pair interface, here named Pai2 (also a Tripl3 interface similarly exists)
 *
 *  Cursor and Vect0r are also both the same Pai2 typealias having  properties true for
 *
 *  `.size==.first` and `.get(i)==.second.invoke(i)`
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
 * cursor.second(0) returns rowVec 0  (interchangably mentioned as y=0)
 *
 * # to access the whole cursor x,y plane use
 * `for(i in 0 until cursor.size) cursor.second(i)`
 *
 * # column meta
 * `cursor.scalars` requests the type information (not the byte widths) for each column
 *
 * Cursors are created from within the blackboard state of a CoroutineContext which is accessable from each value
 * by default unless specialized using `RowVec[x].second()`   within every cursor value is a function`RowVec[i].second`
 * providing the underlying construction factors and potentially cell-specific data.  Generally these are not accessed
 * in DataFrame usecases but this forms the basis for emergent spreadsheet functions on top of cursor state.
 *
 * CoroutineContext access may yet require some caution in kotlin 1.3 around performance overhead
 *
 */
typealias Cursor = Vect0r<RowVec>

//var ignore = cursorOf(...).α { }

fun cursorOf(root: TableRoot): Cursor = root.let { (nioc: NioCursor, crt: CoroutineContext): TableRoot ->
    val (xy: IntArray, mapper: (IntArray) -> Tripl3<() -> Any?, (Any?) -> Unit, NioMeta>) = nioc
    val (xsize: Int, ysize: Int) = xy
    Vect0r(ysize) { iy ->
        Vect0r(xsize) { ix ->
            val (a: () -> Any?) = mapper(intArrayOf(ix, iy))
            a() t2 {
                val cnar: Columnar = crt[Arity.arityKey] as Columnar
                //todo define spreadsheet context linkage; insert a matrix of (Any?)->Any? to crt as needed
                // and call in a cell through here
                val name =
                    cnar.right.get(ix) ?: throw(InstantiationError("Tableroot's Columnar has no names"))
                val type = cnar.left[ix]
                Scalar(type, name)
            }
        }
    }
}

@Deprecated("unit testing holdover from prior codebase no longer adds clarity")
fun Cursor.reify() =
    this α RowVec::toList

@Deprecated("unit testing holdover from prior codebase no longer adds clarity")
fun Cursor.narrow() =
    (reify()) α { list: List<Pai2<*, *>> -> list.map(Pai2<*, *>::first) }

inline val <reified C : Vect0r<R>, reified R> C.`…`: List<R> get() = this.toList()

inline val Cursor.scalars: Vect0r<Scalar>
    get() = toSequence().first()
        .right α { it: () -> CoroutineContext -> /*runBlocking*/it() `→` { it[Arity.arityKey] as Scalar } }

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

fun Cursor.resample(indexcol: Int): Cursor = let {
    val curs = this[indexcol]
    val indexValues = curs.narrow().map { it.first() as LocalDate }.toSequence()
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
 * prior to this class, pivot works well enough, but leaves an opaque cursor.
 *
 * this will be an available contextElement containing the converted key values.
 */
class PivotInfo(val pivotKey: List<Any?>, val parentContext: () -> CoroutineContext) :CoroutineContext.Element{
    companion object  {
        object pivotInfoKey: CoroutineContext.Key<PivotInfo>
    }
    override val key: CoroutineContext.Key<PivotInfo> =pivotInfoKey
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
        val theRow: RowVec = cursr.second(iy)
        theRow.let { (_: Int, original: (Int) -> Pai2<Any?, () -> CoroutineContext>): RowVec ->
            RowVec(xsize) { ix: Int ->
                when {
                    /** this is a passthru column
                     */
                    ix < lhs.size -> original(lhs[ix])
                    else /*fanout*/ -> {
                        val theKey: List<Any?> = theRow[axis].left.toList()
                        val keyGate = whichKey(ix)
                        val original1 = original(fanOut[whichFanoutIndex(ix)])
                        val cellVal = if (keys[theKey] == keyGate) {
                            original1.first
                        } else null

                        cellVal t2 { PivotInfo(theKey, original1.second) + synthScalars[ix - lhs.size] }
                    }
                }
            }
        }
    }
}


/**
 * reducer func -- operator for sum/avg/mean etc. would be nice, but we have to play nice in a type-safe language so  ∑'s just a hint  of a reducer semantic
 */
inline fun Cursor.`∑`(crossinline reducer: (Any?, Any?) -> Any?): Cursor =
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
inline infix fun Cursor.α(crossinline unaryFunctor: (Any?) -> Any?): Cursor =
    Cursor(first) { iy: Int ->
        val aggcell = second(iy)
        (aggcell.left α (unaryFunctor)).zip(aggcell.right)
    }

inline fun Cursor.group(
    /**these columns will be preserved as the cluster key.
     * the remaining indexes will be aggregate
     */
    vararg axis: Int
): Cursor = let { orig ->
    val clusters = orig.groupClusters(axis)
    val masterScalars = orig.scalars
    Cursor(clusters.size) { cy ->
        val cluster = clusters[cy]
        val cfirst = cluster.first()
        RowVec(masterScalars.first) { ix: Int ->
            when (ix) {
                in axis -> orig.second(cfirst)[ix]
                else -> Vect0r(cluster.size) { iy: Int -> orig.second(cluster[iy])[ix].first } t2 masterScalars[ix].`⟲`
            }
        }
    }
}

inline fun Cursor.group(
    /**these columns will be preserved as the cluster key.
     * the remaining indexes will be aggregate
     */
    axis: IntArray,
    crossinline reducer: ((Any?, Any?) -> Any?)
): Cursor = run {
    val clusters = groupClusters(axis)
    val masterScalars = scalars
    val xSize = masterScalars.first
    Cursor(clusters.size) { cy ->
        val acc1 = arrayOfNulls<Any?>(xSize)
        val cluster = clusters[cy]
        val keyIndex = cluster.first()
        val valueIndices = acc1.indices - axis.toTypedArray()

        for (i in cluster) {
            val value = this.second(i).left
            for (valueIndex in valueIndices)
                acc1[valueIndex] = reducer(acc1[valueIndex], value[valueIndex])
        }
        RowVec(masterScalars.first) { ix: Int ->
            when (ix) {
                in axis -> {
                    this.second(keyIndex)[ix]
                }
                else -> acc1[ix] t2 masterScalars[ix].`⟲`
            }
        }
    }
}

inline fun Cursor.groupClusters(
    axis: IntArray,
    clusters: MutableMap<List<Any?>, MutableList<Int>> = linkedMapOf()
) = run {
    System.err.println("--- groupClusters")
    keyClusters(axis, clusters)
    clusters.values α MutableList<Int>::toIntArray
}

inline fun Cursor.keyClusters(
    axis: IntArray,
    clusters: MutableMap<List<Any?>, MutableList<Int>>
): MutableMap<List<Any?>, MutableList<Int>> = clusters.apply {
    val cap = max(8, sqrt(size.toDouble()).toInt())

    forEachIndexed { iy: Int, row: RowVec ->
        row[axis].left.toList().let {
            getOrPut(it) { ArrayList(cap) } += iy
        }
    }
    logDebug { "cap: $cap keys:${clusters.size to clusters.keys}" }
}


inline fun cmpany(o1: List<Any?>, o2: List<Any?>): Int =
    o1.joinToString(0.toChar().toString()).compareTo(o2.joinToString(0.toChar().toString()))

inline fun Cursor.ordered(axis: IntArray, comparator: Comparator<List<Any?>> = Comparator(::cmpany)) = combine(
    (keyClusters(axis, comparator.let { TreeMap(comparator) }) `→`
            MutableMap<List<Any?>, MutableList<Int>>::values α
            (IntArray::toVect0r `⚬` MutableList<Int>::toIntArray)).toVect0r()
).let { superIndex ->
    Cursor(superIndex.size) { iy: Int ->
        val ix2 = superIndex.get(iy)
        val second1: RowVec = this.second(ix2)
        second1
    }
}

/**
 * this writes a cursor values to a single Network-endian ByteBuffer translation and writes an additional filename+".meta"
 * file containing commented meta description strings
 *
 * this is a tempfile format until further notice, changes and fixes should be aggressively developed, there is no
 * RFC other than this comment to go by
 *
 */
fun Cursor.writeBinary(
    pathname: String,
    defaultVarcharSize: Int = 128,
    varcharSizes: Map<
            /**
            column*/
            Int,
            /**length*/
            Int>? = null
) {
    val mementos = scalars α Scalar::first

    val vec = scalars `→` { scalars: Vect0r<Scalar> ->
        Columnar.of(
            scalars
        ) t2 mementos
    }
    /** create context columns */
    val (wcolumnar: Arity, _: Vect0r<TypeMemento>) = vec
    /** create context columns */
    val (_: Arity, ioMemos: Vect0r<TypeMemento>) = vec
    val wcoords = networkCoords(ioMemos, defaultVarcharSize, varcharSizes)
    val wrecordlen: Int = wcoords.right.last()
    MappedFile(pathname, "rw", FileChannel.MapMode.READ_WRITE).use { mappedFile ->
        mappedFile.randomAccessFile.setLength(wrecordlen.toLong() * size)


        /**
         * preallocate the mmap file
         */

        val drivers1: Array<CellDriver<ByteBuffer, Any?>> =
            Fixed.mapped[ioMemos] as Array<CellDriver<ByteBuffer, Any?>>
        val wfixedWidth: RecordBoundary = FixedWidth(wrecordlen, wcoords, { null }, { null })

        writeMeta(pathname, wcoords)
        /**
         * nio object
         */
        val wnio: Medium = NioMMap(mappedFile, drivers1)
        wnio.recordLen = wrecordlen.`⟲`
        val windex: Addressable = indexableOf(wnio as NioMMap, wfixedWidth as FixedWidth)


        val wtable: TableRoot = /*runBlocking*/(
                windex +
                        wcolumnar +
                        wfixedWidth +
                        wnio +
                        RowMajor()
                ).let { coroutineContext ->
            val wniocursor: NioCursor = wnio.values(coroutineContext)
            val arity = coroutineContext[Arity.arityKey] as Columnar
            System.err.println("columnar memento: " + arity.left.toList())
            wniocursor t2 coroutineContext
        }

        val scalars = scalars
        val xsize = scalars.size
        val ysize = size

        for (y in 0 until ysize) {
            val rowVals = this.second(y).left
            for (x in 0 until xsize) {
                val tripl3 = wtable.first[x, y]
                val writefN = tripl3.second
                val any = rowVals[x]
//                System.err.println("wfn: ($y,$x)=$any")
                writefN(any)
            }
        }
    }
}


fun networkCoords(
    ioMemos: Vect0r<TypeMemento>,
    defaultVarcharSize: Int,
    varcharSizes: Map<Int, Int>?
): Vect02<Int, Int> = Unit.let {
    val sizes1 = networkSizes(ioMemos, defaultVarcharSize, varcharSizes)
    //todo: make IntArray Tw1nt Matrix
    var wrecordlen = 0
    val wcoords = Array(sizes1.size) { ix ->
        val size = sizes1[ix]
        Tw1n(wrecordlen, (wrecordlen + size).also { wrecordlen = it })
    }

    wcoords.map { tw1nt: Tw1nt -> tw1nt.ia.toList() }.toList().flatten().toIntArray().let { ia ->
        Vect02(wcoords.size) { ix: Int ->
            val i = ix * 2
            val i1 = i + 1
            Tw1n(ia[i], ia[i1])
        }
    }
}

fun networkSizes(
    ioMemos: Vect0r<TypeMemento>,
    defaultVarcharSize: Int,
    varcharSizes: Map<Int, Int>?
): Vect0r<Int> = ioMemos.mapIndexed { ix, memento: TypeMemento ->
    val get = varcharSizes?.get(ix)
    memento.networkSize ?: (get ?: defaultVarcharSize)
}


fun Cursor.writeMeta(
    pathname: String,
//    wrecordlen: Int,
    wcoords: Vect02<Int, Int>
) {
    Files.newOutputStream(
        Paths.get(pathname + ".meta")
    ).bufferedWriter().use {

            fileWriter ->

        val s: Vect02<TypeMemento, String?> = scalars as Vect02<TypeMemento, String?>

        val coords = wcoords.toList()
            .map { listOf(it.first, it.second) }.flatten().joinToString(" ")
        val nama = s.right
            .map { s1: String? -> s1!!.replace(' ', '_') }.toList().joinToString(" ")
        val mentos = s.left
            .mapIndexed<TypeMemento, Any> { ix, it -> if (it is IOMemento) it.name else wcoords[ix].size }.toList()
            .joinToString(" ")
        listOf(
            "# format:  coords WS .. EOL names WS .. EOL TypeMememento WS ..",
            "# last coord is the recordlen",
            coords,
            nama,
            mentos
        ).forEach { s ->
            fileWriter.write(s)
            fileWriter.newLine()
        }

    }
}

@Suppress("USELESS_CAST")
fun binaryCursor(
    binpath: Path,
    mappedFile: MappedFile,
    metapath: Path = Paths.get(binpath.toString() + ".meta")
) = mappedFile.run {
    val lines = Files.readAllLines(metapath)
    lines.removeIf { it.startsWith("# ") || it.isNullOrBlank() }
    val rcoords: Vect02<Int, Int> = lines[0].split("\\s+".toRegex()).α(String::toInt).zipWithNext()
    val rnames = lines[1].split("\\s+".toRegex()).toVect0r()
    val typeVec = lines[2].split("\\s+".toRegex()).α(IOMemento::valueOf)
    val recordlen = rcoords.last().second
    val drivers = NioMMap.binary(typeVec)
    val nio = NioMMap(this, drivers)
    val fixedWidth = FixedWidth(recordlen, rcoords, { null }, { null })
    val indexable: Addressable = indexableOf(nio, fixedWidth)
    cursorOf(
        RowMajor().fromFwf(
            fixedWidth,
            indexable as Indexable,
            nio,
            Columnar(typeVec.zip(rnames) as Vect02<TypeMemento, String?>)
        )
    )
}


/***
 *
 * this creates a one-hot encoding set of categories for each value in each column.
 *
 * every distinct (column,value) permutation is reified as (as of this comment, expensive) in-place pivot
 *
 * TODO: staple the catx values to the cursor foreheads
 *
 *
 * maybe this is faster if each (column,value) pair was a seperate 1-column cursor.  first fit for now.
 *
 */
fun Cursor.categories(
    /**
    if this is a value, that value is omitted from columns. by default null is an omitted value.  if this is a DummySpec, the DummySpec specifies the index
     */
    dummySpec: Any? = null
): Cursor = let { curs ->
    val origScalars = curs.scalars
    val xSize = origScalars.size
    val ySize = curs.size
/* todo: vector */
    val sequence = sequence<Cursor> {
        for (catx in 0 until xSize) {
            val cat2 = sequence/* todo: vector */ {
                for (iy in 0 until ySize)
                    yield(curs.second(iy)[0].first)
            }.distinct().toList().let { cats ->
                val noDummies = onehot_mask(dummySpec, cats)
                if (noDummies.first > -1)
                    cats - cats[noDummies.first]
                else
                    cats
            }


            val catxScalar = origScalars[catx]
            yield(Cursor(curs.size) { iy: Int ->
                RowVec(cat2.size) { ix: Int ->
                    val cell = curs.second(iy)[catx]
                    val rowValue = cell.first
                    val diagonalValue = cat2[ix]
                    val cardinal = if (rowValue == diagonalValue) 1 else 0
                    cardinal t2 {
                        /**
                         * there may be context data other than simple scalars in this cell, so we will just replace the scalar key and pass it along.
                         */
                        cell.second() + Scalar(
                            IOMemento.IoInt,
                            origScalars[catx].second + "_" + diagonalValue.toString()
                        )
                    }
                }
            })
        }
    }

    //widthwize join (90 degrees of combine, right?)
    join(sequence.toVect0r())
}


/**
 * Cursors creates one series of values, then optionally omits a column from cats
 */
inline fun onehot_mask(dummy: Any?, cats: List<*>) =
    when {
        dummy is DummySpec ->
            when (dummy) {
                DummySpec.First -> 0 t2 cats.first()
                DummySpec.Last -> cats.lastIndex t2 cats.last()
//                DummySpec.None -> TODO()
            }
        dummy != null -> cats.indexOf(dummy) t2 dummy
        else -> -1 t2 Unit
    }


/**
 * if you specify first or last categories to be the Dummy, this is
 */
enum class DummySpec {
    /*Dummy --  None,*/ First, Last
}

