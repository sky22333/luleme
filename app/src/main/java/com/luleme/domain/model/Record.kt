package com.luleme.domain.model

data class Record(
    val id: Long = 0,
    val uuid: String = "",
    val timestamp: Long,
    val date: String,
    val note: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
