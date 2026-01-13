package com.amigo.dialer.prefs

import android.content.Context

class LastCallPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("last_call_prefs", Context.MODE_PRIVATE)

    fun saveLastDialed(number: String) {
        prefs.edit().putString(KEY_LAST_NUMBER, number).apply()
    }

    fun getLastDialed(): String = prefs.getString(KEY_LAST_NUMBER, "") ?: ""

    companion object {
        private const val KEY_LAST_NUMBER = "last_number"
    }
}
