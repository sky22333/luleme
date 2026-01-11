package com.luleme.domain.repository

import com.luleme.domain.model.UserSettings

interface UserSettingsRepository {
    suspend fun getSettings(): UserSettings?
    suspend fun saveSettings(settings: UserSettings)
}
