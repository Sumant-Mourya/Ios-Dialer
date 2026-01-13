package com.amigo.dialer.contacts.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ContactEntity::class],
    version = 3,
    exportSchema = false
)
abstract class ContactsDatabase : RoomDatabase() {
    
    abstract fun contactDao(): ContactDao
    
    companion object {
        @Volatile
        private var INSTANCE: ContactsDatabase? = null
        
        fun getDatabase(context: Context): ContactsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ContactsDatabase::class.java,
                    "contacts_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
