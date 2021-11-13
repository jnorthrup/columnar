package vec.util

import java.io.BufferedReader
import java.io.InputStreamReader

fun fileSha256Sum(pathname: String): String {
    val command = ProcessBuilder().command("sha256sum", pathname)
    val process = command.start()
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    val builder = StringBuilder()
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        builder.append(line)
        builder.append("\n")
    }
    val result = builder.toString()
    return result
}