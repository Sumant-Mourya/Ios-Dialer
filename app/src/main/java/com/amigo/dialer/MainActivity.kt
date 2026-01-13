package com.amigo.dialer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material3.Scaffold
import com.amigo.dialer.contacts.ContactsScreen
import com.amigo.dialer.defaultdialer.AboutAppScreen
import com.amigo.dialer.defaultdialer.DefaultDialerHandler
import com.amigo.dialer.ui.theme.DialerTheme
import com.amigo.dialer.utils.DialerUtils

class MainActivity : ComponentActivity() {

    private var wasDefaultDialer = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check initial status
        wasDefaultDialer = DialerUtils.isDefaultDialer(this)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Hide status bar (NON-DEPRECATED)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            DialerTheme {
                var isDefaultDialer by remember { mutableStateOf(DialerUtils.isDefaultDialer(this)) }
                val lifecycleOwner = LocalLifecycleOwner.current

                // Observe lifecycle to check default dialer status on resume
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val currentStatus = DialerUtils.isDefaultDialer(this@MainActivity)
                            if (currentStatus != isDefaultDialer) {
                                isDefaultDialer = currentStatus
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    if (isDefaultDialer) {
                        ContactsScreen()
                    } else {
                        DefaultDialerHandler(
                            activity = this@MainActivity,
                            onStatusChanged = { newStatus ->
                                isDefaultDialer = newStatus
                            }
                        ) { onSetDefaultClick ->
                            AboutAppScreen(onSetDefaultClick = onSetDefaultClick)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Check if default dialer status changed
        val isCurrentlyDefault = DialerUtils.isDefaultDialer(this)
        
        if (wasDefaultDialer && !isCurrentlyDefault) {
            // App was removed as default dialer
            Toast.makeText(
                this,
                "RecLine is no longer your default dialer",
                Toast.LENGTH_LONG
            ).show()
        }
        
        wasDefaultDialer = isCurrentlyDefault
    }
}


