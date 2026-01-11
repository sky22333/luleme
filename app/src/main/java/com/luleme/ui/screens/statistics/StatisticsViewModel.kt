package com.luleme.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luleme.domain.model.Record
import com.luleme.domain.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class StatisticsUiState(
    val weekData: Map<DayOfWeek, Int> = emptyMap(),
    val monthData: Map<LocalDate, Int> = emptyMap(),
    val totalCount: Int = 0,
    val maxStreak: Int = 0,
    val averageFrequency: Float = 0f,
    val loading: Boolean = false
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val recordRepository: RecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState(loading = true))
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            
            val allRecords = recordRepository.getAllRecords()
            val today = LocalDate.now()

            // Week Data
            val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekData = mutableMapOf<DayOfWeek, Int>()
            for (i in 0..6) {
                val date = startOfWeek.plusDays(i.toLong())
                val count = allRecords.count { it.date == date.format(DateTimeFormatter.ISO_DATE) }
                weekData[date.dayOfWeek] = count
            }

            // Month Data (Current Month)
            val startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth())
            val lengthOfMonth = today.lengthOfMonth()
            val monthData = mutableMapOf<LocalDate, Int>()
            for (i in 0 until lengthOfMonth) {
                val date = startOfMonth.plusDays(i.toLong())
                val count = allRecords.count { it.date == date.format(DateTimeFormatter.ISO_DATE) }
                monthData[date] = count
            }

            // All Time Stats
            val totalCount = allRecords.size
            val maxStreak = calculateMaxStreak(allRecords)
            
            val firstRecord = allRecords.minByOrNull { it.timestamp }
            val average = if (firstRecord != null) {
                val days = ChronoUnit.DAYS.between(LocalDate.parse(firstRecord.date), today) + 1
                val weeks = kotlin.math.ceil(days / 7.0).toFloat()
                totalCount.toFloat() / weeks
            } else {
                0f
            }

            _uiState.value = StatisticsUiState(
                weekData = weekData,
                monthData = monthData,
                totalCount = totalCount,
                maxStreak = maxStreak,
                averageFrequency = average,
                loading = false
            )
        }
    }

    private fun calculateMaxStreak(records: List<Record>): Int {
        if (records.isEmpty()) return 0
        
        val sortedDates = records.map { LocalDate.parse(it.date) }.distinct().sorted()
        var maxStreak = 0
        var currentStreak = 0
        
        // This logic calculates consecutive days with records. 
        // Note: Requirements say "Longest continuous record days", assuming it means days with at least one record.
        // However, for this app, maybe streak means "days WITHOUT record"? 
        // Usually streaks in habit trackers are for "doing the habit". 
        // But here, maybe "not doing it" is the goal? 
        // Requirements say "Core concept: Scientific management". 
        // Let's assume streak means "Consecutive days with records" for now as per standard definition,
        // although "No Fap" apps usually track days *without*.
        // The requirements don't explicitly say "No Fap". It says "Manage frequency".
        // "Longest continuous record days" (最长连续记录天数) implies days WITH records.
        
        for (i in 0 until sortedDates.size) {
            if (i == 0) {
                currentStreak = 1
            } else {
                val prev = sortedDates[i - 1]
                val curr = sortedDates[i]
                if (ChronoUnit.DAYS.between(prev, curr) == 1L) {
                    currentStreak++
                } else {
                    maxStreak = maxOf(maxStreak, currentStreak)
                    currentStreak = 1
                }
            }
        }
        maxStreak = maxOf(maxStreak, currentStreak)
        
        return maxStreak
    }
}
