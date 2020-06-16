package cursors

import cursors.io.RowVec
import cursors.io.left
import cursors.io.scalars
import kotlinx.coroutines.flow.*
import vec.macros.*
import vec.util.BloomFilter
import vec.util.logDebug
import java.util.ArrayList
import kotlin.math.max
import kotlin.math.sqrt


fun bloomAccess(groupClusters: List<IntArray>): List<Pai2<BloomFilter, IntArray>> {
    return groupClusters.map {
        val n = it.size
        val bloomFilter = BloomFilter(n/*, n * 11*/).apply {
            for (i in it) {
                add(i)
            }
        }
        bloomFilter t2 it
    }
}

inline fun Cursor.group(
        /**these columns will be preserved as the cluster key.
         * the remaining indexes will be aggregate
         */
        vararg axis: Int
): Cursor = let { orig ->
    val clusters = groupClusters(axis)
    val masterScalars = orig.scalars
    Cursor(clusters.size) { cy ->
        val cluster = clusters[cy]
        val cfirst = cluster.first()
        RowVec(masterScalars.first) { ix: Int ->
            when (ix) {
                in axis -> (orig at (cfirst))[ix]
                else -> vec.macros.Vect0r(cluster.size) { iy: Int -> (orig at (cluster[iy]))[ix].first } t2 masterScalars[ix].`⟲`
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