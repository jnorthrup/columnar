package trie
import java.util.*

class ArrayMap<K, V>(
    val entre: Array<out Map.Entry<K, V>>,
    val cmp: Comparator<K> = Comparator<K> { o1, o2 ->
        ((o1 as? Comparable<K>)?.compareTo(o2 as K)) ?: o1.toString().compareTo(o2.toString())
    },
) : Map<K, V> {
    override val entries: Set<Map.Entry<K, V>>
        get() = map { (k, v) ->
            object : Map.Entry<K, V> {
                override val key get() = k
                override val value get() = v
            }
        }.toSet()
    override val size get() = entre.size
    override val keys get() = entre.map(Map.Entry<K, *>::key).toSet()
    override val values get() = entre.map(Map.Entry<K, V>::value)
    val comparator = entryComparator<K, V>(cmp)

    override fun containsKey(key1: K) = 0 <= binIndexOf(key1)

    override fun containsValue(value: V) = entre.any { (_, v) -> Objects.equals(value, v) }
    override fun get(key1: K) = binIndexOf(key1).takeIf { it >= 0 }?.let { ix -> entre[ix].value }

    private fun binIndexOf(key1: K) = Arrays.binarySearch(entre, comparatorKeyShim(key1), comparator)

    fun comparatorKeyShim(key1: K): Map.Entry<K, V> = ShimEntry(key1)

    override fun isEmpty() = run(entre::isEmpty)

    companion object {
        fun <K, V> entryComparator(comparator1: Comparator<K>): Comparator<Map.Entry<K, *>> =
            Comparator<Map.Entry<K, *>> { (o1: K), (o2: K) -> comparator1.compare(o1, o2) }

        /**
         * if there aren't guarantees about ordered constructor entries, we can do a quick sort first on the comparator
         */
        fun <K, V> sorting(
            map: Map<K, V>,
            cmp: Comparator<K> = Comparator<K> { o1, o2 -> o1.toString().compareTo(o2.toString()) },
        ): ArrayMap<K, V> =
            ArrayMap(map.entries.sortedWith(entryComparator<K, V>(cmp)).toTypedArray(), cmp)
    }
}

class ShimEntry<K, V>(private val key1: K) : Map.Entry<K, V> {
    override val key: K get() = key1
    override val value: V get() = TODO("Not yet implemented")
}
