package com.amigo.dialer.recentcall.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RecentCallEntity::class],
    version = 2,
    exportSchema = false
)
abstract class RecentCallsDatabase : RoomDatabase() {

    abstract fun recentCallDao(): RecentCallDao

    companion object {
        @Volatile
        private var INSTANCE: RecentCallsDatabase? = null

        fun getDatabase(context: Context): RecentCallsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RecentCallsDatabase::class.java,
                    "recent_calls_db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
