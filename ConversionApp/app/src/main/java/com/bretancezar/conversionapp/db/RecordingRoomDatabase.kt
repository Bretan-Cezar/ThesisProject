package com.bretancezar.conversionapp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bretancezar.conversionapp.domain.Recording
import com.bretancezar.conversionapp.utils.Converters


@Database(entities = [Recording::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class RecordingRoomDatabase: RoomDatabase() {

    abstract fun entityDao(): RecordingDAO

    companion object {

        @Volatile
        private var INSTANCE: RecordingRoomDatabase? = null

        fun getDatabase(
            context: Context
        ) : RecordingRoomDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RecordingRoomDatabase::class.java,
                    "database"
                )
                    .fallbackToDestructiveMigration()
                    .addTypeConverter(Converters())
                    .build()

                INSTANCE = instance

                return instance
            }
        }
    }
}