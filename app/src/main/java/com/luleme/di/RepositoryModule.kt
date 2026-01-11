package com.luleme.di

import com.luleme.data.repository.RecordRepositoryImpl
import com.luleme.data.repository.UserSettingsRepositoryImpl
import com.luleme.domain.repository.RecordRepository
import com.luleme.domain.repository.UserSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecordRepository(
        recordRepositoryImpl: RecordRepositoryImpl
    ): RecordRepository

    @Binds
    @Singleton
    abstract fun bindUserSettingsRepository(
        userSettingsRepositoryImpl: UserSettingsRepositoryImpl
    ): UserSettingsRepository
}
