package trie

import vec.macros.Qu4d
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by kenny on 6/6/16.
 */
typealias Node<T > = Qu4d< T  , AtomicBoolean,Int,MutableMap<T,*>>