package helloworld

import Helloworld.HelloRequest
import com.gossipmesh.core.Listener
import com.gossipmesh.core.Member
import com.gossipmesh.core.MemberAddress
import com.gossipmesh.core.MemberState
import java.util.*
import java.util.function.Function

internal class LoadBalancer : Listener {
    private val serviceFactories: MutableMap<Byte, Any>
    private val services: MutableMap<Byte, MutableMap<MemberAddress?, Any?>?>
    private val random: Random
    fun <T> registerService(serviceByte: Int, factory: ServiceFactory<T>): Service<T> {
        assert(!serviceFactories.containsKey(serviceByte.toByte()))
        serviceFactories[serviceByte.toByte()] = factory
        return Service(serviceByte.toByte())
    }

    inner class Service<T>(private val serviceByte: Byte) {
        val endpoint: T
            get() {
                val nodes: Map<MemberAddress?, *>? = services[serviceByte]
                return if (nodes == null || nodes.isEmpty()) {
                    throw RuntimeException("No services available to handle request")
                } else {
                    val arr: Array<Any> = nodes.values.toTypedArray<Any>()
                    val index = random.nextInt(arr.size)
                    arr[index] as T
                }
            }

    }

    override fun accept(
        from: MemberAddress?,
        address: MemberAddress?,
        member: Member?,
        oldMember: Member?
    ) {
        val serviceUpdated = (member != null && oldMember != null
                && member.serviceByte != oldMember.serviceByte
                && member.servicePort != oldMember.servicePort)
        if (isAlive(oldMember) && (isDead(member) || serviceUpdated)) {
            services.computeIfPresent(
                oldMember!!.serviceByte
            ) { b: Byte?, nodes: MutableMap<MemberAddress?, Any?>? ->
                val service = nodes!!.remove(address)
                if (service != null) {
                    val factory =
                        serviceFactories[oldMember.serviceByte] as ServiceFactory<Any>?
                    factory!!.destroy(service)
                }
                if (nodes.isEmpty()) null else nodes
            }
        }
        if (isDead(oldMember) && isAlive(member) || serviceUpdated) {
            val factory = serviceFactories[member!!.serviceByte] as ServiceFactory<Any>?
            if (factory != null) { // we can't build a client if we don't have the service byte registered!
                services.computeIfAbsent(
                    member.serviceByte,
                    Function<Byte, MutableMap<MemberAddress?, Any?>?> { initialCapacity: Byte ->
                        ConcurrentHashMap(initialCapacity.toInt())
                    }
                )[address] = factory.create(address!!.address, member.servicePort)
            }
        }
    }

    companion object {
        private fun isAlive(member: Member?): Boolean {
            return member != null && (member.state === MemberState.ALIVE || member.state === MemberState.SUSPICIOUS)
        }

        private fun isDead(member: Member?): Boolean {
            return member == null || member.state === MemberState.DEAD || member.state === MemberState.LEFT
        }
    }

    init {
        serviceFactories = HashMap()
        services = HashMap()
        random = Random()
    }
}