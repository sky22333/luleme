package com.luleme.domain.model

data class Record(
    val id: Long = 0,
    val timestamp: Long,
    val date: String,
    val note: String? = null
)
