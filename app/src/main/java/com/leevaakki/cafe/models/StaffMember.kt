package com.leevaakki.cafe.models

import com.google.firebase.Timestamp

data class StaffMember(
    val id: String = "",
    val name: String = "",
    val role: String = "", // Chef, Server, Delivery, Manager
    val branchId: String = "",
    val phone: String = "",
    val email: String = "",
    val joinedDate: Timestamp = Timestamp.now(),
    val salary: Double = 0.0,
    val status: String = "Active" // Active, On Leave, Resigned
)
