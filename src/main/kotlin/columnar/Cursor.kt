@file:Suppress("UNCHECKED_CAST")

package columnar

import columnar.context.*
import kotlinx.coroutines.runBlocking
import java.io.FileWriter
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.time.LocalDate
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext
import kotlin.math.sqrt

typealias Cursor = Vect0r<RowVec>

fun cursorOf(root: TableRoot): Cursor = root.let { (nioc: NioCursor, crt: CoroutineContext): TableRoot ->
    nioc.let { (xy: IntArray, mapper: (IntArray) -> Tripl3<() -> Any?, (Any?) -> Unit, NioMeta>): NioCursor ->
        xy.let { (xsize: Int, ysize: Int): IntArray ->
            Vect0r(ysize) { iy ->
                Vect0r(xsize) { ix ->
                    mapper(intArrayOf(ix, iy)).let { (a: () -> Any?): Tripl3<() -> Any?, (Any?) -> Unit, NioMeta> ->
                        a() t2 {
                            val cnar: Columnar = crt[Arity.arityKey] as Columnar
                            //todo define spreadsheet context linkage; insert a matrix of (Any?)->Any? to crt as needed
                            // and call in a cell through here
                            val name =
                                cnar.second?.get(ix) ?: throw(InstantiationError("Tableroot's Columnar has no names"))
                            val type  = cnar.first[ix]
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

val accnil = Array<Any?>(0) {}
val <C : Vect0r<R>, R> C.`…`: List<R> get() = this.toList()

val Cursor.scalars
    get() = toSequence().first()
        .right α { it: () -> CoroutineContext -> runBlocking(it()) { coroutineContext[Arity.arityKey] as Scalar } }

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
        fanoutScalars.map({ (ioMemento , s: String?): Scalar ->
            Scalar(ioMemento, "$synthPrefix:$s")
        })
    }.flatten().toTypedArray()
    System.err.println("--- pivot")
    cursr.first t2 { iy: Int ->
        val theRow: RowVec = cursr.second(iy)
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

                        cellVal t2 synthScalars[ix - lhs.size].`⟲`
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
infix fun Cursor.α(unaryFunctor: (Any?) -> Any?): Cursor =
    Cursor(first) { iy: Int ->
        val aggcell: RowVec = second(iy)
        (aggcell.left α unaryFunctor).zip(aggcell.right)
    }

fun Cursor.group(
    /**these columns will be preserved as the cluster key.
     * the remaining indexes will be aggregate
     */
    vararg axis: Int
): Cursor = let { orig ->
    val clusters = orig.groupClusters(axis)
    val masterScalars = orig.scalars
    Cursor(clusters.size) { cy ->
        val cluster = clusters[cy]
        RowVec(masterScalars.first) { ix: Int ->
            when (ix) {
                in axis -> orig.second(
                    cluster.first()
                )[ix]
                else -> Vect0r(cluster.size) { iy: Int ->
                    orig.second(cluster[iy])[ix].first
                } t2 masterScalars[ix].`⟲`
            }
        }
    }
}

fun Cursor.group(
    /**these columns will be preserved as the cluster key.
     * the remaining indexes will be aggregate
     */
    axis: IntArray, reducer: ((Any?, Any?) -> Any?)
): Cursor = run {
    val clusters = groupClusters(axis)
    val masterScalars = scalars
    Cursor(
        clusters.size
    ) { cy ->
        val acc1 = arrayOfNulls<Any?>(masterScalars.first)
        val cluster = clusters[cy]
        val valueIndices = acc1.indices - axis.toTypedArray()
        for (i in cluster) {
            val value = this.second(i).left
            for (valueIndex in valueIndices)
                acc1[valueIndex] = reducer(acc1[valueIndex], value[valueIndex])
        }
        RowVec(masterScalars.first) { ix: Int ->
            when (ix) {
                in axis -> this.second(cluster.first())[ix]
                else -> acc1[ix] t2 masterScalars[ix].`⟲`
            }
        }
    }
}

fun Cursor.groupClusters(
    axis: IntArray
) = run {
    val clusters: Map<List<Any?>, MutableList<Int>>
    System.err.println("--- group")
    clusters = linkedMapOf()
    val cap = Math.max(8, (sqrt(first.toDouble()).toInt()))
    val mapIndexed = mapIndexedToList { iy: Int, row: RowVec ->
        row[axis].left.toList().let {
            clusters.get(it).let { clust ->
                if (clust != null) clust.add(iy) else clusters[it] = ArrayList<Int>(cap).apply { add(iy) }
            }
        }
    }
    logDebug { "cap: $cap keys:${clusters.size to clusters.keys /*.also { System.err.println("if this is visible without -ea we have a problem with `⟲`") }*/}" }
    val list = clusters.values α MutableList<Int>::toIntArray
    list
}




fun Cursor.writeBinary(
    pathname: String,
    defaultVarcharSize: Int = 128
) {
    /** create context columns
     *
     */
    val (wcolumnar: Arity, ioMemos: Vect0r<TypeMemento>) = scalars `→` { scalars: Vect0r<Scalar> ->
        Columnar.of(
            scalars
        ) t2 (scalars α Scalar::first)
    }
    val sizes = ioMemos α { memento: TypeMemento ->
        memento.networkSize ?: defaultVarcharSize
    }
    //todo: make IntArray Tw1nt Matrix
    var wrecordlen = 0

    val wcoords = Array(sizes.size) {
        val size = sizes[it]
        Tw1n(wrecordlen, (wrecordlen + size).also { wrecordlen = it })
    }
    System.err.println("wcoords:" + wcoords.toList().map { (a, b) -> a to b })


    MappedFile(pathname, "rw", FileChannel.MapMode.READ_WRITE).use { mappedFile ->
        mappedFile.randomAccessFile.setLength(wrecordlen.toLong() * size)

        /**
         * preallocate the mmap file
         */

        val drivers1: Array<CellDriver<ByteBuffer, Any?>> =
            Fixed.mapped[ioMemos] as Array<CellDriver<ByteBuffer, Any?>>
        val wfixedWidth: RecordBoundary = FixedWidth(
            wrecordlen, wcoords α { tw1nt: Tw1nt -> tw1nt }, null.`⟲`, null.`⟲`
        )
         FileWriter(pathname + ".meta").use {fileWriter->
             fileWriter.appendln("# format: recordlen EOL coords WS .. EOL names WS .. EOL TypeMememento WS ..")
             fileWriter.appendln("$wrecordlen")
             val flatten = wcoords.toList().map { listOf(it.first, it.second) }.flatten().joinToString(" ")
             fileWriter.appendln("" + flatten)
             fileWriter.appendln("" +              scalars.toList().map { scalar: Scalar -> scalar.second }.joinToString(" "))
             fileWriter.appendln("" +              scalars.toList().map { scalar: Scalar -> scalar.first}.joinToString(" "))

         }
        /**
         * nio object
         */
        val wnio: Medium = NioMMap(mappedFile, drivers1)
        wnio.recordLen = wrecordlen.`⟲`
        val windex: Addressable = RowMajor.indexableOf(wnio as NioMMap, wfixedWidth as FixedWidth)


        val wtable: TableRoot = runBlocking(
            windex +
                    wcolumnar +
                    wfixedWidth +
                    wnio +
                    RowMajor()
        ) {
            val wniocursor:NioCursor = wnio.values()
            val coroutineContext1 =  coroutineContext
            val arity = coroutineContext1[Arity.arityKey] as Columnar
            val first = System.err.println("columnar memento: " + arity.first.toList())
            wniocursor t2 coroutineContext1


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
