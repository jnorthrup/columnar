package cursors.lillypads

import com.sun.net.httpserver.*
import java.io.InputStreamReader
import java.net.InetSocketAddress



// FETCHSIZE=20  BULKSIZE=5 LIMIT=10  TYPES=['"VIEW"'] TERSE=true  ../../jdbc2json/bin/jdbctocouchdbbulk.sh http://127.0.0.1:9989/foo1_ jdbc:sqlite:/tmp/sa.sqlite

fun main(args: Array<String>) {
    val inetSocketAddress = InetSocketAddress(9989)
    val srv = HttpServer.create(inetSocketAddress, 1024)
    val createContext: HttpContext = srv.createContext("/" ) { exchange: HttpExchange ->
        println("bitbanging: " + exchange.requestMethod + " " + exchange.requestURI)

        val requestHeaders: Headers = exchange.requestHeaders

        println(requestHeaders.map { (k, v): Map.Entry<String, MutableList<String>> -> k to v }.toMap())

        InputStreamReader(exchange.requestBody).use { it.readLines().forEach { println(it) } }
        exchange.sendResponseHeaders(403, 0)
        exchange.close()
    }
    srv.start()

    while (true) {
        Thread.sleep(10000)
    }
}