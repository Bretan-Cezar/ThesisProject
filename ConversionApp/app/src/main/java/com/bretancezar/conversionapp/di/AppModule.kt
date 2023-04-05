package com.bretancezar.conversionapp.di

import android.app.Application
import com.bretancezar.conversionapp.controller.AppController
import com.bretancezar.conversionapp.db.RecordingDAO
import com.bretancezar.conversionapp.db.RecordingRoomDatabase
import com.bretancezar.conversionapp.repository.RecordingRepository
import com.bretancezar.conversionapp.repository.RecordingRepositoryImpl
import com.bretancezar.conversionapp.service.RecorderService
import com.bretancezar.conversionapp.service.RetrofitService
import com.bretancezar.conversionapp.service.StorageService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule(private val application: Application? = null) {

    @Singleton
    @Provides
    fun provideRetrofitService(): RetrofitService {

        return RetrofitService.getInstance()!!
    }

    @Singleton
    @Provides
    fun provideRecorderService(): RecorderService {

        return RecorderService(application!!)
    }

    @Singleton
    @Provides
    fun provideRecordingDAO(): RecordingDAO {

        return RecordingRoomDatabase.getDatabase(application!!).entityDao()
    }

    @Module
    @InstallIn(SingletonComponent::class)
    interface AppModuleInt {

        @Singleton
        @Binds
        fun provideRecordingRepository(repo: RecordingRepositoryImpl): RecordingRepository

        @Singleton
        fun provideStorageService(): StorageService

//        @Singleton
//        @Binds
//        fun provideAppController(): AppController
    }
}