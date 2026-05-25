package com.luleme.domain.model

data class UserSettings(
    val age: Int,
    val lockEnabled: Boolean,
    val webDavUrl: String = "",
    val webDavUsername: String = "",
    val webDavPassword: String = "",
    val webDavDirectory: String = ""
)
