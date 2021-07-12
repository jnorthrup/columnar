package vec.macros

import cursors.io.Vect02_.left
import cursors.io.Vect02_.right
import java.util.*


/**
 * pay once for the conversion from a mutable map to an array map and all that implies
 */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified V> Map<Int, V>.sparseVect0r() = let { top ->

    val toTypedArray = ((this as? SortedMap)?.entries ?: entries.sortedBy { it.key }).toTypedArray()
    val sparse = bindSparse (toTypedArray α { (k,v)->k t2 v })
    SparseVect0r(sparse, toTypedArray as Array<Map.Entry<Int, V>>)
}

inline fun <reified V>  bindSparse(
    driver:Vect02<Int,V>
): Pai2<Int, (Int) -> V?> {
    val sparse = driver.let { entres ->
        val k = driver.left.toIntArray()
        0 t2 if (driver.size <= 16)
            { x ->
                var r: V? = null
                if ( driver.size>0) {
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
    return sparse
}

/**
 * massive chimera
 */
class SparseVect0r<V>(
    private val sparse: Vect0r<V ?>,
    private val typedArray: Array<Map.Entry<Int, V>>
) : Vect0r<V?> by sparse, Iterable<Pai2<Int, *>> by (typedArray α { (k, v) -> Pai2<Int, Any?>(k, v) }).`➤`
