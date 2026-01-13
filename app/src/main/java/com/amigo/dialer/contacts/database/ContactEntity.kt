package com.amigo.dialer.contacts.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String?,
    val isFavorite: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
