package com.luleme.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luleme.domain.model.Record
import com.luleme.domain.repository.RecordRepository
import com.luleme.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val todayRecords: List<Record>,
        val weekCount: Int,
        val age: Int
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = HomeUiState.Loading
            }
            try {
                val todayRecords = recordRepository.getTodayRecords()
                
                // Get this week's records
                val today = LocalDate.now()
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                
                val weekRecords = recordRepository.getRecordsBetween(
                    startOfWeek.format(DateTimeFormatter.ISO_DATE),
                    endOfWeek.format(DateTimeFormatter.ISO_DATE)
                )
                
                val settings = userSettingsRepository.getSettings()
                val age = settings?.age ?: 25 // Default age

                _uiState.value = HomeUiState.Success(
                    todayRecords = todayRecords,
                    weekCount = weekRecords.size,
                    age = age
                )
            } catch (e: Exception) {
                // If we are already showing data (Success), don't replace it with an error screen on refresh failure.
                // Only show Error state if we have nothing to show.
                if (_uiState.value !is HomeUiState.Success) {
                    _uiState.value = HomeUiState.Error(e.message ?: "未知错误")
                } else {
                    // TODO: In a real app, we might want to emit a one-time event (like a Snackbar) here
                    // to notify the user that the refresh failed, without destroying the UI.
                    e.printStackTrace()
                }
            }
        }
    }

    fun recordToday() {
        viewModelScope.launch {
            try {
                recordRepository.addRecord()
                loadData(showLoading = false)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
