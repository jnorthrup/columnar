package cursors

import vec.macros.Vect0r
import vec.macros.α

/** positionally preserves the objects of type t as v<v<T?>>
 */
inline infix operator fun <reified T> Cursor.div(t: Class <T>): Vect0r<Vect0r<T?>> =
    -this α { outer -> outer α { inner -> inner as? T } }