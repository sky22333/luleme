package com.luleme.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.luleme.data.local.entity.RecordEntity

data class DailyCount(
    val date: String,
    val count: Int
)

@Dao
abstract class RecordDao {
    @Query("SELECT * FROM records WHERE date >= :startDate AND date <= :endDate ORDER BY timestamp DESC")
    abstract suspend fun getRecordsBetween(startDate: String, endDate: String): List<RecordEntity>
    
    @Insert
    abstract suspend fun insertRecord(record: RecordEntity): Long

    @Insert
    abstract suspend fun insertRecords(records: List<RecordEntity>)

    @Query(
        """
        UPDATE records
        SET timestamp = :timestamp,
            date = :date,
            note = :note,
            updated_at = :updatedAt
        WHERE id = :id
        """
    )
    abstract suspend fun updateRecord(id: Long, timestamp: Long, date: String, note: String?, updatedAt: Long)

    @Query("DELETE FROM records WHERE id = :id")
    abstract suspend fun deleteRecord(id: Long)

    @Query("SELECT * FROM records WHERE date = :date ORDER BY timestamp ASC")
    abstract suspend fun getRecordsByDate(date: String): List<RecordEntity>

    @Query(
        """
        SELECT date, COUNT(*) as count
        FROM records
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY date
        """
    )
    abstract suspend fun getDailyCountsBetween(startDate: String, endDate: String): List<DailyCount>
    
    @Query("DELETE FROM records")
    abstract suspend fun clearAll()

    @Query("SELECT * FROM records ORDER BY timestamp DESC")
    abstract suspend fun getAllRecords(): List<RecordEntity>

    @Transaction
    suspend fun replaceAll(records: List<RecordEntity>) {
        clearAll()
        if (records.isNotEmpty()) {
            insertRecords(records)
        }
    }
}
