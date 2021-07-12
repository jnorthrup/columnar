package vec.macros

inline class VList<T>(val v: Vect0r<T>) : List<T> {
    override val size: Int
        get() = v.first
    val z get() = `Vect0r➤`<T>(v)

    override fun contains(element: T) = z.contains(element)


    override fun containsAll(elements: Collection<T>): Boolean {
        for (element in elements) {
            if (!z.contains(element)) return false
        }
        return true
    }

    override fun get(index: Int) = v[index]

    override fun indexOf(element: T): Int = z.indexOf(element)

    override fun isEmpty(): Boolean {
        return v.size == 0
    }

    override fun iterator(): Iterator<T> {
        return z.iterator()
    }

    override fun lastIndexOf(element: T): Int {
        return v.size - 1
    }

    override fun listIterator(): ListIterator<T> {
        return listIterator(0)
    }

    override fun listIterator(index: Int): ListIterator<T> {
        return object : ListIterator<T> {
            var x = index
            override fun hasNext(): Boolean {
                return x < size - 1
            }

            override fun hasPrevious(): Boolean {
                return x > 0
            }

            override fun next(): T {
                return v[++x]
            }

            override fun nextIndex(): Int {
                return x.inc()
            }

            override fun previous(): T {
                return v[--x]
            }

            override fun previousIndex(): Int {
                return x.dec()
            }
        }

    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        return List<T>(toIndex - fromIndex) {
            v.get(toIndex + it)
        }
    }

}
