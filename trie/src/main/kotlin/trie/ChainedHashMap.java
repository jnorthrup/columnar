package trie;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @see AbstractIterableMap
 * @see Map
 */
public class ChainedHashMap<K, V> extends AbstractIterableMap<K, V> {
    private static final double DEFAULT_RESIZING_LOAD_FACTOR_THRESHOLD = 5.0;
    private static final int DEFAULT_INITIAL_CHAIN_COUNT = 5;
    private static final int DEFAULT_INITIAL_CHAIN_CAPACITY = 5;

    /*
    Warning:
    DO NOT rename this `chains` field or change its type.
    We will be inspecting it in our Gradescope-only tests.

    An explanation of this field:
    - `chains` is the main array where you're going to store all of your data (see the [] square bracket notation)
    - The other part of the type is the AbstractIterableMap<K, V> -- this is saying that `chains` will be an
    array that can store an AbstractIterableMap<K, V> object at each index.
       - AbstractIterableMap represents an abstract/generalized Map. The ArrayMap you wrote in the earlier part
       of this project qualifies as one, as it extends the AbstractIterableMap class.  This means you can
       and should be creating ArrayMap objects to go inside your `chains` array as necessary. See the instructions on
       the website for diagrams and more details.
        (To jump to its details, middle-click or control/command-click on AbstractIterableMap below)
     */
    AbstractIterableMap<K, V>[] chains;
    private int size;
    private double resizeFactor;
    private int initialChains;
    private int chainCapacity;
    // You're encouraged to add extra fields (and helper methods) though!

    public ChainedHashMap() {
        this(DEFAULT_RESIZING_LOAD_FACTOR_THRESHOLD, DEFAULT_INITIAL_CHAIN_COUNT, DEFAULT_INITIAL_CHAIN_CAPACITY);
    }

    public ChainedHashMap(double resizingLoadFactorThreshold, int initialChainCount, int chainInitialCapacity) {
        this.initialChains = initialChainCount; //num of chains
        this.resizeFactor = resizingLoadFactorThreshold;
        this.chainCapacity = chainInitialCapacity;
        this.size = 0; //size
        this.chains = createArrayOfChains(initialChains); //hold chains in overall map
    }

    /**
     * This method will return a new, empty array of the given size that can contain
     * {@code AbstractIterableMap<K, V>} objects.
     * <p>
     * Note that each element in the array will initially be null.
     * <p>
     * Note: You do not need to modify this method.
     *
     * @see ArrayMap createArrayOfEntries method for more background on why we need this method
     */
    @SuppressWarnings("unchecked")
    private AbstractIterableMap<K, V>[] createArrayOfChains(int arraySize) {
        return (AbstractIterableMap<K, V>[]) new AbstractIterableMap[arraySize];
    }

    /**
     * Returns a new chain.
     * <p>
     * This method will be overridden by the grader so that your ChainedHashMap implementation
     * is graded using our solution ArrayMaps.
     * <p>
     * Note: You do not need to modify this method.
     */
    protected AbstractIterableMap<K, V> createChain(int initialSize) {
        return new ArrayMap<>(initialSize);
    }


    private static int hashCode(Object key, int length) {
        int hash = 0;
        if (key != null) {
            hash = Math.abs(key.hashCode() % length);
        }
        return hash;
    }

    @Override
    public V get(Object key) {
        V result = null;
        int hash = hashCode(key, chains.length);
        if (chains[hash] != null) {
            result = chains[hash].get(key);
        }
        return result;
    }

