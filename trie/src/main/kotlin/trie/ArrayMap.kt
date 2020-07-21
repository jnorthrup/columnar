package trie

import java.util.*


public class ArrayMap<K : Comparable<K>, V>(val entre: Array<Map.Entry<K, V?>>) : Map<K, V?> {
    override val entries: Set<Map.Entry<K, V?>>
        get() = map { (k, v) ->
            object : Map.Entry<K, V?> {
                override val key get() = k
                override val value get() = v
            }
        }.toSet()
    override val size get() = entre.size
    override val keys get() = entre.map(Map.Entry<K, *>::key).toSet()
    override val values get() = entre.map(Map.Entry<K, V?>::value)
    override fun containsKey(key1: K): Boolean {
        val tmpKey = object : Map.Entry<K, V?> {
            override val key: K
                get() = key1
            override val value: V?
                get() = TODO("Not yet implemented")
        }
        return 0 <= Arrays.binarySearch(entre, tmpKey, compareBy(Map.Entry<K, V?>::key))

    }

    override fun containsValue(value: V?) = entre.any { (_, v) -> Objects.equals(value, v) }
    override fun get(key1: K): V? {
        val tmpKey = object : Map.Entry<K, V?> {
            override val key: K
                get() = key1
            override val value: V?
                get() = TODO("Not yet implemented")
        }
        val ix = Arrays.binarySearch(entre, tmpKey, compareBy(Map.Entry<K, V?>::key))
       return  if (ix >= 0) entre[ix].value else null
    }


    override fun isEmpty() = run(entre::isEmpty)
}