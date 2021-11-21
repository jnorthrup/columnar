package cursors.io

import cursors.Cursor
import cursors.TypeMemento
import cursors.at
import cursors.context.Arity.Companion.arityKey
import cursors.context.Scalar
import vec.macros.*
import vec.macros.Vect02_.right

val Cursor.scalars: Vect0r<Scalar>
    get() = (this at 0).right α { it()[arityKey] as Scalar }

val Cursor.width get() = this.scalars.size
val Cursor.colIdx: Vect02<IOMemento, String?> get() = scalars α { sc: Scalar -> sc as Pai2<IOMemento, String?> }

fun networkCoords(
    ioMemos: Array<TypeMemento>,
    defaultVarcharSize: Int,
    varcharSizes: Map<Int, Int>?,
): Vect02<Int, Int> = run {
    val sizes = networkSizes(ioMemos, defaultVarcharSize, varcharSizes)
    //todo: make IntArray Tw1nt Matrix
    var wrecordlen = 0
    val wcoords = Array(sizes.size) { ix ->
        (wrecordlen t2 (wrecordlen + sizes[ix]).also { wrecordlen = it })
    }

    wcoords.map { tw1nt -> (-tw1nt).toList() }.flatten().toIntArray().let { ia ->
        Vect02(wcoords.size) { ix: Int ->
            val i = ix * 2
            val i1 = i + 1
            (ia[i] t2 ia[i1])
        }
    }
}

fun networkSizes(
    ioMemos: Array<TypeMemento>,
    defaultVarcharSize: Int,
    varcharSizes: Map<Int, Int>?,
) = ioMemos.mapIndexed { ix, memento: TypeMemento ->
    val sz = varcharSizes?.get(ix)
    memento.networkSize ?: (sz ?: defaultVarcharSize)
}
