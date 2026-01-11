package com.luleme.ui.screens.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luleme.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var pinHash: String? = null
    private var isLockEnabled: Boolean = false

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = userSettingsRepository.getSettings()
            pinHash = settings?.pinHash
            isLockEnabled = settings?.lockEnabled ?: false
            _isLoading.value = false
        }
    }

    fun verifyPin(pin: String): Boolean {
        if (!isLockEnabled || pinHash == null) return true
        return hashPin(pin) == pinHash
    }

    private fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
