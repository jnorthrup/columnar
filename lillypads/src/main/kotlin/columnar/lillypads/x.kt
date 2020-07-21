package  columnar.lillypads

import java.security.MessageDigest

fun main() {
    val x = "x"
    listOf(
            HashUtils.md5sum(x),
            HashUtils.sha1(x),
//        HashUtils.sha3(x),
            HashUtils.sha256(x),
            HashUtils.sha512(x)
    ).forEach { println(it) }
}

/**
 * Hashing Utils
 * @author Sam Clarke <www.samclarke.com>
 * @license MIT
 */
object HashUtils {
    fun sha1(input: String) = hashString("SHA-1", input)
    fun sha256(input: String) = hashString("SHA-256", input)

    //    fun sha3(input: String) = hashString("SHA-3", input)
    fun sha512(input: String) = hashString("SHA-512", input)
    fun md5sum(input: String) = hashString("MD5", input)

    /**
     * Supported algorithms on Android:
     *
     * Algorithm	Supported API Levels
     * MD5          1+
     * SHA-1	    1+
     * SHA-224	    1-8,22+
     * SHA-256	    1+
     * SHA-384	    1+
     * SHA-512	    1+
     */
    private fun hashString(type: String, input: String): String {
        val HEX_CHARS = "0123456789ABCDEF"
        val bytes = MessageDigest
                .getInstance(type)
                .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(HEX_CHARS[i shr 4 and 0x0f])
            result.append(HEX_CHARS[i and 0x0f])
        }

        return result.toString()
    }
}

