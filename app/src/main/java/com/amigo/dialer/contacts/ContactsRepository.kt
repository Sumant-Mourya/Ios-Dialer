package com.amigo.dialer.contacts

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.amigo.dialer.contacts.database.ContactEntity
import com.amigo.dialer.contacts.database.ContactsDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactsRepository(private val context: Context) {

    private val database = ContactsDatabase.getDatabase(context)
    private val contactDao = database.contactDao()

    /**
     * Get contacts with paging for efficient loading
     */
    fun getContactsPaged(): Flow<PagingData<Contact>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 10
            ),
            pagingSourceFactory = { contactDao.getAllContactsPaged() }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toContact(context.contentResolver) }
        }
    }

    /**
     * Get favorite contacts with paging
     */
    fun getFavoriteContactsPaged(): Flow<PagingData<Contact>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 10
            ),
            pagingSourceFactory = { contactDao.getFavoriteContactsPaged() }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toContact(context.contentResolver) }
        }
    }

    /**
     * Search contacts with paging
     */
    fun searchContactsPaged(query: String): Flow<PagingData<Contact>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 10
            ),
            pagingSourceFactory = { contactDao.searchContactsPaged(query) }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toContact(context.contentResolver) }
        }
    }

    /**
     * Get contacts from Room database as Flow for reactive updates
     */
    fun getContactsFlow(): Flow<List<Contact>> {
        return contactDao.getAllContacts().map { entities ->
            entities.map { entity -> entity.toContact(context.contentResolver) }
        }
    }

    /**
     * Favorite contacts as Flow
     */
    fun getFavoriteContactsFlow(): Flow<List<Contact>> {
        return contactDao.getFavoriteContacts().map { entities ->
            entities.map { entity -> entity.toContact(context.contentResolver) }
        }
    }

    /**
     * Sync contacts from device to Room database
     */
    suspend fun syncContacts() = withContext(Dispatchers.IO) {
        try {
            val deviceContacts = fetchContactsFromDevice()
            android.util.Log.d("ContactsRepo", "Fetched ${deviceContacts.size} contacts from device")
            
            if (deviceContacts.isNotEmpty()) {
                val entities = deviceContacts.map { contact ->
                    ContactEntity(
                        id = contact.id,
                        name = contact.name,
                        phoneNumber = contact.phoneNumber,
                        photoUri = contact.photoUri,
                        isFavorite = contact.isFavorite
                    )
                }
                
                contactDao.deleteAll()
                contactDao.insertAll(entities)
                
                val dbCount = contactDao.getContactsCount()
                android.util.Log.d("ContactsRepo", "Inserted into DB. Total count: $dbCount")
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error syncing contacts", e)
            throw e
        }
    }

    /**
     * Check if database has contacts
     */
    suspend fun hasContacts(): Boolean = withContext(Dispatchers.IO) {
        contactDao.getContactsCount() > 0
    }

    /**
     * Fetch contacts from device content provider
     */
    private fun fetchContactsFromDevice(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val contentResolver: ContentResolver = context.contentResolver

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.STARRED
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            val starredIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val name = it.getString(nameIndex) ?: "Unknown"
                val number = it.getString(numberIndex) ?: ""
                val photoUri = it.getString(photoIndex)
                val isFavorite = starredIndex != -1 && it.getInt(starredIndex) == 1

                contacts.add(
                    Contact(
                        id = id,
                        name = name,
                        phoneNumber = number,
                        photoUri = photoUri,
                        isFavorite = isFavorite
                    )
                )
            }
        }

        return contacts
    }

    /**
     * Extension function to convert ContactEntity to Contact with bitmap
     */
    private fun ContactEntity.toContact(contentResolver: ContentResolver): Contact {
        val photoBitmap = photoUri?.let { uri ->
            try {
                val inputStream = contentResolver.openInputStream(uri.toUri())
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                null
            }
        }

        return Contact(
            id = id,
            name = name,
            phoneNumber = phoneNumber,
            photoUri = photoUri,
            photoBitmap = photoBitmap,
            isFavorite = isFavorite
        )
    }

    // Keep old method for backward compatibility
    suspend fun getContacts(): List<Contact> = withContext(Dispatchers.IO) {
        fetchContactsFromDevice()
    }
}
