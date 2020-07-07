package trie

/**
 * Created by kenny on 6/6/16.
 */
class Trie<T, V>() {

    val root = mutableMapOf<T, Node<T, V>>()
    fun add(v: V, vararg values: T) {
        var children = root

        for ((i, value) in values.withIndex()) {

            val isLeaf = i == values.size - 1
            // add new node
            if (!children.contains(value)) {
                val node = Node<T, V>(value, isLeaf, v)
                children[value] = node
                children = node.children

            } else {
                // exist, so traverse current path + set isLeaf if needed
                val node = children[value]!!
                if (isLeaf) {
                    node.isLeaf = isLeaf
                }
                children = node.children
            }
        }
    }

    fun contains(vararg values: T): Boolean {
        return search(*values) != null
    }

     operator fun get(vararg key: T) = search(*key)?.payload

    fun search(vararg segments: T): Node<T, V>? {
        var children = root
        if (children.isEmpty()) {
            return null
        }
        for ((i, value) in segments.withIndex()) {
            val isLeaf = i == segments.lastIndex
            // add new node
            if (children.contains(value)) {
                // exist, so traverse current path, ending if is last value, and is leaf node
                val node = children[value]!!
                if (isLeaf) {
                    if (node.isLeaf) {
                        return node
                    } else {
                        return null
                    }
                }
                // not at end, continue traversing
                children = node.children
            } else return null
        }
        throw IllegalStateException("Should not get here")
    }
}