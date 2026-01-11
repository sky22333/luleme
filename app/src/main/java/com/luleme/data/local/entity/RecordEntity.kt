package com.luleme.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,           // Unix Timestamp
    
    @ColumnInfo(name = "date")
    val date: String,              // yyyy-MM-dd
    
    @ColumnInfo(name = "note")
    val note: String? = null,      // Optional note (encrypted)
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
