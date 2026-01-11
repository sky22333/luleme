package com.luleme.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.luleme.data.local.dao.RecordDao
import com.luleme.data.local.dao.UserSettingsDao
import com.luleme.data.local.entity.RecordEntity
import com.luleme.data.local.entity.UserSettingsEntity

@Database(entities = [RecordEntity::class, UserSettingsEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    abstract fun userSettingsDao(): UserSettingsDao
}
