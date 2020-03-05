package com.gossipmesh.core

import com.gossipmesh.core.Member.Companion.isLaterGeneration
import java.io.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.experimental.and

class Gossiper(socket: DatagramSocket, serviceByte: Int, servicePort: Int, options: GossiperOptions) {
    private val members: MutableMap<MemberAddress, Member>
    private val waiting: MutableMap<MemberAddress, ScheduledFuture<*>>
    private val serviceByte: Byte
    private val servicePort: Short
    private val options: GossiperOptions
    private val executor: ScheduledExecutorService
    private val socket: DatagramSocket
    private val listeners: HashMap<Any, Listener>
    private var listener: Thread? = null
    private var generation: Byte = 0

    constructor(serviceByte: Int, servicePort: Int, options: GossiperOptions) : this(DatagramSocket(), serviceByte, servicePort, options)
    constructor(port: Int, serviceByte: Int, servicePort: Int, options: GossiperOptions) : this(DatagramSocket(port), serviceByte, servicePort, options)

    private fun loggingExceptions(f: Runnable): Runnable {
        return Runnable {
            try {
                f.run()
            } catch (ex: Exception) {
                LOGGER.log(Level.SEVERE, "Exception thrown in task", ex)
            }
        }
    }

    @Throws(IOException::class)
    fun start(): Int {
        executor.scheduleAtFixedRate(loggingExceptions(Runnable {
            try {
                probe()
            } catch (ex: IOException) {
                LOGGER.log(Level.SEVERE, "Exception thrown when trying to probe", ex)
            }
        }), 0, options.protocolPeriodMs.toLong(), TimeUnit.MILLISECONDS)
        socket.soTimeout = 500
        listener = Thread(Runnable {
            val buffer = ByteArray(508)
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    assert(packet.offset == 0)
                    val address = MemberAddress(
                            (packet.address as Inet4Address),
                            packet.port.toShort())
                    val recvBuffer = Arrays.copyOf(packet.data, packet.length)
                    executor.execute(loggingExceptions(Runnable {
                        try {
                            ByteArrayInputStream(recvBuffer).use { `is` -> DataInputStream(`is`).use { dis -> handleMessage(address, dis) } }
                        } catch (ex: IOException) {
                            LOGGER.log(Level.SEVERE, "IO Exception while handling a message from $address", ex)
                        }
                    }))
                } catch (ex: SocketTimeoutException) {
                    // do nothing
                } catch (ex: IOException) {
                    if (ex.message != "Socket closed") {
                        LOGGER.log(Level.SEVERE, "IO Exception reading from datagram socket", ex)
                    }
                }
            }
        })
        listener!!.isDaemon = true
        listener!!.start()
        return socket.localPort
    }

    private fun randomNodes(matching: (MemberAddress?, Member?) -> Boolean = { _, _ -> true }): Iterable<MemberAddress> {
        return Iterable {
            members.entries
                    .filter { x: Map.Entry<MemberAddress, Member> -> matching(x.key, x.value) }
                    .map { it.key }
                    .sortedWith(RandomComparator())
                    .iterator()
        }
    }

    private inner class RandomComparator<T> @JvmOverloads internal constructor(private val random: Random = Random()) : Comparator<T> {
        private val map: MutableMap<T, Int> = IdentityHashMap()
        override fun compare(left: T, right: T): Int {
            return Integer.compare(valueOf(left), valueOf(right))
        }

        fun valueOf(obj: T): Int {
            synchronized(map) { return map.computeIfAbsent(obj, { random.nextInt() }) }
        }

    }

    @Throws(InterruptedException::class)
    fun stop(timeunit: Long, unit: TimeUnit?) {
        listener!!.interrupt() // this thread should shut itself down within half a second at worst
        socket.close()
        executor.shutdownNow()
        executor.awaitTermination(timeunit, unit)
    }

    @Throws(IOException::class)
    private fun probe() {
        var i = options.fanoutFactor
        for (address in randomNodes()) {
            if (--i < 0) {
                break
            }
            LOGGER.log(Level.FINEST, "Performing direct ping on: $address")
            ping(address)
        }
    }

    private fun scheduleTask(address: MemberAddress, command: Runnable, delay: Int, units: TimeUnit = TimeUnit.MILLISECONDS) {
        waiting.computeIfAbsent(address) {
            lateinit var future: ScheduledFuture<*>  //= arrayOfNulls(1)
            executor.schedule(loggingExceptions(Runnable {
                waiting.remove(address, future)
                command.run()
            }), delay.toLong(), units).also { future = it }
        }
    }

    @Throws(IOException::class)
    private fun sendMessage(address: MemberAddress, header: MessageWriter<DataOutput>) {
        val outBuffer = ByteArray(508)
        var limit = 0
        val sending: MutableSet<MemberAddress> = HashSet()
        try {
            FiniteByteArrayOutputStream(outBuffer).use { os ->
                DataOutputStream(os).use { dos ->
                    dos.write(0) // protocol version
                    header.writeTo(dos)
                    dos.write(generation.toInt())
                    dos.write(serviceByte.toInt())
                    dos.writeShort(servicePort.toInt())
                    val receiver = members[address]
                    if (receiver == null) {
                        dos.write(MemberState.DEAD.ordinal)
                        dos.write(0)
                    } else {
                        dos.write(receiver.state.ordinal)
                        dos.write(receiver.generation.toInt())
                    }
                    limit = os.position()
                    val queue = PriorityQueue<Map.Entry<MemberAddress, Member>>(Comparator.comparingLong { a: Map.Entry<MemberAddress?, Member> -> a.value.timesMentioned })
                    queue.addAll(members.entries)
                    for ((key, value) in queue) {
                        if (key != address) {
                            writeEvent(dos, key, value) // this will eventually throw an exception
                            limit = os.position()
                            sending.add(key)
                        }
                    }
                }
            }
        } catch (ex: IOException) {
            // ignore this, we don't actually care
        }
        val packet = DatagramPacket(outBuffer, limit)
        packet.address = address.address
        packet.port = address.port.toInt() and 0xFFFF
        socket.send(packet)
        for (sent in sending) members[sent]!!.timesMentioned++
    }

    fun connectTo(address: Inet4Address, port: Int) {
        executor.execute(loggingExceptions(Runnable {
            try {
                ping(MemberAddress(address, port.toShort()))
            } catch (ex: IOException) {
                LOGGER.log(Level.SEVERE, "Exception thrown while connecting to $address", ex)
            }
        }))
    }

    @Throws(IOException::class)
    private fun ping(address: MemberAddress) {
        sendMessage(address, object : MessageWriter<DataOutput> {
            override fun writeTo(dos: DataOutput) {
                dos.write(0x01)
            }
        })
        val member = updateMember(null, address, { m: Member? ->
            m ?: Member(MemberState.DEAD, 0.toByte(), 0.toByte(), 0)
        })
        if (member != null) {
            scheduleTask(address, Runnable {
                try {
                    indirectPing(address, member)
                } catch (ex: IOException) {
                    LOGGER.log(Level.SEVERE, "Exception thrown while performing indirect ping of $address", ex)
                }
            }, options.pingTimeoutMs, TimeUnit.MILLISECONDS)
        }
    }

    @Throws(IOException::class)
    private fun indirectPing(address: MemberAddress, member: Member) {
        updateMember(null, address, { m: Member? -> m?.merge(member.withState(MemberState.SUSPICIOUS)) })
        var i = options.numberOfIndirectEndPoints
        for (relay in randomNodes { a, _ -> a != address }) {
            if (--i < 0) {
                break
            }
            sendMessage(relay, object : MessageWriter<DataOutput> {
                override fun writeTo(dos: DataOutput) {
                    dos.write(0x05)
                    writeAddress(dos, address)
                }
            })
        }
        scheduleTask(address, Runnable {
            val dead = updateMember(null, address, { m: Member? -> m?.merge(member.withState(MemberState.DEAD)) })
            scheduleTask(address, Runnable {
                // only prune the state if it hasn't changed
                updateMember(null, address, { m: Member? -> if (m != dead) m else null })
            }, options.deathTimeoutMs, TimeUnit.MILLISECONDS)
        }, options.indirectPingTimeoutMs, TimeUnit.MILLISECONDS)
    }

    @Throws(IOException::class)
    private fun handleMessage(address: MemberAddress, input: DataInput) {
        val version = input.readByte()
        if (version.toInt() == 0) {
            val b = input.readByte()
            when (b.toInt()) {
                0x00 -> handleDirectAck(address, input)
                0x01 -> handleDirectPing(address, input)
                0x04, 0x05 -> handleRequest(address, input, (b and 0x01))
                0x06, 0x07 -> handleForwarded(address, input, (b and 0x01))
            }
        } else {
            LOGGER.log(Level.SEVERE, "Unknown protocol version received: $version")
        }
    }

    private fun removeAndCancel(address: MemberAddress) {
        val f = waiting.remove(address)
        f?.cancel(true)
    }

    @Throws(IOException::class)
    private fun handleDirectAck(address: MemberAddress, input: DataInput) {
        removeAndCancel(address) // if we were waiting to hear from them - here they are!
        handleEvents(address, input)
    }

    @Throws(IOException::class)
    private fun handleDirectPing(address: MemberAddress, input: DataInput) {
        removeAndCancel(address) // if we were waiting to hear from them - here they are!
        handleEvents(address, input)
        sendMessage(address, object : MessageWriter<DataOutput> {
            override fun writeTo(dos: DataOutput) {
                dos.write(0x00)
            }
        })
    }

    @Throws(IOException::class)
    private fun handleRequest(address: MemberAddress, input: DataInput, b: Byte) {
        removeAndCancel(address) // if we were waiting to hear from them - here they are!
        val destination = parseAddress(input)
        handleEvents(address, input)
        sendMessage(address, object : MessageWriter<DataOutput> {
            override fun writeTo(dos: DataOutput) {
                dos.write(b.toInt() or 0x06)
                writeAddress(dos, destination)
            }
        })
    }

    @Throws(IOException::class)
    private fun handleForwarded(address: MemberAddress, input: DataInput, b: Byte) {
        removeAndCancel(address) // if we were waiting to hear from them - here they are!
        val source = parseAddress(input)
        handleEvents(address, input)
        when (b.toInt()) {
            0x00 -> removeAndCancel(source)
            0x01 -> sendMessage(address, object : MessageWriter<DataOutput> {
                override fun writeTo(dos: DataOutput) {
                    dos.write(0x04)
                    writeAddress(dos, source)
                }
            })
        }
    }

    private fun updateMember(from: MemberAddress?, address: MemberAddress, update: (Member?) -> Member?): Member? {
        val oldMember = members[address]
        val newMember = update.invoke(oldMember)
        if (oldMember != newMember) {
            if (newMember == null) {
                members.remove(address)
            } else {
                members[address] = newMember
            }
            notifyListeners(from, address, newMember, oldMember)
        }
        return newMember
    }

    private fun handleEvents(from: MemberAddress, input: DataInput) = try {
        val senderGeneration = input.readByte()
        val senderService = input.readByte()
        val senderServicePort = input.readShort()
        val senderMember = Member(MemberState.ALIVE,
                senderGeneration,
                senderService,
                senderServicePort)
        updateMember(null, from, { m: Member? -> m?.merge(senderMember) ?: senderMember })
        val myState = MemberState.values()[input.readByte().toInt()]
        val myGeneration = input.readByte()
        if (myState === MemberState.SUSPICIOUS || myState === MemberState.DEAD) {
            var newGeneration = (myGeneration + 1).toByte()
            if (isLaterGeneration(generation, newGeneration)) {
                newGeneration = generation
            }
            generation = newGeneration
        }

        // This is an infinte loop that will be broken when we hit an exception,
        // because DataInput doesn't let us see whether we're at the end
        while (true) {
            val address = parseAddress(input)
            val state = MemberState.values()[input.readByte().toInt()]
            val generation = input.readByte()
            val serviceByte = if (state === MemberState.ALIVE) input.readByte() else 0
            val servicePort = if (state === MemberState.ALIVE) input.readShort() else 0
            val newMember = Member(state, generation, serviceByte, servicePort)
            updateMember(from, address) { m: Member? ->
                when {
                    m != null -> {
                        m.merge(newMember)
                    }
                    else -> when (state) {
                        MemberState.ALIVE, MemberState.SUSPICIOUS -> newMember
                        else -> null
                    }

                    // if we don't already know about this node and we get a DEAD or a LEFT: we don't care.
                    // Just ignore it. This means that our pruning will actually remove nodes, because we
                    // won't keep broadcasting dead nodes indefinitely.
                }
            }
        }
    } catch (ex: IOException) {
        // this is fine - just means we're done with handling events
    }

    private fun notifyListeners(from: MemberAddress?, address: MemberAddress, newState: Member?, oldState: Member?) {
        for (listener in listeners.values) {
            listener.accept(from, address, newState, oldState)
        }
    }

    fun addListener(key: Any, listener: Listener) {
        executor.execute(loggingExceptions(Runnable { listeners[key] = listener }))
    }

    fun removeListener(key: Any) {
        executor.execute(loggingExceptions(Runnable { listeners.remove(key) }))
    }

    companion object {
        private val LOGGER = Logger.getLogger(Gossiper::class.java.canonicalName)

        @Throws(IOException::class)
        private fun parseAddress(stream: DataInput): MemberAddress {
            val addressBytes = ByteArray(4)
            stream.readFully(addressBytes)
            return MemberAddress(
                    (Inet4Address.getByAddress(addressBytes) as Inet4Address),
                    stream.readShort())
        }

        @Throws(IOException::class)
        private fun writeAddress(output: DataOutput, address: MemberAddress) {
            output.write(address.address.address)
            output.writeShort(address.port.toInt())
        }

        @Throws(IOException::class)
        private fun writeEvent(output: DataOutput, address: MemberAddress, member: Member) {
            writeAddress(output, address)
            output.write(member.state.ordinal)
            output.write(member.generation.toInt())
            if (member.state === MemberState.ALIVE) {
                output.write(member.serviceByte.toInt())
                output.writeShort(member.servicePort.toInt())
            }
        }
    }

    init {
        members = HashMap()
        waiting = HashMap()
        this.serviceByte = serviceByte.toByte()
        this.servicePort = servicePort.toShort()
        this.options = options
        executor = ScheduledThreadPoolExecutor(1)
        this.socket = socket
        listeners = HashMap()
    }
}