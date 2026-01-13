package com.amigo.dialer.recentcall.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_calls")
data class RecentCallEntity(
    @PrimaryKey val id: Long,
    val name: String?,
    val number: String?,
    val type: Int,
    val date: Long,
    val durationSec: Long
)
