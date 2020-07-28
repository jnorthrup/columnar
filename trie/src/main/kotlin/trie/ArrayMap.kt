package trie

import java.util.*
import kotlin.Comparator

class ArrayMap<K, V>(val entre: Array<Map.Entry<K, V>>, val cmp: Comparator<K> = Comparator<K> { o1, o2 -> o1.toString().compareTo(o2.toString()) }) : Map<K, V> {

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
    val comparator = Comparator<Map.Entry<K, *>> { o1, o2 -> cmp.compare(o1!!.key, o2!!.key) }
    override fun containsKey(key1: K): Boolean {
        return 0 <= Arrays.binarySearch(entre, comparatorKeyShim(key1), comparator)
    }

    override fun containsValue(value: V): Boolean = entre.any { (_, v) -> Objects.equals(value, v) }
    override fun get(key1: K): V? {
        val ix = Arrays.binarySearch(entre, comparatorKeyShim(key1), comparator)
        return if (ix >= 0) entre[ix].value else null
    }

    fun comparatorKeyShim(key1: K): Map.Entry<K, V> {
        return object : Map.Entry<K, V> {
            override val key: K
                get() = key1
            override val value: V
                get() = TODO("Not yet implemented")
        }
    }

    override fun isEmpty() = run(entre::isEmpty)
}