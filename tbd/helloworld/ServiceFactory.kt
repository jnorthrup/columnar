package helloworld

import Helloworld.HelloRequest
import io.grpc.Channel
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.AbstractStub
import java.net.Inet4Address
import java.util.function.Function

interface ServiceFactory<T> {
    fun create(ip: Inet4Address, port: Short): T
    fun destroy(service: T)

    companion object {
        fun <T : AbstractStub<T>?> grpcFactory(make: Function<Channel?, T>): ServiceFactory<T>? {
            return object : ServiceFactory<T> {
                override fun create(ip: Inet4Address, port: Short): T {
                    val channel: ManagedChannel = ManagedChannelBuilder
                        .forAddress(ip.hostAddress, port)
                        .usePlaintext()
                        .keepAliveWithoutCalls(true)
                        .build()
                    return make.apply(channel)
                }

                override fun destroy(service: T) {
                    (service.getChannel() as ManagedChannel).shutdown()
                }
            }
        }
    }
}