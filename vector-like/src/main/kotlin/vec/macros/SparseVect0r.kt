package vec.macros

import cursors.io.Vect02_.left
import cursors.io.Vect02_.right
import java.util.*


/**
 * pay once for the conversion from a mutable map to an array map and all that implies
 */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified V> Map<Int, V>.sparseVect0r(): SparseVect0r<V> = let { top ->
    val toTypedArray: Array<Map.Entry<Int, V>> =
        ((this as? SortedMap)?.entries ?: entries.sortedBy { it.key }).toTypedArray()
    val sparse: Vect0r<V?> = bindSparse(toTypedArray α { (k, v) -> k t2 v })
    SparseVect0r(sparse, toTypedArray α { (k, v) -> k t2 v })
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
class SparseVect0r<V>(
    private val sparse: Vect0r<V?>,
    private val entries: Vect02<Int, V>
) : Vect0r<V?> by sparse, Iterable<Pai2<Int,V>> by (entries).`➤`/* {
    val left: Vect0r<Int> get() = (entries as Vect02<Int, Any?>).left
    val right: Vect0r<V> get() = (entries as Vect02<Int, Any?>).right as Vect0r<V>
    val keys by this::left
    val values by this::right
}
*/
