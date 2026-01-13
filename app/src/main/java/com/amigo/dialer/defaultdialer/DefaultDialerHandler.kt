package com.amigo.dialer.defaultdialer

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.amigo.dialer.utils.DialerUtils

@Composable
fun DefaultDialerHandler(
    activity: ComponentActivity,
    onStatusChanged: (Boolean) -> Unit = {},
    content: @Composable (onSetDefaultClick: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var isDefaultDialer by remember { mutableStateOf(DialerUtils.isDefaultDialer(context)) }
    
    // Launcher for default dialer request
    val defaultDialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val isNowDefault = DialerUtils.isDefaultDialer(context)
        isDefaultDialer = isNowDefault
        onStatusChanged(isNowDefault)
        
        if (isNowDefault) {
            Toast.makeText(
                context,
                "RecLine is now your default dialer",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "Default dialer not set",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // Check default dialer status when activity resumes
    LaunchedEffect(Unit) {
        val currentStatus = DialerUtils.isDefaultDialer(context)
        isDefaultDialer = currentStatus
        onStatusChanged(currentStatus)
    }
    
    content {
        if (!isDefaultDialer) {
            DialerUtils.requestDefaultDialer(
                activity = activity,
                launcher = defaultDialerLauncher
            )
        } else {
            Toast.makeText(
                context,
                "Already set as default dialer",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
