package com.gossipmesh.core

interface Listener {
    fun accept(from: MemberAddress?, address: MemberAddress?, newMember: Member?, oldMember: Member?)
}