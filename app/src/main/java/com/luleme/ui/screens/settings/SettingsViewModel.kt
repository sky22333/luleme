package com.luleme.ui.screens.settings

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.luleme.domain.model.Record
import com.luleme.domain.model.UserSettings
import com.luleme.domain.repository.RecordRepository
import com.luleme.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.StringReader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SettingsUiState(
    val age: Int = 25,
    val lockEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val recordRepository: RecordRepository,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = userSettingsRepository.getSettings()
            if (settings != null) {
                _uiState.value = SettingsUiState(
                    age = settings.age,
                    lockEnabled = settings.lockEnabled
                )
            } else {
                // Initialize default settings
                val default = UserSettings(25, false)
                userSettingsRepository.saveSettings(default)
                _uiState.value = SettingsUiState(25, false)
            }
        }
    }

    fun updateAge(age: Int) {
        viewModelScope.launch {
            val current = _uiState.value
            saveSettings(current.copy(age = age))
        }
    }

    fun toggleLock(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            saveSettings(current.copy(lockEnabled = enabled))
        }
    }

    private suspend fun saveSettings(state: SettingsUiState) {
        userSettingsRepository.saveSettings(
            UserSettings(
                age = state.age,
                lockEnabled = state.lockEnabled
            )
        )
        _uiState.value = state
    }

    fun clearAllData() {
        viewModelScope.launch {
            recordRepository.clearAll()
        }
    }
    
    suspend fun getAllRecordsJson(): String {
        val records = recordRepository.getAllRecords().map { it.toBackupRecord() }
        val payload = BackupPayload(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            records = records
        )
        return gson.toJson(payload)
    }

    suspend fun restoreData(json: String): Boolean {
        return try {
            val records = parseRecordsFromJson(json) ?: return false
            recordRepository.replaceAllRecords(records)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @Keep
    private data class BackupPayload(
        @SerializedName("version")
        val version: Int,
        @SerializedName("exportedAt")
        val exportedAt: Long,
        @SerializedName("records")
        val records: List<BackupRecord>
    )

    @Keep
    private data class BackupRecord(
        @SerializedName("uuid")
        val uuid: String,
        @SerializedName("timestamp")
        val timestamp: Long,
        @SerializedName("note")
        val note: String?,
        @SerializedName("createdAt")
        val createdAt: Long,
        @SerializedName("updatedAt")
        val updatedAt: Long
    )

    private fun parseRecordsFromJson(json: String): List<Record>? {
        val sanitized = json.trim().trimStart('\uFEFF')
        if (sanitized.isEmpty()) return null
        val reader = JsonReader(StringReader(sanitized))
        reader.isLenient = false
        val element = JsonParser.parseReader(reader)
        if (!element.isJsonObject) return null
        val obj = element.asJsonObject
        val version = parseInt(obj.get("version")) ?: return null
        if (version != 1) return null
        val recordsElement = if (obj.has("records")) obj.get("records") else null
        return if (recordsElement != null && recordsElement.isJsonArray) {
            parseRecordsArray(recordsElement)
        } else {
            null
        }
    }

    private fun parseRecordsArray(element: JsonElement): List<Record> {
        if (!element.isJsonArray) return emptyList()
        val array = element.asJsonArray
        val records = mutableListOf<Record>()
        val uuids = mutableSetOf<String>()
        array.forEach { item ->
            if (!item.isJsonObject) throw BackupFormatException()
            val obj = item.asJsonObject
            val uuid = parseString(obj.get("uuid"))?.takeIf { it.isNotBlank() } ?: throw BackupFormatException()
            if (!uuids.add(uuid)) throw BackupFormatException()
            val timestamp = parseLong(obj.get("timestamp"))?.takeIf { it > 0 } ?: throw BackupFormatException()
            val createdAt = parseLong(obj.get("createdAt"))?.takeIf { it > 0 } ?: throw BackupFormatException()
            val updatedAt = parseLong(obj.get("updatedAt"))?.takeIf { it >= createdAt } ?: throw BackupFormatException()
            val note = parseString(obj.get("note"))
            records.add(
                Record(
                    uuid = uuid,
                    timestamp = timestamp,
                    date = timestamp.toDateString(),
                    note = note,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            )
        }
        return records
    }

    private fun parseInt(element: JsonElement?): Int? {
        return parseLong(element)?.takeIf { it in 1L..Int.MAX_VALUE.toLong() }?.toInt()
    }

    private fun parseLong(element: JsonElement?): Long? {
        if (element == null || element.isJsonNull) return null
        if (element.isJsonPrimitive) {
            val prim = element.asJsonPrimitive
            return when {
                prim.isNumber -> prim.asLong
                prim.isString -> prim.asString.toLongOrNull()
                else -> null
            }
        }
        return null
    }

    private fun parseString(element: JsonElement?): String? {
        if (element == null || element.isJsonNull) return null
        if (element.isJsonPrimitive) {
            val prim = element.asJsonPrimitive
            return if (prim.isString) prim.asString else null
        }
        return null
    }

    private fun Record.toBackupRecord(): BackupRecord {
        return BackupRecord(
            uuid = uuid,
            timestamp = timestamp,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Long.toDateString(): String {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ISO_DATE)
    }

    private class BackupFormatException : IllegalArgumentException()
}
