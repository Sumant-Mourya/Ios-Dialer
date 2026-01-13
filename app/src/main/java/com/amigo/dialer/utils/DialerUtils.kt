package com.amigo.dialer.utils

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher

object DialerUtils {
    
    /**
     * Check if this app is the default dialer
     */
    fun isDefaultDialer(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            context.packageName == telecomManager.defaultDialerPackage
        } else {
            true // Assume true for older versions
        }
    }
    
    /**
     * Request to set this app as default dialer
     */
    fun requestDefaultDialer(
        activity: ComponentActivity,
        launcher: ActivityResultLauncher<Intent>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isDefaultDialer(activity)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ - Use RoleManager
                    val roleManager = activity.getSystemService(Context.ROLE_SERVICE) as RoleManager
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                    launcher.launch(intent)
                } else {
                    // Android 6-9 - Use TelecomManager
                    val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, activity.packageName)
                    launcher.launch(intent)
                }
            }
        }
    }
}
