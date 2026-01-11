package com.luleme.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.luleme.data.local.entity.RecordEntity

@Dao
interface RecordDao {
    @Query("SELECT * FROM records WHERE date >= :startDate AND date <= :endDate ORDER BY timestamp DESC")
    suspend fun getRecordsBetween(startDate: String, endDate: String): List<RecordEntity>
    
    @Insert
    suspend fun insertRecord(record: RecordEntity): Long

    @Insert
    suspend fun insertRecords(records: List<RecordEntity>)
    
    @Query("DELETE FROM records")
    suspend fun clearAll()

    @Query("SELECT * FROM records ORDER BY timestamp DESC")
    suspend fun getAllRecords(): List<RecordEntity>
}
