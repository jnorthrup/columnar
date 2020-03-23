package columnar.lillypads

import java.net.InetAddress
import java.net.NetworkInterface


val allBroadcastAddresses: List<InetAddress>
    get() = NetworkInterface.getNetworkInterfaces().toList().filterNot { it.isLoopback }.filter { it.isUp }.map {
        it.interfaceAddresses.map { it.broadcast }.filterNotNull()
    }.flatten()

fun main(args: Array<String>) {
    println(allBroadcastAddresses)
}