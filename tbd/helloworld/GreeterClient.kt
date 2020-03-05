package helloworld

import Helloworld.HelloRequest
import com.gossipmesh.core.Gossiper
import com.gossipmesh.core.GossiperOptions
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address

class GreeterClient(channel: ManagedChannel) {
    private val channel: ManagedChannel
    private val stub: GreeterGrpc.GreeterBlockingStub
    fun sayHello(name: String?): String {
        val response: Helloworld.HelloReply = stub.sayHello(
            Helloworld.HelloRequest.newBuilder()
                .setName(name)
                .build()
        )
        return response.getMessage()
    }

    fun authority(): String {
        return channel.authority()
    }

    companion object {
        var factory: ServiceFactory<GreeterClient> = object : ServiceFactory<GreeterClient> {
            override fun create(ip: Inet4Address, port: Short): GreeterClient {
                val channel: ManagedChannel = ManagedChannelBuilder
                    .forAddress(ip.hostAddress, port)
                    .usePlaintext()
                    .keepAliveWithoutCalls(true)
                    .build()
                return GreeterClient(channel)
            }

            override fun destroy(service: GreeterClient) {
                service.channel.shutdown()
            }
        }

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val loadBalancer = LoadBalancer()
            val greeterService: LoadBalancer.Service<GreeterClient?> =
                loadBalancer.registerService(0x02, factory)!!
            val options = GossiperOptions()
            val gossiper = Gossiper(0, 0, options)
            gossiper.addListener("load-balancer", loadBalancer)
            val gossipPort = gossiper.start()
            println(gossipPort)
            for (i in args.indices) {
                var details = args[i].split(":").toTypedArray()
                if (details.size == 1) {
                    details = arrayOf("127.0.0.1", details[0])
                }
                gossiper.connectTo(
                    (Inet4Address.getByName(details[0]) as Inet4Address), details[1].toInt()
                )
            }
            InputStreamReader(System.`in`).use { reader ->
                BufferedReader(reader).use { bufferedReader ->
                    while (true) {
                        print("Enter your name: ")
                        System.out.flush()
                        val name = bufferedReader.readLine()
                        val greeter = greeterService.endpoint
                        val start = System.nanoTime()
                        val response = greeter!!.sayHello(name)
                        val end = System.nanoTime()
                        val time = (end - start).toDouble() / 1000000
                        println(greeter.authority() + " " + response + " took " + time + "ms")
                    }
                }
            }
        }
    }

    init {
        this.channel = channel
        stub = GreeterGrpc.newBlockingStub(channel)
    }
}