package trie;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
 
import java.util.Iterator;
import java.util.Map;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A map that additionally implements the Iterable interface.
 *
 * Iterating over the map is equivalent to iterating over {@code map.entrySet()}, so having the map
 * be Iterable is technically redundant, but we've chosen to specify the class this way because
 * the iterator is simpler to implement than the entrySet method.
 *
 * This class inherits default method implementations for almost all Map methods, most of which
 * delegate all work to the iterator. For better efficiency, implementers of this class should
 * provide their own implementations of the basic Map methods included in this file.
 *
 * For the most part, the documentation in this class is copied from {@link Map}, but without a few
 * extra exceptions and edge cases that you should not need to worry about in your implementations.
 */
public abstract class AbstractIterableMap<K, V> extends AbstractMap<K, V> implements Iterable<Map.Entry<K, V>> {

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that
     * {@code Objects.equals(key, k)},
     * then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <i>necessarily</i> indicate that the map
     * contains no mapping for the key; it's also possible that the map
     * explicitly maps the key to {@code null}.  The {@link #containsKey
     * containsKey} operation may be used to distinguish these two cases.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     */
    @Override
    public V get(Object key) {
        return super.get(key);
    }

    /**
     * Associates the specified value with the specified key in this map
     * (optional operation).  If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.  (A map
     * {@code m} is said to contain a mapping for a key {@code k} if and only
     * if {@link #containsKey(Object) m.containsKey(k)} would return
     * {@code true}.)
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key},
     *         if the implementation supports {@code null} values.)
     */
    @Override
    public V put(K key, V value) {
        return super.put(key, value);
    }

    /**
     * Removes the mapping for a key from this map if it is present
     * (optional operation).   More formally, if this map contains a mapping
     * from key {@code k} to value {@code v} such that
     * {@code Objects.equals(key, k)}, that mapping
     * is removed.  (The map can contain at most one such mapping.)
     *
     * <p>Returns the value to which this map previously associated the key,
     * or {@code null} if the map contained no mapping for the key.
     *
     * <p>A return value of {@code null} does not <i>necessarily</i> indicate that the map
     * contained no mapping for the key; it's also possible that the map
     * explicitly mapped the key to {@code null}.
     *
     * <p>The map will not contain a mapping for the specified key once the
     * call returns.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     */
    @Override
    public V remove(Object key) {
        return super.remove(key);
    }

    /**
     * Removes all of the mappings from this map (optional operation).
     * The map will be empty after this call returns.
     */
    @Override
    public void clear() {
        super.clear();
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified
     * key.  More formally, returns {@code true} if and only if
     * this map contains a mapping for a key {@code k} such that
     * {@code Objects.equals(key, k)}.  (There can be
     * at most one such mapping.)
     *
     * @param key key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified
     *         key
     */
    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(key);
    }


    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        return super.size();
    }

    /**
     * Returns an iterator that, when used, will yield all key-value mappings contained within this
     * map.
     */
    @Override
    public abstract Iterator<Map.Entry<K, V>> iterator();

    /**
     * An entrySet implementation that delegates all work to the map's iterator.
     *
     * Contrary to the base Map interface, the set returned is not mutable, so it is not possible
     * to remove items from the map through the entrySet.
     *
     * @return a set view of the mappings contained in this map
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return AbstractIterableMap.this.iterator();
            }

            @Override
            public int size() {
                return AbstractIterableMap.this.size();
            }
        };
    }

}