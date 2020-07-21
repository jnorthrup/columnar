package trie;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public class BinarySearchTreeMap<K extends Comparable<K>, V> extends AbstractIterableMap<K, V> {
    @Nullable
    private TreeNode<K, V> overallRoot;
    private int size;

    public BinarySearchTreeMap() {
        overallRoot = null;
        size = 0;
    }

    @Override
    public V put(K key, V value) {
        TreeNode<K, V> oldNode = new TreeNode<>(null, null);
        overallRoot = put(overallRoot, new TreeNode<>(key, value), oldNode);
        return oldNode.value;
    }

    private TreeNode<K, V> put(TreeNode<K, V> current, TreeNode<K, V> newNode, TreeNode<K, ? super V> oldNode) {
        TreeNode<K, V> res;
        if (current == null) {
            size++;
            res = newNode;
        } else {
            int result = newNode.key.compareTo(current.key);
            if (result == 0) {
                oldNode.value = current.value;
                current.value = newNode.value;
            } else if (result < 0) {
                current.left = put(current.left, newNode, oldNode);
            } else {
                current.right = put(current.right, newNode, oldNode);
            }
            res = current;
        }
        return res;
    }

    @Override
    public V get(Object key) {
        V res = null;
        TreeNode<K, V> result = getNode(overallRoot, key);
        if (result != null) {
            res = result.value;
        }
        return res;
    }

    private TreeNode<K, V> getNode(TreeNode<K, V> current, Object key) {
        TreeNode<K, V> res;
        if (current == null) {
            res = null;
        } else {
            int result = compare(key, current.key);
            if (result == 0) {
                res = current;
            } else if (result < 0) {
                res = getNode(current.left, key);
            } else {
                res = getNode(current.right, key);
            }
        }
        return res;
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
        return getNode(overallRoot, key) != null;
    }

    @Override
    public int size() {
        return size;
    }

    @NotNull
    @Override
    public Iterator<Entry<K, V>> iterator() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    int compare(Object k1, Object k2) {
        return ((Comparable<? super K>) k1).compareTo((K) k2);
    }

    static class TreeNode<K, V> {
        final K key;

        V value;
        TreeNode<K, V> left;
        TreeNode<K, V> right;

        TreeNode(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
