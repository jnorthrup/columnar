package cursors

import cursors.context.Scalar
import cursors.hash.md4
import cursors.io.*
import trie.ArrayMap
import vec.macros.*
import vec.macros.Vect02_.left
import vec.macros.Vect02_.right
import vec.util.BloomFilter
import java.util.*
import kotlin.math.max
import kotlin.math.sqrt


fun bloomAccess(groupClusters: Iterable<IntArray>)  = groupClusters α {
    val n = it.size
    val bloomFilter = BloomFilter(n/*, n * 11*/).apply {
        for (i in it) {
            add(i)
        }
    }
    bloomFilter t2 it
}


/**
 * group on indexes created using the columnNames of the cursor passed in
 *
 */
fun Cursor.group(
    /**these columns will be preserved as the cluster key.
     * the remaining indexes will be aggregate.
     *
     * setting the precedent here where curs[[-]foo"] is adequate to convey a longer scala2s extraction
     */
    axis: Cursor,
): Cursor = group(*colIdx.get(*axis.colIdx.right.toList().filterNotNull().toTypedArray()).toTypedArray().toIntArray())


fun Cursor.group(
    /**these columns will be preserved as the cluster key.
     * the remaining indexes will be aggregate
     */
    vararg axis: Int,
): Cursor = let { orig ->
    val clusters = groupClusters(axis)
    val masterScalars = orig.scalars
    Cursor(clusters.size) { cy ->
        val cluster = clusters[cy]
        val cfirst = cluster.first()
        RowVec(masterScalars.first) { ix: Int ->
            when (ix) {
                in axis -> (orig at (cfirst))[ix]
                else -> vec.macros.Vect0r(cluster.size) { iy: Int -> (orig at (cluster[iy]))[ix].first } t2 { masterScalars[ix] }
            }
        }
    }
}

inline fun Cursor.group(
    /**these columns will be preserved as the cluster key.
     * the remaining indexes will be aggregate
     */
    axis: IntArray,
    crossinline reducer: ((Any?, Any?) -> Any?),
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
            val value = (this at (i)).left
            for (valueIndex in valueIndices)
                acc1[valueIndex] = reducer(acc1[valueIndex], value[valueIndex])
        }
        RowVec(masterScalars.first) { ix: Int ->
            if (ix in axis) {
                (this at (keyIndex))[ix]
            } else acc1[ix] t2 masterScalars[ix].`⟲`
        }
    }
}


/**
 * Performs  [Cursor.keyClusters] and returns trimmed values for group clusters
 */
fun Cursor.groupClusters(
    axis: IntArray,
    clusters: MutableMap<List<Any?>, MutableList<Int>> = linkedMapOf(),
) = run {
    System.err.println("--- groupClusters")
    keyClusters(axis, clusters)
    clusters.values α MutableList<Int>::toIntArray
}

/**
 * grows a list for each cluster key using a bit of a guess on median capacity -- does not trim the clusters.
 */
fun Cursor.keyClusters(
    axis: IntArray,
    clusters: MutableMap<List<Any?>, MutableList<Int>>,
): MutableMap<List<Any?>, MutableList<Int>> = clusters.apply {
    val cap = max(8, sqrt(size.toDouble()).toInt())
    forEachIndexed { iy: Int, row: RowVec ->
        row[axis].left.toList().let {
            getOrPut(it) { ArrayList(cap) } += iy
        }
    }
//    logDebug { "cap: $cap keys:${clusters.size to clusters.keys}" } been stable for a while
}

/**
 * ordered keys of ordered cluster indexes trimmed.
 */
fun Cursor.mapClusters(axis: IntArray) =
    keyClusters(axis, linkedMapOf()).entries.map { (k: List<Any?>, v: MutableList<Int>) ->
        k to v.toIntArray()
    }.toMap(linkedMapOf())


/**
 * /primary/ key mapping.  collision behaviors are map-defined
 * produces more idealized hash bucket distributions
 */
fun Cursor.mapOnColumnsMd4(vararg colNames: String): Map<String, Int> = run {
    val kix = colIdx.get(*colNames)
    val index = this[kix]
    Array(size) {
        (index at it).run {
            index.left.toList().md4 to it
        }
    }.toMap()
}

/**
 * /primary/ key mapping.  collision behaviors are map-defined
 *
 */
fun Cursor.mapOnColumns(vararg colNames: String) = let {
    val kix = colIdx.get(*colNames)
    val index = this[kix]
    val scalars: Vect0r<Scalar> = index.scalars
    val map = (scalars as Vect02<TypeMemento, String?>).left.toArray().map { IOMemento::cmp }
    Array(size) { iy ->
        (index at iy).let { row: RowVec ->
            row.left.toList() to iy
        }
    }.toMap()
}

/**
 * /primary/ key mapping.  collision behaviors are map-defined
 *
 */
fun Cursor.arrayMapOnColumns(vararg colNames: String) = let {
    val kix = colIdx.get(*colNames)
    val index = this[kix]
    val scalars: Vect0r<Scalar> = index.scalars
    val map = (scalars as Vect02<TypeMemento, String?>).left.toArray().map { IOMemento.cmp(it) }
    val cmp = Comparator { l1: List<*>, l2: List<*> ->
        var res = 0
        var ix = 0
        while (res == 0 && ix < map.size) {
            res = map[ix].invoke(l1[ix], l2[ix])
            ix++
        }
        res
    }
    val comparator = Comparator<Pair<List<*>, Int>> { o1, o2 ->
        val (l1) = o1
        val (l2) = o2
        cmp.compare(l1, l2)
    }

    val toTypedArray: Array<Pair<List<Any?>, Int>> = Array(size) { iy ->
        (index at iy).let { row: RowVec ->
            row.left.toList() to iy
        }
    }.toSortedSet(comparator).toTypedArray()


    val entre: Array<Map.Entry<List<Any?>, Int>> = toTypedArray.map { (a, b) ->
        object : Map.Entry<List<Any?>, Int> {
            override val key get() = a
            override val value get() = b
        }
    }.toTypedArray()
    ArrayMap(entre, cmp)
}

/**
 * /primary/ key mapping.  collision behaviors are map-defined
 *
 */
fun Cursor.trieOnColumns(vararg colNames: String) = let {
    val kix = colIdx.get(*colNames)
    val index = this[kix]
    trie.Trie().apply {
        repeat(size) { iy ->
            add(iy, *(index at iy).left.α(Any?::toString).toArray())
        }
    }/* only for short paths...*/
        .also { it.freeze() }
}
