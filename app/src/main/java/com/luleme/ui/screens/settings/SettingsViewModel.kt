package com.luleme.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.luleme.domain.model.Record
import com.luleme.domain.model.UserSettings
import com.luleme.domain.repository.RecordRepository
import com.luleme.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

data class SettingsUiState(
    val age: Int = 25,
    val lockEnabled: Boolean = false,
    val hasPin: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val recordRepository: RecordRepository,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var currentPinHash: String? = null

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = userSettingsRepository.getSettings()
            if (settings != null) {
                currentPinHash = settings.pinHash
                _uiState.value = SettingsUiState(
                    age = settings.age,
                    lockEnabled = settings.lockEnabled,
                    hasPin = settings.pinHash != null
                )
            } else {
                // Initialize default settings
                val default = UserSettings(25, false, null)
                userSettingsRepository.saveSettings(default)
                _uiState.value = SettingsUiState(25, false, false)
            }
        }
    }

    fun updateAge(age: Int) {
        viewModelScope.launch {
            val current = _uiState.value
            saveSettings(current.copy(age = age), currentPinHash)
        }
    }

    fun toggleLock(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            // Only allow enabling if PIN is set
            if (enabled && currentPinHash == null) {
                // Should not happen if UI is correct, but just in case
                return@launch
            }
            saveSettings(current.copy(lockEnabled = enabled), currentPinHash)
        }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            val hash = hashPin(pin)
            currentPinHash = hash
            val current = _uiState.value
            // Auto enable lock when PIN is set for the first time
            val newLockState = true
            saveSettings(current.copy(lockEnabled = newLockState), hash)
        }
    }
    
    fun verifyPin(pin: String): Boolean {
        if (currentPinHash == null) return true // No PIN set
        return hashPin(pin) == currentPinHash
    }

    private suspend fun saveSettings(state: SettingsUiState, pinHash: String?) {
        userSettingsRepository.saveSettings(
            UserSettings(
                age = state.age,
                lockEnabled = state.lockEnabled,
                pinHash = pinHash
            )
        )
        _uiState.value = state.copy(hasPin = pinHash != null)
        currentPinHash = pinHash
    }
    
    private fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            recordRepository.clearAll()
        }
    }
    
    suspend fun getAllRecordsJson(): String {
        val records = recordRepository.getAllRecords()
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
            if (records.isNotEmpty()) {
                recordRepository.importRecords(records)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private data class BackupPayload(
        val version: Int,
        val exportedAt: Long,
        val records: List<Record>
    )

    private fun parseRecordsFromJson(json: String): List<Record>? {
        val element = JsonParser.parseString(json)
        return when {
            element.isJsonArray -> parseRecordsArray(element)
            element.isJsonObject -> {
                val obj = element.asJsonObject
                val recordsElement = if (obj.has("records")) obj.get("records") else null
                if (recordsElement != null && recordsElement.isJsonArray) {
                    parseRecordsArray(recordsElement)
                } else {
                    null
                }
            }
            else -> null
        }
    }

    private fun parseRecordsArray(element: JsonElement): List<Record> {
        if (!element.isJsonArray) return emptyList()
        val array = element.asJsonArray
        val records = mutableListOf<Record>()
        array.forEach { item ->
            if (!item.isJsonObject) return@forEach
            val obj = item.asJsonObject
            val timestamp = parseLong(obj.get("timestamp")) ?: return@forEach
            val date = parseString(obj.get("date")) ?: return@forEach
            val id = parseLong(obj.get("id")) ?: 0L
            val note = parseString(obj.get("note"))
            records.add(
                Record(
                    id = id,
                    timestamp = timestamp,
                    date = date,
                    note = note
                )
            )
        }
        return records
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
            return if (prim.isString) prim.asString else prim.toString()
        }
        return null
    }
}
