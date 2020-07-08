package trie

import vec.macros.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by kenny on 6/6/16.
 */
class Trie<T>() {

    val root: MutableMap<T, Node<T>> = mutableMapOf()
    fun add(v: Int, vararg values: T) {
        var children: MutableMap<T, Node<T>> = root
        var parent = root
        for ((i, value) in values.withIndex()) {

            val isLeaf = i == values.size - 1
            // add new node
            if (!children.contains(value)) {
                val node = Node(value, AtomicBoolean(isLeaf), v, mutableMapOf<T, Node<T>>())
                children[value] = node as Node<T>
                children = node.fourth

            } else {
                // exist, so traverse current path + set isLeaf if needed
                val node = children[value]!!
                if (isLeaf != node.second.get()) {
                    node.second.set(isLeaf)
                }
                children = node.fourth as MutableMap<T, Node<T>>
            }
        }
    }

    fun contains(vararg values: T): Boolean {
        return search(*values) != null
    }

    operator fun get(vararg key: T) = search(*key)?.third

    fun search(vararg segments: T): Node<T>? {
        var children = root// exist, so traverse current path, ending if is last value, and is leaf node
        // not at end, continue traversing
        // add new node
        if (children.isNotEmpty()) {
            for ((i, value) in segments.withIndex()) {
                val isLeaf = i == segments.lastIndex
                // add new node
                if (children.contains(value)) {
                    // exist, so traverse current path, ending if is last value, and is leaf node
                    val node = children[value]!!
                    if (isLeaf) if (node.second.get()) {
                        return node
                    } else {
                        return null
                    }
                    // not at end, continue traversing
                    children = node.fourth as MutableMap<T, Node<T>>
                } else return null
            }
            throw IllegalStateException("Should not get here")
        } else return null
    }
}