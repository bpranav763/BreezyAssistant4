package com.breezy.assistant

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [CallerInfo::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callerDao(): CallerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "breezy_callerid_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
