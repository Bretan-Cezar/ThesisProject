package com.bretancezar.conversionapp.di

import android.content.Context
import com.bretancezar.conversionapp.db.RecordingDAO
import com.bretancezar.conversionapp.db.RecordingRoomDatabase
import com.bretancezar.conversionapp.repository.RecordingRepository
import com.bretancezar.conversionapp.repository.RecordingRepositoryImpl
import com.bretancezar.conversionapp.service.RetrofitService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Singleton
    @Provides
    fun provideRetrofitService(): RetrofitService {

        return RetrofitService.getInstance()
    }

    @Singleton
    @Provides
    fun provideRecordingDAO(@ApplicationContext context: Context): RecordingDAO {

        return RecordingRoomDatabase.getDatabase(context).entityDao()
    }

    @Module
    @InstallIn(SingletonComponent::class)
    interface AppModuleInt {

        @Singleton
        @Binds
        fun provideRecordingRepository(repo: RecordingRepositoryImpl): RecordingRepository
    }
}