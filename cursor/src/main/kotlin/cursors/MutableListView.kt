package cursors

import vec.macros.Vect0r

/**
 * we can use this to break all of the imutable-size contracts of Vect0r and hide bugs for days
 */
class MutableListView<T>(
    val list: List<T>
) : Vect0r<T> {
    override val first by list::size
    override val second: (Int) -> T
        get() = list::get
}