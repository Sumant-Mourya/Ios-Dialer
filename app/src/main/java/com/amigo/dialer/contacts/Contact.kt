package com.amigo.dialer.contacts

import android.graphics.Bitmap

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null,
    val photoBitmap: Bitmap? = null
)
