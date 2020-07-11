package trie

import vec.macros.Qu4d
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Created by kenny on 6/6/16.
 */
class Node(val pathSeg: String, var leaf: Boolean, val payload: Int, val children: SortedMap<String, Node> = sortedMapOf())

