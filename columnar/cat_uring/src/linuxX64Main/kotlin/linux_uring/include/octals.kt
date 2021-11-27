package linux_uring.include

import kotlin.math.pow


/**
>>> 64.toOctal()
res87: kotlin.Int = 100

 */
fun Int.toOctal(): Int {
    var decimal = this
    var octalNumber = 0
    var i = 1
    while (decimal != 0) {
        octalNumber += decimal % 8 * i
        decimal /= 8
        i *= 10
    }
    return octalNumber
}

/**>>> 10.fromOctal()
res85: kotlin.Int = 8

 */
fun Int.fromOctal(): Int {
    var octal = this
    var decimal = 0
    var i = 0
    while (octal != 0) {
        decimal += (octal % 10 * 8.0.pow(i.toDouble())).toInt()
        octal /= 10
        ++i
    }
    return decimal
}
