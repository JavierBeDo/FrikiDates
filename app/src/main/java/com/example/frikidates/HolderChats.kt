package com.example.frikidates

import com.google.firebase.Timestamp

data class HolderChats(
    val userId: String = "",
    val username: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0L
)

