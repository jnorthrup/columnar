package ports

actual interface SortedMap<K, V> {
    actual val keys: MutableSet<K>
    actual val entries: MutableSet<MutableMap.MutableEntry<K, V>>
}