    @Override
    public V put(K key, V value) {
        int hash = hashCode(key, chains.length);
        V returnVal = null;
        if (chains[hash] == null) {
            chains[hash] = createChain(chainCapacity);
            returnVal = chains[hash].put(key, value);
            this.size++;
        } else if (chains[hash] != null) {
            int oldSize = chains[hash].size();
            returnVal = chains[hash].put(key, value);
            if (oldSize != chains[hash].size()) {
                size++;
            }
        }
        //load factor == number of key val / capacity of array
        if (((double) size / (double) chains.length) > resizeFactor) {
            AbstractIterableMap<K, V>[] doubleChains = createArrayOfChains(chains.length * 2);
            for (Entry<K, V> entry : this) { //from piazza
                int newHash = hashCode(entry.getKey(), doubleChains.length);
                if (doubleChains[newHash] == null) {
                    doubleChains[newHash] = createChain(chainCapacity);
                }
                doubleChains[newHash].put(entry.getKey(), entry.getValue());
            }
            this.chains = doubleChains;
        }
        return returnVal;
    }

    @Override
    public V remove(Object key) {
        V result = null;
        V removeKey = null;
        int hash = hashCode(key, chains.length);
        if (chains[hash] != null) {
            removeKey = chains[hash].remove(key);
            size--;
            result = removeKey;
        }
        return result;
    }

    @Override
    public void clear() {
        this.chains = createArrayOfChains(initialChains);
        this.size = 0;
    }

    @Override
    public boolean containsKey(Object key) {
        int hash = hashCode(key, chains.length);
        boolean hello = true;
        if (chains[hash] == null) {
            hello = false;
        } else {
            hello = chains[hash].containsKey(key);
        }
        return hello;
    }

    @Override
    public int size() {
        return size;
    }

    @NotNull
    @Override
    public Iterator<Entry<K, V>> iterator() {
        // Note: you won't need to change this method (unless you add more constructor parameters)
        return new ChainedHashMapIterator<>(this.chains);
    }

    /*
    See the assignment webpage for tips and restrictions on implementing this iterator.
     */
    private static class ChainedHashMapIterator<K, V> implements Iterator<Entry<K, V>> {
        private AbstractIterableMap<K, V>[] chains; //all chains
        // You may add more fields and constructor parameters
        private Iterator<Entry<K, V>> currentChain; //which chain we are on
        private int tracker; //track which chain

        private ChainedHashMapIterator(AbstractIterableMap<K, V>[] chains) {
            this.chains = chains;
            this.tracker = 0;
            if (this.chains[tracker] != null) {
                this.currentChain = this.chains[tracker].iterator();
                //currentChain keeps track of the chain we want to iterate through
            }
        }
        // Each index in the array of chains is null iff that chain has no entries.
        // The currentChain field of the iterator always references the current chain being iterated through
        // (the chain which contains the next entry that next will return).

        // The currentChain field is null after the iterator has been exhausted of all entries.

        @Override
        // want to get every piece of data. Do this by going through every index of chains,
        // and then iterating through the currentChain ???
        // only add to tracker after we get through every value in each chain
        public boolean hasNext() {
            boolean result = true;
            boolean finished = false;
            boolean hello = false;
            while (tracker <= chains.length - 1) { //loop through chains
                if (currentChain != null && currentChain.hasNext()) {
                    //if the current chain we are on isn't null and it has a .next value return true;
                    finished = true;
                    break;
                } else if (currentChain == null) {
                    while (this.chains[tracker] == null) {
                        tracker++;
                        if (tracker == chains.length || tracker == chains.length - 1) { /////////////
                            result = false;
                            finished = true;
                            break;
                        }
                    }
                    if (finished) break;
                    currentChain = this.chains[tracker].iterator();
                    if (currentChain.hasNext()) {
                        finished = true;
                        break;
                    }
                } else {
                    tracker++;
                    if (tracker <= chains.length - 1) {
                        if (this.chains[tracker] != null) {
                            currentChain = this.chains[tracker].iterator();
                        }
                    }
                }
            }
            if (!finished) {
                result = hello;
            }
            return result;
        }

        @Override
        public Entry<K, V> next() {
            if (hasNext()) {
                return currentChain.next();
            } else {
                throw new NoSuchElementException();
            }


        }
    }
}
