package columnar.lillypads

import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel


val allBroadcastAddresses: List<InetAddress>
    get() = NetworkInterface.getNetworkInterfaces().toList().filterNot { it.isLoopback }.filter { it.isUp }.map {
        it.interfaceAddresses.map { it.broadcast }.filterNotNull()
    }.flatten()
val activeAddresses
    get() = NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp }.map {
        it.interfaceAddresses
    }.flatten()

fun main(args: Array<String>) {

    println(allBroadcastAddresses)
    println("is anylocal   " + activeAddresses.map { it.address to it.address.isAnyLocalAddress })
    println("is sitelocal  " + activeAddresses.map { it.address to it.address.isSiteLocalAddress })
    println("is loop       " + activeAddresses.map { it.address to it.address.isLoopbackAddress })

    val channel = DatagramChannel.open(StandardProtocolFamily.INET)

    channel.setOption(StandardSocketOptions.SO_BROADCAST, true)
    channel.bind(InetSocketAddress(allBroadcastAddresses.last(), 2112))
    println(channel.localAddress)
    channel.configureBlocking(true)
    val allocate = ByteBuffer.allocate(1505)
    while (true) {//nc -b -u 192.168.1.255 2112

        channel.receive(allocate)
        println(String(allocate.array()))
    }
}