package com.luleme.data.repository

import com.luleme.data.encryption.EncryptionManager
import com.luleme.data.local.dao.RecordDao
import com.luleme.data.local.entity.RecordEntity
import com.luleme.domain.model.Record
import com.luleme.domain.repository.RecordRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

    override suspend fun addRecord(note: String?) {
        val today = LocalDate.now()
        val dateString = today.format(DateTimeFormatter.ISO_DATE)
        
        val encryptedNote = note?.let { encryptionManager.encryptData(it) }
        
        val entity = RecordEntity(
            timestamp = System.currentTimeMillis(),
            date = dateString,
            note = encryptedNote
        )
        dao.insertRecord(entity)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }

    override suspend fun getAllRecords(): List<Record> {
        return dao.getAllRecords().map { it.toDomain() }
    }

    override suspend fun importRecords(records: List<Record>) {
        val entities = records.map { record ->
            val encryptedNote = record.note?.let { encryptionManager.encryptData(it) }
            RecordEntity(
                id = 0, // Reset ID to avoid conflicts and auto-generate
                timestamp = record.timestamp,
                date = record.date,
                note = encryptedNote
            )
        }
        dao.insertRecords(entities)
    }

    private fun RecordEntity.toDomain(): Record {
        val decryptedNote = this.note?.let { encryptionManager.decryptData(it) }
        return Record(
            id = this.id,
            timestamp = this.timestamp,
            date = this.date,
            note = decryptedNote
        )
    }
}
