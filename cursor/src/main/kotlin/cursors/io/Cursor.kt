package cursors.io

import cursors.Cursor
import cursors.TypeMemento
import cursors.context.*
import cursors.context.Arity.Companion.arityKey
import vec.macros.*

inline val Cursor.scalars: Vect0r<Scalar>
    get() = toSequence().first().right.map {
        it.invoke() `→` {
            it[arityKey] as Scalar
        }
    }
inline val Cursor.width get()=this.scalars.size
inline val Cursor.scala2s   :Vect02<IOMemento,String?> get()=  scalars α {sc:Scalar->sc as Pai2<IOMemento, String?> }

fun networkCoords(
    ioMemos: Vect0r<TypeMemento>,
    defaultVarcharSize: Int,
    varcharSizes: Map<Int, Int>?
): Vect02<Int, Int> = run {
    val sizes = networkSizes(ioMemos, defaultVarcharSize, varcharSizes)
    //todo: make IntArray Tw1nt Matrix
    var wrecordlen = 0
    val wcoords: Array<Tw1nt> = Array(sizes.size) { ix ->
        Tw1n(wrecordlen, (wrecordlen + sizes[ix]).apply { wrecordlen = this })
    }

    wcoords.map { tw1nt: Tw1nt -> tw1nt.ia.toList() }.toList().flatten().toIntArray().let { ia ->
        Vect02(wcoords.size) { ix: Int ->
            val i = ix * 2
            val i1 = i + 1
            Tw1n(ia[i], ia[i1])
        }
    }
}

fun networkSizes(
    ioMemos: Vect0r<TypeMemento>,
    defaultVarcharSize: Int,
    varcharSizes: Map<Int, Int>?
): Vect0r<Int> = ioMemos.mapIndexed { ix, memento: TypeMemento ->
    val sz = varcharSizes?.get(ix)
    memento.networkSize ?: (sz ?: defaultVarcharSize)
}

