package com.amigo.dialer.call

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.amigo.dialer.ui.theme.DialerTheme
import kotlinx.coroutines.delay

class CallActivity : ComponentActivity() {
    
    private lateinit var audioManager: AudioManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AudioManager early
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // Show on lock screen and turn screen on
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        
        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Hide status bar
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        setContent {
            DialerTheme {
                val callState by CallManager.callState.collectAsState()
                var callDuration by remember { mutableStateOf(0L) }
                
                // Timer for ongoing calls
                LaunchedEffect(callState) {
                    val shouldTrackTime = callState is CallState.Active || callState is CallState.OnHold
                    if (shouldTrackTime) {
                        val startTime = CallManager.getCallStartTime()
                        while (CallManager.callState.value is CallState.Active || CallManager.callState.value is CallState.OnHold) {
                            callDuration = (System.currentTimeMillis() - startTime) / 1000
                            CallManager.updateCallDuration(callDuration)
                            delay(1000)
                        }
                    }
                }
                
                // Monitor call state to finish activity
                LaunchedEffect(callState) {
                    if (callState is CallState.Ended || callState is CallState.Idle) {
                        delay(1000) // Give user time to see call ended
                        finish()
                    }
                }
                
                when (val state = callState) {
                    is CallState.Incoming -> {
                        IncomingCallScreen(
                            callerName = state.callerName,
                            callerNumber = state.phoneNumber,
                            onAccept = {
                                CallManager.acceptCall()
                            },
                            onDecline = {
                                CallManager.declineCall()
                                finish()
                            }
                        )
                    }
                    is CallState.Dialing, 
                    is CallState.Connecting,
                    is CallState.Active,
                    is CallState.OnHold -> {
                        OngoingCallScreen(
                            callerName = when (state) {
                                is CallState.Dialing -> state.callerName
                                is CallState.Connecting -> state.callerName
                                is CallState.Active -> state.callerName
                                is CallState.OnHold -> state.callerName
                                else -> "Unknown"
                            },
                            callerNumber = when (state) {
                                is CallState.Dialing -> state.phoneNumber
                                is CallState.Connecting -> state.phoneNumber
                                is CallState.Active -> state.phoneNumber
                                is CallState.OnHold -> state.phoneNumber
                                else -> ""
                            },
                            callDuration = callDuration,
                            isConnected = state is CallState.Active || state is CallState.OnHold,
                            isOnHold = state is CallState.OnHold,
                            onEndCall = {
                                CallManager.endCall()
                            },
                            onToggleMute = { isMuted ->
                                CallManager.toggleMute(this@CallActivity, isMuted)
                            },
                            onToggleSpeaker = { isSpeakerOn ->
                                CallManager.toggleSpeaker(this@CallActivity, isSpeakerOn)
                            },
                            onToggleHold = { hold ->
                                CallManager.toggleHold(hold)
                            }
                        )
                    }
                    else -> {
                        // Finish activity if no active call
                        LaunchedEffect(Unit) {
                            finish()
                        }
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up audio settings
        try {
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            android.util.Log.e("CallActivity", "Error resetting audio", e)
        }
        CallManager.unregisterCall()
    }
}
