package com.amigo.dialer.call

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

object CallManager {
    
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()
    
    private val _callDuration = MutableStateFlow(0L)
    val callDuration: StateFlow<Long> = _callDuration.asStateFlow()
    
    private var currentCall: Call? = null
    private var callStartTime: Long = 0
    private var audioManager: AudioManager? = null
    private var inCallService: WeakReference<android.telecom.InCallService>? = null
    
    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            handleCallStateChange(call, state)
        }
    }
    
    fun startCall(context: Context, phoneNumber: String, callerName: String = "Unknown") {
        try {
            // Initialize AudioManager if not already done
            if (audioManager == null) {
                audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            }
            
            val telecomManager = ContextCompat.getSystemService(context, TelecomManager::class.java)
            
            if (telecomManager != null) {
                val uri = Uri.fromParts("tel", phoneNumber, null)
                telecomManager.placeCall(uri, null)
                
                _callState.value = CallState.Dialing(
                    phoneNumber = phoneNumber,
                    callerName = callerName
                )
            } else {
                // Fallback to regular call intent
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                
                _callState.value = CallState.Dialing(
                    phoneNumber = phoneNumber,
                    callerName = callerName
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _callState.value = CallState.Error(e.message ?: "Failed to start call")
        }
    }

    fun attachInCallService(service: android.telecom.InCallService) {
        inCallService = WeakReference(service)
        if (audioManager == null) {
            audioManager = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
    }

    fun detachInCallService(service: android.telecom.InCallService) {
        if (inCallService?.get() == service) {
            inCallService = null
        }
    }
    
    fun registerCall(call: Call) {
        currentCall = call
        call.registerCallback(callback)
        handleCallStateChange(call, call.state)
    }
    
    fun unregisterCall() {
        currentCall?.unregisterCallback(callback)
        currentCall = null
        // Clean up audio when call is removed
        resetAudioMode()
    }
    
    private fun handleCallStateChange(call: Call, state: Int) {
        when (state) {
            Call.STATE_DIALING -> {
                val details = call.details
                val phoneNumber = details.handle?.schemeSpecificPart ?: ""
                _callState.value = CallState.Dialing(
                    phoneNumber = phoneNumber,
                    callerName = details.contactDisplayName ?: "Unknown"
                )
                // Set audio mode for call
                setupAudioForCall()
            }
            Call.STATE_RINGING -> {
                val details = call.details
                val phoneNumber = details.handle?.schemeSpecificPart ?: ""
                _callState.value = CallState.Incoming(
                    phoneNumber = phoneNumber,
                    callerName = details.contactDisplayName ?: "Unknown Caller"
                )
            }
            Call.STATE_ACTIVE -> {
                val details = call.details
                val phoneNumber = details.handle?.schemeSpecificPart ?: ""
                callStartTime = System.currentTimeMillis()
                _callState.value = CallState.Active(
                    phoneNumber = phoneNumber,
                    callerName = details.contactDisplayName ?: "Unknown"
                )
                // Ensure audio mode is set for active call
                setupAudioForCall()
            }
            Call.STATE_DISCONNECTED -> {
                _callState.value = CallState.Ended
                resetCallState()
            }
            Call.STATE_CONNECTING -> {
                val details = call.details
                val phoneNumber = details.handle?.schemeSpecificPart ?: ""
                _callState.value = CallState.Connecting(
                    phoneNumber = phoneNumber,
                    callerName = details.contactDisplayName ?: "Unknown"
                )
                // Set audio mode for call
                setupAudioForCall()
            }
            Call.STATE_HOLDING -> {
                val details = call.details
                val phoneNumber = details.handle?.schemeSpecificPart ?: ""
                _callState.value = CallState.OnHold(
                    phoneNumber = phoneNumber,
                    callerName = details.contactDisplayName ?: "Unknown"
                )
            }
        }
    }
    
    fun acceptCall() {
        currentCall?.answer(0)
    }
    
    fun declineCall() {
        currentCall?.disconnect()
    }
    
    fun endCall() {
        currentCall?.disconnect()
    }
    
    fun toggleMute(context: Context, isMuted: Boolean) {
        // Prefer InCallService API when available because the system manages audio focus
        inCallService?.get()?.setMuted(isMuted) ?: run {
            if (audioManager == null) {
                audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            }
            audioManager?.isMicrophoneMute = isMuted
        }
    }

    fun toggleSpeaker(context: Context, isSpeakerOn: Boolean) {
        // Prefer the telecom-managed audio route; fall back to AudioManager if needed
        val routed = inCallService?.get()?.let { service ->
            try {
                service.setAudioRoute(
                    if (isSpeakerOn) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
                )
                true
            } catch (e: Exception) {
                android.util.Log.e("CallManager", "Error setting audio route via InCallService", e)
                false
            }
        } ?: false

        if (!routed) {
            if (audioManager == null) {
                audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            }
            audioManager?.let { audio ->
                try {
                    if (audio.mode != AudioManager.MODE_IN_CALL &&
                        audio.mode != AudioManager.MODE_IN_COMMUNICATION) {
                        audio.mode = AudioManager.MODE_IN_CALL
                    }
                    audio.isSpeakerphoneOn = isSpeakerOn
                    android.util.Log.d(
                        "CallManager",
                        "Speaker toggled via AudioManager: $isSpeakerOn, state: ${audio.isSpeakerphoneOn}, mode: ${audio.mode}"
                    )
                } catch (e: Exception) {
                    android.util.Log.e("CallManager", "Error toggling speaker via AudioManager", e)
                }
            }
        }
    }

    fun toggleHold(hold: Boolean) {
        currentCall?.let { call ->
            try {
                val canHold = (call.details.callCapabilities and Call.Details.CAPABILITY_HOLD) != 0
                if (!canHold) {
                    android.util.Log.w("CallManager", "Call does not support hold")
                    return
                }
                if (hold && call.state != Call.STATE_HOLDING) {
                    call.hold()
                } else if (!hold && call.state == Call.STATE_HOLDING) {
                    call.unhold()
                }
            } catch (e: Exception) {
                android.util.Log.e("CallManager", "Error toggling hold", e)
            }
        }
    }
    
    private fun setupAudioForCall() {
        audioManager?.let { audio ->
            try {
                if (audio.mode != AudioManager.MODE_IN_CALL) {
                    audio.mode = AudioManager.MODE_IN_CALL
                }
                android.util.Log.d("CallManager", "Audio mode set for call: ${audio.mode}")
            } catch (e: Exception) {
                android.util.Log.e("CallManager", "Error setting audio mode", e)
            }
        }
    }
    
    private fun resetAudioMode() {
        audioManager?.let { audio ->
            try {
                audio.isSpeakerphoneOn = false
                audio.mode = AudioManager.MODE_NORMAL
                android.util.Log.d("CallManager", "Audio mode reset to normal")
            } catch (e: Exception) {
                android.util.Log.e("CallManager", "Error resetting audio mode", e)
            }
        }
    }

    fun updateCallDuration(duration: Long) {
        _callDuration.value = duration
    }

    private fun resetCallState() {
        callStartTime = 0
        _callDuration.value = 0
        // Reset audio settings
        resetAudioMode()
    }

    fun getCallStartTime(): Long = callStartTime
}

sealed class CallState {
    object Idle : CallState()
    data class Dialing(val phoneNumber: String, val callerName: String) : CallState()
    data class Connecting(val phoneNumber: String, val callerName: String) : CallState()
    data class Incoming(val phoneNumber: String, val callerName: String) : CallState()
    data class Active(val phoneNumber: String, val callerName: String) : CallState()
    data class OnHold(val phoneNumber: String, val callerName: String) : CallState()
    object Ended : CallState()
    data class Error(val message: String) : CallState()
}
