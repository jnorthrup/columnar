package trie


/**
 * Created by kenny on 6/6/16.
 */
class Trie(val root: MutableMap<String, Node> = mutableMapOf()) {
    fun add(v: Int, vararg values: String) {
        var children = root
        var parent = root
        for ((i, value) in values.withIndex()) {

            val isLeaf = i == values.size - 1
            // add new node
            if (!children.contains(value)) {
                val node = Node(value, isLeaf, v)
                children[value] = node as Node
                children = node.children

            } else {
                // exist, so traverse current path + set isLeaf if needed
                val node = children[value]!!
                if (isLeaf != node.leaf) {
                    node.leaf = isLeaf
                }
                children = node.children
            }
        }
    }

    fun contains(vararg values: String): Boolean {
        return search(*values) != null
    }

    operator fun get(vararg key: String) = search(*key)?.payload

    fun search(vararg segments: String): Node? {
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
                    if (isLeaf) if (node.leaf) {
                        return node
                    } else {
                        return null
                    }
                    // not at end, continue traversing
                    children = node.children
                } else return null
            }
            throw IllegalStateException("Should not get here")
        } else return null
    }
}