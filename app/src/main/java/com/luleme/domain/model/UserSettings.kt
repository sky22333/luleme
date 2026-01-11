package com.luleme.domain.model

data class UserSettings(
    val age: Int,
    val lockEnabled: Boolean,
    val pinHash: String?
)
