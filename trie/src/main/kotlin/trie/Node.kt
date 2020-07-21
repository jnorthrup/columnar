package trie

/**
 * Created by kenny on 6/6/16.
 */
class Node(val pathSeg: String, var leaf: Boolean, val payload: Int, val children: MutableMap<String, Node> = linkedMapOf())

