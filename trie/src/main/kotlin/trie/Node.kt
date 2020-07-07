package trie

/**
 * Created by kenny on 6/6/16.
 */
data class Node<T, V>(val value: T?,
                      var isLeaf: Boolean,
                      val payload: V,
                      val children: MutableMap<T,Node<T,V>> = mutableMapOf()
)