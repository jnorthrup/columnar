package vec.macros

import java.util.RandomAccess

inline class `Vect0r➤`<S>(val p: Vect0r<S>) : Iterable<S>, RandomAccess {
    override fun iterator(): Iterator<S> {
        val size = p.size
        return object : Iterator<S> /*,Enumeration<S>*/ {
            var x = 0
            override fun hasNext(): Boolean {
                return x < size
            }

            override fun next(): S = p.get(x)
            //        override fun hasMoreElements() = hasNext()
            //        override fun nextElement(): S =next()

        }
    }
}
