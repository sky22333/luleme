package com.luleme.di

import android.content.Context
import androidx.room.Room
import com.luleme.data.local.dao.RecordDao
import com.luleme.data.local.dao.UserSettingsDao
import com.luleme.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "luleme_db"
        )
        .fallbackToDestructiveMigration() // Allow schema changes
        .build()
    }

    @Provides
    fun provideRecordDao(database: AppDatabase): RecordDao {
        return database.recordDao()
    }

    @Provides
    fun provideUserSettingsDao(database: AppDatabase): UserSettingsDao {
        return database.userSettingsDao()
    }
}
