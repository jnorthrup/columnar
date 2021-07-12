package vec.macros

import cursors.io.Vect02_.left
import cursors.io.Vect02_.right
import java.util.*


/**
 * pay once for the conversion from a mutable map to an array map and all that implies
 */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified V> Map<Int, V>.sparseVect0r(): SparseVect0r<V> = let { top ->
    val entries =
        ((this as? SortedMap)?.entries ?: entries.sortedBy { it.key }).toList()
    val sparse: Vect0r<V?> = bindSparse(entries α { (k, v) -> k t2 v })
    SparseVect0r(sparse, entries)
}

inline fun <reified V> bindSparse(
    driver: Vect02<Int, V>
): Vect0r<V?> = driver.let { entres ->
    val k = driver.left.toIntArray()
    0 t2 if (driver.size <= 16) //64 byte cacheline
        { x ->
            var r: V? = null
            if (driver.size > 0) {
                var i = 0
                do {
                    if (k[i] == x)
                        r = entres.right[x]
                    ++i
                } while (i < entres.size && r == null)
            }
            r
        } else { x: Int ->
        k.binarySearch(x).takeUnless {
            0 > it
        }?.let { i -> entres.right[i] }
    }
}

/**
 * massive chimera
 */
@OptIn(ExperimentalStdlibApi::class)
class SparseVect0r<V>(
    private val sparse: Vect0r<V?>,
    private val entries: List<Map.Entry<Int, V>>,
) : Vect0r<V?> by sparse, Iterable<Map.Entry<Int, V>> by (entries) {
    val left: Vect0r<Int> get() = entries α Map.Entry<Int, V>::key
    val right: Vect0r<V> get() = entries α Map.Entry<Int, V>::value
    val keys by this::left
    val values by this::right
}
