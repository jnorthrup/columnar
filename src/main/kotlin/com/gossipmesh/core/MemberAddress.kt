package com.gossipmesh.core

import java.net.Inet4Address
import java.util.*

class MemberAddress(val address: Inet4Address, val port: Short) {
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as MemberAddress
        return port == that.port && address == that.address
    }

    override fun hashCode(): Int {
        return Objects.hash(address, port)
    }

    override fun toString(): String {
        return "udp://" + address.hostAddress + ":" + (port.toInt() and 0xFFFF)
    }

}