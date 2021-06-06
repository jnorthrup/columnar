Prefix Tree / Radix Tree / Trie
===============================

Generic Trie implementation in Kotlin.

Information about Tries can be found [here](https://en.wikipedia.org/wiki/Trie).

```kotlin
val trie = Trie<Char>()

assertFalse(trie.contains(emptyList()))

trie.add(arrayListOf('a', 'b', 'c'))
assertFalse(trie.contains(arrayListOf('a')))
assertFalse(trie.contains(arrayListOf('a', 'b')))
assertTrue(trie.contains(arrayListOf('a', 'b', 'c')))
```
