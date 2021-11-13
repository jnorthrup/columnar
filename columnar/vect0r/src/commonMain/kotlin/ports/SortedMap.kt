package ports

expect interface SortedMap <K,V> {
   val keys: MutableSet<K>
   val entries: MutableSet<MutableMap.MutableEntry<K, V>>
}