package com.amigo.dialer.call

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

class DialerInCallService : InCallService() {
    override fun onCreate() {
        super.onCreate()
        CallManager.attachInCallService(this)
    }
    
    override fun onDestroy() {
        CallManager.detachInCallService(this)
        super.onDestroy()
    }
    
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        
        // Register the call with CallManager
        CallManager.registerCall(call)
        
        // Launch the CallActivity to show the UI
        val intent = Intent(this, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                   Intent.FLAG_ACTIVITY_CLEAR_TOP or
                   Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }
    
    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.unregisterCall()
    }
}
