package trie

import vec.macros.Qu4d
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by kenny on 6/6/16.
 */
//data class Node<T, V>(val value: T?,
//                      var isLeaf: Boolean,
//                      val payload: V,
//                      val children: MutableMap<T,Node<T,V>> = mutableMapOf()
//)
typealias Node<T > = Qu4d< T  , AtomicBoolean,Int,MutableMap<T,*>>