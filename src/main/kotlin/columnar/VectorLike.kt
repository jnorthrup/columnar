package columnar

import kotlinx.coroutines.flow.*


typealias Vect0r<Value> = Pair< /*Size*/ () -> Int, ( /*Key*/ Int) -> Value>

val<T> Vect0r<T>.size: Int get() = first.invoke()

@JvmName("vlike_Sequence_1")
inline operator fun <reified T> Sequence<T>.get(vararg index: Int) = get(index)

@JvmName("vlike_Sequence_Iterable2")
inline operator fun <reified T> Sequence<T>.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_Sequence_IntArray3")
inline operator fun <reified T> Sequence<T>.get(index: IntArray) = this.toList()[index].asSequence()

@JvmName("vlike_Flow_1")
suspend inline fun <reified T> Flow<T>.get(vararg index: Int) = get(index)

@JvmName("vlike_Flow_Iterable2")
suspend inline fun <reified T> Flow<T>.get(indexes: Iterable<Int>) = this.get(indexes.toList().toIntArray())

@JvmName("vlike_Flow_IntArray3")
suspend inline fun <reified T> Flow<T>.get(index: IntArray) = this.toList()[index].asFlow()

@JvmName("vlike_List_1")
inline operator fun <reified T> List<T>.get(vararg index: Int) = get(index)

@JvmName("vlike_List_Iterable2")
inline operator fun <reified T> List<T>.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_List_IntArray3")
inline operator fun <reified T> List<T>.get(index: IntArray) = List(index.size) { i: Int -> this[index[i]] }

@JvmName("vlike_Array_1")
inline operator fun <reified T> Array<T>.get(vararg index: Int) = get(index)

@JvmName("vlike_Array_Iterable2")
inline operator fun <reified T> Array<T>.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_Array_IntArray3")
inline operator fun <reified T> Array<T>.get(index: IntArray) = Array(index.size) { i: Int -> this[index[i]] }

@JvmName("vlike_Vect0r_get")
inline operator fun <reified T> Vect0r<T>.get(index: Int) = second(index)

@JvmName("vlike_Vect0r_1")
inline operator fun <reified T> Vect0r<T>.get(vararg index: Int) = get(index)

@JvmName("vlike_Vect0r_Iterable2")
inline operator fun <reified T> Vect0r<T>.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_Vect0r_IntArray3")
inline operator fun <reified T> Vect0r<T>.get(index: IntArray) = this.let { (a, b) ->
    index::size  to { ix: Int -> b(index[ix]) }
}

inline fun <reified T> Vect0r<T>.toArray() = this.let { (_, vf) -> Array(size) { vf(it) } }
fun <T> Vect0r<T>.toList() = this.let { (_, vf) -> List(size) { vf(it) } }

fun <T> Vect0r<T>.toSequence() = this.let { (_, vf) ->
    sequence {
        for (ix in 0 until size) {
            yield(vf(ix))
        }
    }
}

fun <T> Vect0r<T>.toFlow() = this.let { (_, vf) ->
    flow {
        for (ix in 0 until size) {
            emit(vf(ix))
        }
    }
}

inline fun <reified T, R> Vect0r<T>.map(fn: (T) -> R) = List<R>(size) { ix -> fn(this[ix]) }
inline fun <reified T, R> Vect0r<T>.mapIndexed(fn: (Int, T) -> R) = List<R>(size) { ix -> fn(ix, this[ix]) }
inline fun <reified T, R> Vect0r<T>.forEach(fn: (T) -> Unit) {
    for (ix in (0 until size)) fn(this[ix])
}

inline fun <reified T, R> Vect0r<T>.forEachIndexed(fn: (Int, T) -> Unit) {
    for (ix in (0 until size)) fn(ix, this[ix])
}

fun <T> Array<T>.toVect0r(): Vect0r<T> = { size } to { ix: Int -> this[ix] }
fun <T> List<T>.toVect0r(): Vect0r<T> = { size } to { ix: Int -> this[ix] }
suspend fun <T> Flow<T>.toVect0r(): Vect0r<T> = this.toList().toVect0r()
fun <T> Iterable<T>.toVect0r(): Vect0r<T> = this.toList().toVect0r()
fun <T> Sequence<T>.toVect0r(): Vect0r<T> = this.toList().toVect0r()

@JvmName("combine_Flow")
inline fun <reified T> combine(vararg s: Flow<T>) = flow {
    for (f in s) {
        f.collect {
            emit(it)
        }
    }
}

@JvmName("combine_Sequence")
inline fun <reified T> combine(vararg s: Sequence<T>) = sequence {
    for (sequence in s) {
        for (t in sequence) {
            yield(t)
        }
    }
}

@JvmName("combine_List")
inline fun <reified T> combine(vararg a: List<T>) =
    a.sumBy { it.size }.let { size ->
        var x = 0
        var y = 0
        List(size) { i ->
            if (y >= a[x].size) {
                ++x
                y = 0
            }
            a[x][y++]
        }
    }

@JvmName("combine_Vect0r")
inline fun <reified T> combine(vararg a: Vect0r<T>) = a.let {
    var acc = 0
    val order = IntArray(a.size) {
        val begin = acc
        acc += a[it].size
        begin
    }

    Vect0r({ acc }) { ix ->
        val offset = (order.binarySearch(ix))

        val slot = if (offset < 0) 0 - (offset + 1) else offset
        val begin = order[slot]
        val t = a[slot][ix - begin]
        t
    }
}


@JvmName("combine_Array")
inline fun <reified T> combine(vararg a: Array<T>) =
    a.sumBy { it.size }.let { size ->
        var x = 0
        var y = 0
        Array(size) { i ->
            if (y >= a[x].size) {
                ++x
                y = 0
            }
            a[x][y++]
        }
    }

//array-like mapped map
inline operator fun <reified K, reified V> Map<K, V>.get(ks: Iterable<K>) = this.get(*ks.toList().toTypedArray())
inline operator fun <K, reified V> Map<K, V>.get(vararg ks: K) = Array(ks.size){ ix->ks[ix].let(this::get)!!}
