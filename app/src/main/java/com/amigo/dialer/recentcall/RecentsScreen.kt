package com.amigo.dialer.recentcall

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CallLog
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.amigo.dialer.R
import com.amigo.dialer.call.CallActivity
import com.amigo.dialer.call.CallManager
import com.amigo.dialer.recentcall.RecentCallRepository
import kotlinx.coroutines.flow.collectLatest
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import androidx.compose.foundation.layout.navigationBars
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ime

data class RecentCall(
    val name: String?,
    val number: String,
    val type: Int,
    val date: Long,
    val durationSec: Long
)

@Composable
fun RecentsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val repository = remember { RecentCallRepository(context) }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    var hasLogPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCallPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Use collectAsState for automatic UI updates when Room data changes
    val recents by repository.getRecents().collectAsState(initial = emptyList())
    var hasLoadedOnce by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Update hasLoadedOnce when we get data
    LaunchedEffect(recents) {
        android.util.Log.d("RecentsScreen", "Recents updated: ${recents.size} items")
        if (recents.isNotEmpty()) hasLoadedOnce = true
    }

    val filteredRecents by remember(recents, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) recents
            else recents.filter { call ->
                val q = searchQuery.trim().lowercase()
                val nameMatch = call.name?.lowercase()?.contains(q) == true
                val numberMatch = call.number?.lowercase()?.contains(q) == true
                nameMatch || numberMatch
            }
        }
    }

    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLogPermission = granted
        if (granted) {
            scope.launch {
                repository.syncFromDevice()
            }
        } else {
            Toast.makeText(context, "Call log permission required", Toast.LENGTH_SHORT).show()
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCallPermission = granted
        if (!granted) {
            Toast.makeText(context, "Call permission is required to call back", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLogPermission) {
            callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
        } else {
            scope.launch { repository.syncFromDevice() }
        }

        if (!hasCallPermission) {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) {
            focusManager.clearFocus()
        }
    }

    val backdrop = remember {
        object : Backdrop {
            override val isCoordinatesDependent: Boolean = false
            override fun DrawScope.drawBackdrop(
                density: Density,
                coordinates: LayoutCoordinates?,
                layerBlock: (GraphicsLayerScope.() -> Unit)?
            ) {
                // Empty implementation
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
//        Image(
//            painter = painterResource(R.drawable.bg),
//            contentDescription = "Background",
//            modifier = Modifier.fillMaxSize(),
//            contentScale = ContentScale.Crop
//        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            when {
                !hasLogPermission -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Grant call log permission to see recents",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                filteredRecents.isEmpty() && hasLoadedOnce -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "No recent calls" else "No matches",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                else -> {
                    items(filteredRecents, key = { it.number }) { call ->
                        RecentCallItem(
                            call = call,
                            onCallBack = {
                                if (hasCallPermission && !call.number.isNullOrBlank()) {
                                    CallManager.startCall(
                                        context = context,
                                        phoneNumber = call.number,
                                        callerName = call.name ?: call.number
                                    )
                                    val intent = Intent(context, CallActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Grant call permission to call back",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                }
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars)) }
        }
    }
}

@Composable
private fun RecentCallItem(call: RecentCall, onCallBack: () -> Unit) {
    val backdrop = remember {
        object : Backdrop {
            override val isCoordinatesDependent: Boolean = false
            override fun DrawScope.drawBackdrop(
                density: Density,
                coordinates: LayoutCoordinates?,
                layerBlock: (GraphicsLayerScope.() -> Unit)?
            ) {
                // Empty implementation
            }
        }
    }

    val (icon, tint) = when (call.type) {
        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade to Color(0xFF4CAF50)
        CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed to Color(0xFFFF6B6B)
        else -> Icons.AutoMirrored.Filled.CallReceived to Color(0xFF64B5F6)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(20.dp) },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                    lens(16f.dp.toPx(), 32f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.05f))
                }
            )
            .clickable(onClick = onCallBack)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Call type",
            tint = tint,
            modifier = Modifier.size(28.dp)
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = call.name?.takeIf { it.isNotBlank() } ?: (call.number ?: "Unknown"),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = formatCallMeta(call.type, call.durationSec, call.date),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

private fun formatCallMeta(type: Int, duration: Long, date: Long): String {
    val typeLabel = when (type) {
        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
        CallLog.Calls.MISSED_TYPE -> "Missed"
        CallLog.Calls.REJECTED_TYPE -> "Rejected"
        CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"
        CallLog.Calls.BLOCKED_TYPE -> "Blocked"
        CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> "Answered elsewhere"
        else -> "Incoming"
    }
    val mins = duration / 60
    val secs = duration % 60
    val durationLabel = if (duration <= 0) "" else " • ${mins}m ${secs}s"
    val timeLabel = DateUtils.getRelativeTimeSpanString(
        date,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    )
    return "$typeLabel$durationLabel • $timeLabel"
}

// Data class exposed to UI
