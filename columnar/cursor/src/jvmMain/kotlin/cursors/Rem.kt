package cursors

import vec.macros.combine

/**  flattens cursor to Vect0r of T? preserving order/position */
inline infix operator fun <reified T> Cursor.rem(t: Class<T>) = combine(this / t)