package cursors

import cursors.context.Scalar.Companion.Scalar
import cursors.io.IOMemento
import cursors.io.scalars
import org.junit.Test
import vec.macros.Vect0r
import vec.macros.t2
import vec.macros.toList
import vec.macros.toVect0r
import vec.util._v
import vec.util.logDebug

class SimpleCursorTest {
    @Test
    fun testScalarsDispatch() {


        val vscalar = _v[
                Scalar(IOMemento.IoString t2 "a"),
                Scalar(IOMemento.IoInt t2 "b"),
                Scalar(IOMemento.IoDouble t2 "c"),
        ]
        SimpleCursor(vscalar, _v[
                _v["dog", 1, 0.0],
                _v["cat", 11, 0.01],
                _v["act", 111, 0.011],
                _v["lib", 1111, 0.0111],
                _v["nil", 11111, 0.1111],

        ]) .scalars.also { logDebug { it.toList().toString() } }
        val listOf: MutableList<Vect0r<*>> = MutableList(0){ _v[null]}
        listOf.clear()
        val to = listOf.toVect0r()
        val simpleCursor: SimpleCursor = SimpleCursor(vscalar, to as Vect0r<Vect0r<*>>)
        simpleCursor.scalars.also { logDebug { it.toList().toString() } }

    }
}