package vec.macros


fun <T : Comparable<T>> Vect0r<T>.binarySearch(key: T) = binarySearch0(0, size, key)

fun <T : Comparable<T>> Vect0r<T>.binarySearch(fromIndex: Int, toIndex: Int, key: T): Int = let {
    require(fromIndex <= toIndex) { "fromIndex($fromIndex) > toIndex($toIndex)" }
    require(fromIndex >= 0){"underflow $fromIndex"}
    require(toIndex <= size){"overflow $toIndex of $size"}
    binarySearch0(fromIndex, toIndex, key)
}

fun <T : Comparable<T>> Vect0r<T>.binarySearch0(fromIndex: Int, toIndex: Int, key: T): Int {
    var low = fromIndex
    var high = toIndex - 1
    while (low <= high) {
        val mid = low + high ushr 1
        val midVal = this[mid]
        val cmp = midVal.compareTo(key)
        if (cmp < 0) {
            low = mid + 1
        } else {
            if (cmp <= 0) {
                return mid
            }
            high = mid - 1
        }
    }
    return -(low + 1)
}

