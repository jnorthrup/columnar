package trie;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @see AbstractIterableMap
 * @see Map
 */
public class ArrayMap<K, V> extends AbstractIterableMap<K, V> {
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    /*
    Warning:
    DO NOT rename this `entries` field or change its type.
    We will be inspecting it in our Gradescope-only tests.

    An explanation of this field:
    - `entries` is the array where you're going to store all of your data (see the [] square bracket notation)
    - The other part of the type is the SimpleEntry<K, V> -- this is saying that `entries` will be an
    array that can store a SimpleEntry<K, V> object at each index.
       - SimpleEntry represents an object containing a key and a value.
        (To jump to its details, middle-click or control/command-click on SimpleEntry below)

    */
    Map.Entry<K, V>[] entries;
    private int size;
    private int resize;

    // You may add extra fields or helper methods though!

    public ArrayMap() {
        this(DEFAULT_INITIAL_CAPACITY);
        this.entries = createArrayOfEntries(DEFAULT_INITIAL_CAPACITY);
        this.size = 0;
        this.resize = 1;
    }

    public ArrayMap(int initialCapacity) {
        this.entries = this.createArrayOfEntries(initialCapacity);
    }

    /**
     * This method will return a new, empty array of the given size that can contain
     * {@code Entry<K, V>} objects.
     * <p>
     * Note that each element in the array will initially be null.
     * <p>
     * Note: You do not need to modify this method.
     */
    @SuppressWarnings("unchecked")
    private Map.Entry<K, V>[] createArrayOfEntries(int arraySize) {
        /*
        It turns out that creating arrays of generic objects in Java is complicated due to something
        known as "type erasure."

        We've given you this helper method to help simplify this part of your assignment. Use this
        helper method as appropriate when implementing the rest of this class.

        You are not required to understand how this method works, what type erasure is, or how
        arrays and generics interact.
        */
        return new AbstractMap.SimpleEntry[arraySize];
    }

    @Override
    public V get(Object key) {
        V result = null;
        boolean finished = false;
        V getKey = (V) key;
        V gotKey = null;
        for (int i = 0; i < size; i++) {
            if (Objects.equals(entries[i].getKey(), key)) {
                gotKey = entries[i].getValue();
            }
            if (i == size - 1 && gotKey != getKey) {
                result = gotKey;
                finished = true;
                break;
            }
        }
        if (!finished) {
            if (size == 0) {
                result = gotKey;
            } else {
                result = getKey;
            }
        }
        return result;
    }

    @Override
    public V put(K key, V value) {
        V result = null;
        boolean finished = false;
        V holder = null;
        AbstractMap.SimpleEntry<K, V> newEntry = new AbstractMap.SimpleEntry<>(key, value);
        if (size == 0) {
            entries[0] = newEntry;
            size++;
            result = holder;
        } else {
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    if (Objects.equals(entries[i].getKey(), key)) {
                        holder = entries[i].getValue();
                        entries[i].setValue(value);
                        result = holder;
                        finished = true;
                        break;
                    }
                }
                if (!finished) {
                    if (entries.length == size) {

                        Map.Entry<K, V>[] storePrevious = createArrayOfEntries(2 * entries.length);
                        System.arraycopy(this.entries, 0, storePrevious, 0, this.entries.length);
                        this.entries = storePrevious;
                    }
                    size++;
                    entries[size - 1] = newEntry;
                }
            }
            if (!finished) {
                result = holder;
            }
        }
        return result;
    }

    @Override
    public V remove(Object key) {
        V holder = null;
        if (size != 0) if (entries[size - 1].getKey().equals(key)) {
            holder = entries[size - 1].getValue();
            entries[size - 1] = null;
            size--;
        } else for (int i = 0; i < size; i++) {
            if (Objects.equals(entries[i].getKey(), key)) {
                holder = entries[i].getValue();
                entries[i] = entries[size - 1];
                entries[size - 1] = null;
                i = size;
                size--;
                //entries[size - 1] = null;
            }
        }
        return holder;
    }

    @Override
    public void clear() {
        this.entries = createArrayOfEntries(DEFAULT_INITIAL_CAPACITY * resize);
        size = 0;
    }

    @Override
    public boolean containsKey(Object key) {
        boolean result = false;
        final boolean hello = false;
        for (int i = 0; i < size; i++) {
            if (Objects.equals(entries[i].getKey(), key)) {
                result = true;
                break;
            }
        }
        if (!result) {
            result = hello;
        }
        return result;
    }

    @Override
    public int size() {
        return this.size;
    }

    @NotNull
    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        // Note: you won't need to change this method (unless you add more constructor parameters)
        return new ArrayMapIterator<K, V>(this.entries, this.size);
    }

    private static class ArrayMapIterator<K, V> implements Iterator<Map.Entry<K, V>> {
        private final Map.Entry<K, V>[] entries;
        private final int size;
        private int tracker;

        // You may add more fields and constructor parameters
        private ArrayMapIterator(Map.Entry<K, V>[] entries, int size) {
            this.entries = entries;
            this.tracker = 0;
            this.size = size;
        }

        @Override
        public boolean hasNext() {
            boolean hasNext = true;
            if (size == 0 || tracker > size - 1) {
                hasNext = false;
            }
            return hasNext;
        }

        @Override
        public Map.Entry<K, V> next() {
            Map.Entry<K, V> what = null;
            if (this.hasNext()) {
                what = new AbstractMap.SimpleEntry<>(this.entries[tracker].getKey(), this.entries[tracker].getValue());
                tracker++;
            } else {
                throw new NoSuchElementException();
            }
            return what;
        }
    }
}
