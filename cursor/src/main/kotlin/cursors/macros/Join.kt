package cursors.macros

import cursors.Cursor
import cursors.at
import cursors.io.scalars
import vec.macros.*

// c[0,1,2]
// scalars[0,1,2],[3,4,5],[6,7,8]
//
//c[iy] at (7)

fun join(vargs: Vect0r<Cursor>): Cursor = join(*vargs.toArray())


fun join(vararg vargs: Cursor): Cursor =

muxIndexes(vargs α Cursor::scalars).let { (isize, tails) ->
    vargs[0].size t2 { iy: Int ->
        isize t2 { ix: Int ->
            demuxIndex(tails, ix).let{
                (source, index)->
                (vargs[source] at iy)[index]
            }
        }
    }
}
