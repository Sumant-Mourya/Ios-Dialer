package com.amigo.dialer.recentcall.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_calls")
data class RecentCallEntity(
    @PrimaryKey val number: String, // Use number as primary key for merging
    val name: String?,
    val type: Int,
    val date: Long,
    val durationSec: Long,
    val photoUri: String? = null
)
