package com.amigo.dialer.recentcall

import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
import android.net.Uri
import com.amigo.dialer.recentcall.database.RecentCallEntity
import com.amigo.dialer.recentcall.database.RecentCallsDatabase
import com.amigo.dialer.contacts.PhoneNumberNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RecentCallRepository(context: Context) {
    private val database = RecentCallsDatabase.getDatabase(context)
    private val dao = database.recentCallDao()
    private val resolver = context.contentResolver
    private val syncMutex = Mutex() // Prevent concurrent syncs

    fun getRecents(): Flow<List<RecentCall>> {
        return dao.getAllRecents().map { list ->
            list.map { entity ->
                RecentCall(
                    name = entity.name,
                    number = entity.number,
                    type = entity.type,
                    date = entity.date,
                    durationSec = entity.durationSec,
                    photoUri = entity.photoUri
                )
            }
        }
    }

    suspend fun syncFromDevice() = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val startTime = System.currentTimeMillis()
            android.util.Log.d("RecentCallRepo", "[${System.currentTimeMillis()}] Starting sync from device...")
            
            // Build contact names map once for efficient lookup
            val contactNamesMap = getContactNamesAndPhotoMap()
            
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
                val number = if (numberIdx != -1) it.getString(numberIdx) else null
                if (number.isNullOrBlank()) continue // Skip entries without number
                
                val type = if (typeIdx != -1) it.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                val date = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()
                val duration = if (durationIdx != -1) it.getLong(durationIdx) else 0L

                val normalized = PhoneNumberNormalizer.normalize(number)
                val key = if (normalized.isNotBlank()) normalized else number
                
                // Resolve current contact name and photo from map
                val contactInfo = contactNamesMap[key]
                val currentName = contactInfo?.first
                val photoUri = contactInfo?.second

                calls.add(
                    RecentCallEntity(
                        number = key,
                        name = currentName,
                        type = type,
                        date = date,
                        durationSec = duration,
                        photoUri = photoUri
                    )
                )
            }
        }

            val merged = mergeCalls(calls)
            android.util.Log.d("RecentCallRepo", "Merged ${calls.size} calls into ${merged.size} unique entries")
            if (merged.isNotEmpty()) {
                dao.insertAll(merged)
                android.util.Log.d("RecentCallRepo", "Inserted ${merged.size} entries into database")
            }
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("RecentCallRepo", "[${System.currentTimeMillis()}] Sync completed in ${duration}ms, merged ${merged.size} calls from ${calls.size} entries")
        }
    }

    suspend fun getCachedRecents(): List<RecentCall> = withContext(Dispatchers.IO) {
        dao.getAllRecentsOnce().map { entity ->
            RecentCall(
                name = entity.name,
                number = entity.number,
                type = entity.type,
                date = entity.date,
                durationSec = entity.durationSec,
                photoUri = entity.photoUri
            )
        }
    }

    private fun getContactNamesAndPhotoMap(): Map<String, Pair<String, String?>> {
        val contactMap = mutableMapOf<String, Pair<String, String?>>()
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            null,
            null,
            null
        )

        cursor?.use {
            val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            while (it.moveToNext()) {
                val number = if (numberIdx != -1) it.getString(numberIdx) else null
                val name = if (nameIdx != -1) it.getString(nameIdx) else null
                val photoUri = if (photoIdx != -1) it.getString(photoIdx) else null
                
                if (!number.isNullOrBlank() && !name.isNullOrBlank()) {
                    val normalized = PhoneNumberNormalizer.normalize(number)
                    val key = if (normalized.isNotBlank()) normalized else number
                    contactMap[key] = Pair(name, photoUri)
                }
            }
        }

        android.util.Log.d("RecentCallRepo", "Built contact map with ${contactMap.size} entries")
        return contactMap
    }

    private fun mergeCalls(calls: List<RecentCallEntity>): List<RecentCallEntity> {
        val byNumber = linkedMapOf<String, RecentCallEntity>()

        calls.forEach { call ->
            val existing = byNumber[call.number]

            if (existing == null || call.date > existing.date) {
                // Keep the most recent call, prefer named contacts
                val name = call.name ?: existing?.name
                val photoUri = call.photoUri ?: existing?.photoUri
                byNumber[call.number] = call.copy(name = name, photoUri = photoUri)
            } else if (existing.name.isNullOrBlank() && !call.name.isNullOrBlank()) {
                // Update with name if existing has no name
                val photoUri = call.photoUri ?: existing.photoUri
                byNumber[call.number] = existing.copy(name = call.name, photoUri = photoUri)
            } else if (existing.photoUri.isNullOrBlank() && !call.photoUri.isNullOrBlank()) {
                // Update with photoUri if existing has no photoUri
                byNumber[call.number] = existing.copy(photoUri = call.photoUri)
            }
        }

        return byNumber.values.sortedByDescending { it.date }
    }
}
