package com.luleme.domain.repository

import com.luleme.domain.model.Record

interface RecordRepository {
    suspend fun getTodayRecords(): List<Record>
    suspend fun getRecordsBetween(startDate: String, endDate: String): List<Record>
    suspend fun getRecordsByDate(date: String): List<Record>
    suspend fun getDailyCountsBetween(startDate: String, endDate: String): Map<String, Int>
    suspend fun addRecord(note: String? = null): Long
    suspend fun addRecord(timestamp: Long, note: String? = null): Long
    suspend fun updateRecord(record: Record)
    suspend fun deleteRecord(id: Long)
    suspend fun clearAll()
    suspend fun getAllRecords(): List<Record>
    suspend fun replaceAllRecords(records: List<Record>)
}
