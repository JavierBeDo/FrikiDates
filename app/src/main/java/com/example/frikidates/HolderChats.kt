package com.example.frikidates

data class HolderChats(
    val matchId: String, // Añadir matchId
    val userId: String,
    val username: String,
    val lastMessage: String,
    val timestamp: Long
)

