package com.luleme.data.repository

import com.luleme.data.encryption.EncryptionManager
import com.luleme.data.local.dao.RecordDao
import com.luleme.data.local.entity.RecordEntity
import com.luleme.domain.model.Record
import com.luleme.domain.repository.RecordRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

class RecordRepositoryImpl @Inject constructor(
    private val dao: RecordDao,
    private val encryptionManager: EncryptionManager
) : RecordRepository {

    override suspend fun getTodayRecords(): List<Record> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        return dao.getRecordsBetween(today, today).map { it.toDomain() }
    }

    override suspend fun getRecordsBetween(startDate: String, endDate: String): List<Record> {
        return dao.getRecordsBetween(startDate, endDate).map { it.toDomain() }
    }

    override suspend fun getRecordsByDate(date: String): List<Record> {
        return dao.getRecordsByDate(date).map { it.toDomain() }
    }

    override suspend fun getDailyCountsBetween(startDate: String, endDate: String): Map<String, Int> {
        return dao.getDailyCountsBetween(startDate, endDate).associate { it.date to it.count }
    }

    override suspend fun addRecord(note: String?): Long {
        return addRecord(System.currentTimeMillis(), note)
    }

    override suspend fun addRecord(timestamp: Long, note: String?): Long {
        val dateString = timestamp.toDateString()
        val encryptedNote = note?.let { encryptionManager.encryptData(it) }
        val now = System.currentTimeMillis()

        val entity = RecordEntity(
            uuid = UUID.randomUUID().toString(),
            timestamp = timestamp,
            date = dateString,
            note = encryptedNote,
            createdAt = now,
            updatedAt = now
        )
        return dao.insertRecord(entity)
    }

    override suspend fun updateRecord(record: Record) {
        val encryptedNote = record.note?.let { encryptionManager.encryptData(it) }
        dao.updateRecord(
            id = record.id,
            timestamp = record.timestamp,
            date = record.timestamp.toDateString(),
            note = encryptedNote,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun deleteRecord(id: Long) {
        dao.deleteRecord(id)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }

    override suspend fun getAllRecords(): List<Record> {
        return dao.getAllRecords().map { it.toDomain() }
    }

    override suspend fun replaceAllRecords(records: List<Record>) {
        val entities = records.map { record ->
            val encryptedNote = record.note?.let { encryptionManager.encryptData(it) }
            RecordEntity(
                id = 0, // Reset ID to avoid conflicts and auto-generate
                uuid = record.uuid.ifBlank { UUID.randomUUID().toString() },
                timestamp = record.timestamp,
                date = record.timestamp.toDateString(),
                note = encryptedNote,
                createdAt = record.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
                updatedAt = record.updatedAt.takeIf { it > 0 } ?: System.currentTimeMillis()
            )
        }
        dao.replaceAll(entities)
    }

    private fun RecordEntity.toDomain(): Record {
        val decryptedNote = this.note?.let { encryptionManager.decryptData(it) }
        return Record(
            id = this.id,
            uuid = this.uuid,
            timestamp = this.timestamp,
            date = this.date,
            note = decryptedNote,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    private fun Long.toDateString(): String {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ISO_DATE)
    }
}
