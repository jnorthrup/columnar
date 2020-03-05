package helloworld

import Helloworld.HelloRequest
import com.gossipmesh.core.Gossiper
import com.gossipmesh.core.GossiperOptions
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import java.net.Inet4Address
import java.util.concurrent.TimeUnit

class GreeterServer : GreeterGrpc.GreeterImplBase() {
    fun sayHello(request: HelloRequest, responseObserver: StreamObserver<Helloworld.HelloReply?>) {
        System.out.println("Request: " + request.getName())
        responseObserver.onNext(
            Helloworld.HelloReply.newBuilder()
                .setMessage("Hello there, " + request.getName())
                .build()
        )
        responseObserver.onCompleted()
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val serverPort = args[0].toInt()
            val server: Server = ServerBuilder.forPort(serverPort)
                .addService(GreeterServer())
                .build()
            server.start()
            val options = GossiperOptions()
            val gossiper = Gossiper(0x02, serverPort, options)
            val gossipPort = gossiper.start()
            println(gossipPort)
            for (i in 1 until args.size) {
                var details = args[i].split(":").toTypedArray()
                if (details.size == 1) {
                    details = arrayOf("127.0.0.1", details[0])
                }
                gossiper.connectTo(
                    (Inet4Address.getByName(details[0]) as Inet4Address), details[1].toInt()
                )
            }
            server.awaitTermination()
            gossiper.stop(1, TimeUnit.SECONDS)
        }
    }
}