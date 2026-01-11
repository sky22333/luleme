package com.luleme.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,               // Singleton
    
    @ColumnInfo(name = "age")
    val age: Int,
    
    @ColumnInfo(name = "lock_enabled")
    val lockEnabled: Boolean = false, // Default false as per request (optional)
    
    @ColumnInfo(name = "pin_hash")
    val pinHash: String? = null
)
