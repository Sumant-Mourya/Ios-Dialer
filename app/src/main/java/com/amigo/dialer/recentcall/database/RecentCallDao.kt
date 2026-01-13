package com.amigo.dialer.recentcall.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentCallDao {
    @Query("SELECT * FROM recent_calls ORDER BY date DESC")
    fun getAllRecents(): Flow<List<RecentCallEntity>>

    @Query("SELECT * FROM recent_calls ORDER BY date DESC")
    suspend fun getAllRecentsOnce(): List<RecentCallEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(calls: List<RecentCallEntity>)

    @Query("DELETE FROM recent_calls")
    suspend fun clearAll()
}
