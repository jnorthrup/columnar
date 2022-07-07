package cursors.macros

import cursors.Cursor
import cursors.at
import cursors.io.scalars
import vec.macros.*

/**
 * this is a specific "combine" for cursor column-wise which does not seem to work when generalized to Vector<Vect02> due to the scalar selection features of Cursor
 */
fun join(vargs: Vect0r<Cursor>): Cursor = join(*vargs.toArray())


/**
 * this is a specific "combine" for cursor column-wise which does not seem to work when generalized to Vector<Vect02> due to the scalar selection features of Cursor
 */
fun join(vararg vargs: Cursor): Cursor = muxIndexes(vargs.map(Cursor::scalars)).let { (isize, tails) ->
    vargs[0].first t2 { iy: Int ->
        isize t2 { ix: Int ->
            demuxIndex(tails, ix).let { (source, index) ->
                val theCurs = vargs[source]
                val theRow = (theCurs as Cursor at iy )
                theRow[index]
            }
        }
    }
}
