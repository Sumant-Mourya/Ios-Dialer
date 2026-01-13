package com.amigo.dialer.recentcall

import android.content.Context
import android.provider.CallLog
import com.amigo.dialer.recentcall.database.RecentCallEntity
import com.amigo.dialer.recentcall.database.RecentCallsDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RecentCallRepository(context: Context) {
    private val database = RecentCallsDatabase.getDatabase(context)
    private val dao = database.recentCallDao()
    private val resolver = context.contentResolver

    fun getRecents(): Flow<List<RecentCall>> {
        return dao.getAllRecents().map { list ->
            list.map { entity ->
                RecentCall(
                    id = entity.id,
                    name = entity.name,
                    number = entity.number,
                    type = entity.type,
                    date = entity.date,
                    durationSec = entity.durationSec
                )
            }
        }
    }

    suspend fun syncFromDevice() = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        val cursor = resolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )

        val calls = mutableListOf<RecentCallEntity>()
        cursor?.use {
            val idIdx = it.getColumnIndex(CallLog.Calls._ID)
            val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = it.getColumnIndex(CallLog.Calls.DURATION)

            while (it.moveToNext()) {
                val id = if (idIdx != -1) it.getLong(idIdx) else System.nanoTime()
                val name = if (nameIdx != -1) it.getString(nameIdx) else null
                val number = if (numberIdx != -1) it.getString(numberIdx) else null
                val type = if (typeIdx != -1) it.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                val date = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()
                val duration = if (durationIdx != -1) it.getLong(durationIdx) else 0L

                calls.add(
                    RecentCallEntity(
                        id = id,
                        name = name,
                        number = number,
                        type = type,
                        date = date,
                        durationSec = duration
                    )
                )
            }
        }

        dao.clearAll()
        if (calls.isNotEmpty()) {
            dao.insertAll(calls)
        }
    }

    suspend fun getCachedRecents(): List<RecentCall> = withContext(Dispatchers.IO) {
        dao.getAllRecentsOnce().map { entity ->
            RecentCall(
                id = entity.id,
                name = entity.name,
                number = entity.number,
                type = entity.type,
                date = entity.date,
                durationSec = entity.durationSec
            )
        }
    }
}
