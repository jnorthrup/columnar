package columnar.lillypads

import vec.util._l
import java.lang.Thread.sleep
import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.Callable
import java.util.concurrent.Executors


val allBroadcastAddresses: List<InetAddress>
    get() = NetworkInterface.getNetworkInterfaces().toList().filterNot { it.isLoopback }.filter { it.isUp }.map {
        it.interfaceAddresses.map { it.broadcast }.filterNotNull()
    }.flatten()
val activeAddresses
    get() = NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp }.map {
        it.interfaceAddresses
    }.flatten()

@OptIn(ExperimentalStdlibApi::class)
fun main(args: Array<String>) {
    val receiver = InetSocketAddress(allBroadcastAddresses.last(), 2112)
    Executors.newCachedThreadPool().invokeAll(
        _l[
                Callable {
                    println(allBroadcastAddresses)
                    println("is anylocal   ${activeAddresses.map { it.address to it.address.isAnyLocalAddress }}")
                    println("is sitelocal  ${activeAddresses.map { it.address to it.address.isSiteLocalAddress }}")
                    println("is loop       ${activeAddresses.map { it.address to it.address.isLoopbackAddress }}")

                    val channel = DatagramChannel.open(StandardProtocolFamily.INET)

                    channel.setOption(StandardSocketOptions.SO_BROADCAST, true)
                    channel.setOption(StandardSocketOptions.SO_REUSEADDR, true)
                    channel.setOption(StandardSocketOptions.SO_REUSEPORT, true)
                    channel.bind(receiver)
                    println(channel.localAddress)
                    channel.configureBlocking(true)
                    val allocate = ByteBuffer.allocate(1505)

                    while (true) {//nc -b -u 192.168.1.255 2112

                        val receive = channel.receive(allocate)

                        val sz = (allocate as ByteBuffer).position()
                        val byteArray = ByteArray(sz)
                        allocate.flip().get(byteArray).clear()
                        println(String(byteArray))
                    }
                },
                Callable {

                    val channel = DatagramChannel.open(StandardProtocolFamily.INET)
                    channel.setOption(StandardSocketOptions.SO_BROADCAST, true)
                    channel.bind(InetSocketAddress(allBroadcastAddresses.last(), 0))
                    println(channel.localAddress)
                    channel.configureBlocking(true)
                    val allocate = ByteBuffer.allocate(1505)
                    var c = 0
                    while (true) {//nc -b -u 192.168.1.255 2112
                        sleep(1000)
                        channel.send(ByteBuffer.wrap("item ${c++}".encodeToByteArray()), receiver)
                    }
                }]
    )
}




