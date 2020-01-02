package columnar

import columnar.context.Arity
import columnar.context.Columnar

class DataFrame1(val tableroot: TableRoot) {
    operator fun <T> get(vararg order: Int) = values[order]
    val values get() = vframe
    val nioCursor: NioCursor = tableroot.first
    val shape = nioCursor.first
    val vframe: Vect0r<Vect0r<Any?>> =
        Vect0r({ shape[1] }) { iy -> Vect0r({ shape[0] }) { ix -> tableroot.first[ix, iy].first() } }
    val columns
        get() =
            tableroot.let { (a, b) ->
                b.let { coroutineContext ->
                    //                            runBlocking(coroutineContext) {
                    val c = coroutineContext[Arity.arityKey] as Columnar
                    c.second!!.toList().zip(c.first)
//                            }
                }
            }
}