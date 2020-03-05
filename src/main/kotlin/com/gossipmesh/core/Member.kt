package com.gossipmesh.core

import java.util.*

class Member internal constructor(val state: MemberState, val generation: Byte, val serviceByte: Byte, val servicePort: Short) {
    @JvmField
    var timesMentioned: Long = 0
    fun merge(other: Member): Member {
        return if (isLaterGeneration(other.generation, generation)) {
            other
        } else if (other.state.ordinal > state.ordinal) {
            other
        } else {
            this
        }
    }

    fun withState(state: MemberState): Member {
        return Member(state, generation, serviceByte, servicePort)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val nodeState = o as Member
        return generation == nodeState.generation && state === nodeState.state && serviceByte == nodeState.serviceByte && servicePort == nodeState.servicePort
    }

    override fun hashCode(): Int {
        return Objects.hash(state, generation, serviceByte, servicePort)
    }

    override fun toString(): String {
        return String.format("%s[%s]{%s:%s}",
                state, generation,
                serviceByte, servicePort)
    }

    companion object {
        // is `gen1` later than `gen2`?
        @JvmStatic
        fun isLaterGeneration(gen1: Byte, gen2: Byte): Boolean {
            return (0 < gen1 - gen2 && gen1 - gen2 < 191
                    || gen1 - gen2 <= -191)
        }
    }

}