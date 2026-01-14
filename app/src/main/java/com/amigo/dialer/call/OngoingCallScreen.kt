package com.amigo.dialer.call

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.amigo.dialer.R
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun OngoingCallScreen(
    callerName: String = "Unknown Caller",
    callerNumber: String = "+1 234 567 8900",
    callDuration: Long = 0L,
    isConnected: Boolean = true,
    isOnHold: Boolean = false,
    wasIncomingAnswered: Boolean = false,
    onEndCall: () -> Unit = {},
    onToggleMute: (Boolean) -> Unit = {},
    onToggleSpeaker: (Boolean) -> Unit = {},
    onToggleHold: (Boolean) -> Unit = {}
) {
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isKeypadOpened by remember { mutableStateOf(false) }
    var isOnHoldState by remember(isOnHold) { mutableStateOf(isOnHold) }
    var isVideoOn by remember { mutableStateOf(false) }
    var isAddContactActive by remember { mutableStateOf(false) }
    var isMessageActive by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    val backdrop = object : Backdrop {
        override val isCoordinatesDependent: Boolean = false
        override fun DrawScope.drawBackdrop(
            density: Density,
            coordinates: LayoutCoordinates?,
            layerBlock: (androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit)?
        ) {
            // Empty implementation
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
//        // Background Image
//        Image(
//            painter = painterResource(R.drawable.bg),
//            contentDescription = "Background",
//            modifier = Modifier.fillMaxSize(),
//            contentScale = ContentScale.Crop
//        )

        VideoBackground(
            modifier = Modifier.fillMaxSize(),
            isConnected = isConnected,
            shouldStartPaused = wasIncomingAnswered
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Caller Info Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Profile picture
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            effects = {
                                vibrancy()
                                blur(12f.dp.toPx())
                                lens(20f.dp.toPx(), 40f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawCircle(Color.White.copy(alpha = 0.15f))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Caller",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(60.dp)
                    )
                }

                val hasName = callerName.isNotBlank() &&
                        !callerName.equals("Unknown", ignoreCase = true) &&
                        !callerName.equals("Unknown Caller", ignoreCase = true)

                // Show name if available, otherwise show the number
                val displayText = if (hasName) callerName else callerNumber

                Text(
                    text = displayText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Call Status / Duration with pill-shaped glass background
                val backdrop = object : Backdrop {
                    override val isCoordinatesDependent: Boolean = false
                    override fun DrawScope.drawBackdrop(
                        density: Density,
                        coordinates: LayoutCoordinates?,
                        layerBlock: (GraphicsLayerScope.() -> Unit)?
                    ) {
                        // Empty implementation
                    }
                }

                Box(
                    modifier = Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule() },
                            effects = {
                                vibrancy()
                                blur(10f.dp.toPx())
                                lens(12f.dp.toPx(), 24f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 0.12f))
                            }
                        )
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    if (!isConnected) {
                        Text(
                            text = "Connecting",
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        val status = if (isOnHold) "Hold" else "Active"
                        val annotated = buildAnnotatedString {
                            append("Connected | ")
                            withStyle(SpanStyle(color = Color(0xFF34C759))) {
                                append(formatDuration(callDuration))
                            }
                            append(" | $status")
                        }

                        Text(
                            text = annotated,
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Control Buttons Section
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dismissible overlay when expanded
                if (isExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                isExpanded = false
                            }
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glass Card with Control Buttons
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { com.kyant.capsule.ContinuousRoundedRectangle(24.dp) },
                                effects = {
                                    vibrancy()
                                    blur(12f.dp.toPx())
                                    lens(20f.dp.toPx(), 40f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.08f))
                                }
                            )
                            .padding(24.dp)
                    ) {
                        AnimatedContent(
                            targetState = isExpanded,
                            transitionSpec = {
                                if (targetState) {
                                    // Expanding
                                    (fadeIn(animationSpec = tween(500)) +
                                            expandVertically(animationSpec = tween(500)))
                                        .togetherWith(
                                            fadeOut(animationSpec = tween(300)) +
                                                    shrinkVertically(animationSpec = tween(300))
                                        )
                                } else {
                                    // Collapsing
                                    (fadeIn(animationSpec = tween(300)) +
                                            expandVertically(animationSpec = tween(300)))
                                        .togetherWith(
                                            fadeOut(animationSpec = tween(150)) +
                                                    shrinkVertically(animationSpec = tween(150))
                                        )
                                }
                            },
                            label = "expand_collapse"
                        ) { expanded ->
                            if (!expanded) {
                                // Collapsed view - Show only 4 buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    CallControlButton(
                                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                                        label = "Speaker",
                                        isActive = isSpeakerOn,
                                        onClick = {
                                            isSpeakerOn = !isSpeakerOn
                                            onToggleSpeaker(isSpeakerOn)
                                        }
                                    )

                                    CallControlButton(
                                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        label = "Mute",
                                        isActive = isMuted,
                                        onClick = {
                                            isMuted = !isMuted
                                            onToggleMute(isMuted)
                                        }
                                    )

                                    CallControlButton(
                                        icon = Icons.Default.Dialpad,
                                        label = "Dialpad",
                                        isActive = isKeypadOpened,
                                        onClick = { isKeypadOpened = !isKeypadOpened }
                                    )

                                    CallControlButton(
                                        icon = Icons.Default.MoreVert,
                                        label = "More",
                                        isActive = false,
                                        onClick = { isExpanded = true }
                                    )
                                }
                            } else {
                                // Expanded view - Show all 9 buttons in 3x3 grid
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Row 1
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        CallControlButton(
                                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                                            label = "Speaker",
                                            isActive = isSpeakerOn,
                                            onClick = {
                                                isSpeakerOn = !isSpeakerOn
                                                onToggleSpeaker(isSpeakerOn)
                                            }
                                        )

                                        CallControlButton(
                                            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                            label = "Mute",
                                            isActive = isMuted,
                                            onClick = {
                                                isMuted = !isMuted
                                                onToggleMute(isMuted)
                                            }
                                        )

                                        CallControlButton(
                                            icon = Icons.Default.Dialpad,
                                            label = "Dialpad",
                                            isActive = isKeypadOpened,
                                            onClick = { isKeypadOpened = !isKeypadOpened }
                                        )
                                    }

                                    // Row 2
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        CallControlButton(
                                            icon = Icons.Default.Pause,
                                            label = "Hold",
                                            isActive = isOnHoldState,
                                            onClick = {
                                                isOnHoldState = !isOnHoldState
                                                onToggleHold(isOnHoldState)
                                            }
                                        )

                                        CallControlButton(
                                            icon = if (isVideoOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                            label = "Video",
                                            isActive = isVideoOn,
                                            onClick = { isVideoOn = !isVideoOn }
                                        )

                                        CallControlButton(
                                            icon = Icons.Default.PersonAdd,
                                            label = "Add Call",
                                            isActive = isAddContactActive,
                                            onClick = { isAddContactActive = !isAddContactActive }
                                        )
                                    }

                                    // Row 3
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        CallControlButton(
                                            icon = Icons.AutoMirrored.Filled.Message,
                                            label = "Message",
                                            isActive = isMessageActive,
                                            onClick = { isMessageActive = !isMessageActive }
                                        )

                                        CallControlButton(
                                            icon = Icons.Default.FiberManualRecord,
                                            label = "Record",
                                            isActive = isRecording,
                                            onClick = { isRecording = !isRecording }
                                        )

                                        CallControlButton(
                                            icon = Icons.Default.SwapCalls,
                                            label = "Swap",
                                            isActive = false,
                                            onClick = { /* TODO: Swap calls */ }
                                        )
                                    }

                                    // Close button
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp, 4.dp)
                                                .background(
                                                    Color.White.copy(alpha = 0.3f),
                                                    shape = ContinuousCapsule()
                                                )
                                                .clickable { isExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // End Call Button (Large, centered)
                    LiquidCallButton(
                        icon = Icons.Default.CallEnd,
                        backgroundColor = Color(0xFFFF3B30),
                        onClick = onEndCall,
                        modifier = Modifier.size(width = 120.dp, height = 70.dp),
                        shape = ContinuousCapsule
                    )
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val backdrop = object : Backdrop {
        override val isCoordinatesDependent: Boolean = false
        override fun DrawScope.drawBackdrop(
            density: Density,
            coordinates: LayoutCoordinates?,
            layerBlock: (GraphicsLayerScope.() -> Unit)?
        ) {
            // Empty implementation
        }
    }

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)

            // 🔹 BACKGROUND must be reactive
            .background(
                if (isActive)
                    Color(0xFF2196F3).copy(alpha = 0.85f)
                else
                    Color.White.copy(alpha = 0.12f)
            )

            // 🔹 Blur / glass effect
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(16f.dp.toPx(), 32f.dp.toPx())
                }
            )

            // 🔹 Clickable LAST
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color(0xFF2196F3)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = TimeUnit.SECONDS.toMinutes(seconds)
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}


@Composable
fun VideoBackground(
    modifier: Modifier = Modifier,
    isConnected: Boolean = false,
    shouldStartPaused: Boolean = false
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.bg_video}")
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = !shouldStartPaused
            volume = 0f
            prepare()
        }
    }

    // Control video playback based on connection state
    LaunchedEffect(isConnected) {
        if (isConnected) {
            exoPlayer.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setBackgroundColor(android.graphics.Color.BLACK)
                    keepScreenOn = true
                }
            }
        )
        
        // Apply blur overlay when connected
        if (isConnected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .blur(25.dp)
            )
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun PreviewFunction() {
    OngoingCallScreen()
}