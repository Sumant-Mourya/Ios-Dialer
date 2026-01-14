package com.amigo.dialer.dialpad

import android.Manifest
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.KeyboardBackspace
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amigo.dialer.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.input.pointer.pointerInput
import com.amigo.dialer.call.CallActivity
import com.amigo.dialer.call.CallManager
import com.amigo.dialer.call.LiquidCallButton
import com.amigo.dialer.prefs.LastCallPrefs
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun DialPadScreen(
    initialNumber: String = "",
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf(initialNumber) }
    var pendingNumber by remember { mutableStateOf<String?>(null) }
    val lastCallPrefs = remember { LastCallPrefs(context) }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.CALL_PHONE] == true &&
            permissions[Manifest.permission.READ_PHONE_STATE] == true
        if (granted) {
            val number = pendingNumber ?: phoneNumber
            if (number.isNotBlank()) {
                placeCall(context = context, number = number)
                lastCallPrefs.saveLastDialed(number)
                phoneNumber = ""
            } else {
                Toast.makeText(context, "Enter a number", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Call permission is required", Toast.LENGTH_SHORT).show()
        }
        pendingNumber = null
    }

    val backdrop = remember {
        object : Backdrop {
            override val isCoordinatesDependent: Boolean = false
            override fun DrawScope.drawBackdrop(
                density: Density,
                coordinates: LayoutCoordinates?,
                layerBlock: (GraphicsLayerScope.() -> Unit)?
            ) {
                // Intentionally empty; effects handled in drawBackdrop
            }
        }
    }

    val keys = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* Consume clicks on empty areas */ }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.systemBars))
            Spacer(modifier = Modifier.height(15.dp))

            Spacer(modifier = Modifier.weight(0.2f))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Number pill
                val displayNumber = phoneNumber.ifBlank { "Enter number" }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousRoundedRectangle(24.dp) },
                            effects = {
                                vibrancy()
                                blur(12f.dp.toPx())
                                lens(20f.dp.toPx(), 40f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 0.08f))
                            }
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .defaultMinSize(minHeight = 35.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayNumber,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (phoneNumber.isBlank()) Color.White.copy(alpha = 0.5f) else Color.White,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )

                        if (phoneNumber.isNotEmpty()) {
                            Icon(
                                painterResource(R.drawable.backspace),
                                contentDescription = "Backspace",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(32.dp)
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(color = Color(0xFF2196F3), bounded = true),
                                        onClick = {
                                            phoneNumber = phoneNumber.dropLast(1)
                                        },
                                        onLongClick = {
                                            phoneNumber = ""
                                        }
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(vertical = 6.dp))

                // Keypad
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            row.forEach { (digit, hint) ->
                                DialKey(
                                    label = digit,
                                    subLabel = hint,
                                    onClick = {
                                        phoneNumber += digit
                                    },
                                    onLongClick = if (digit == "0") {
                                        { phoneNumber += "+" }
                                    } else {
                                        null
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(vertical = 10.dp))

                // Call button row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidCallButton(
                        icon = Icons.Default.Call,
                        backgroundColor = Color(0xFF34C759),
                        onClick = {
                            if (phoneNumber.isBlank()) {
                                val last = lastCallPrefs.getLastDialed()
                                if (last.isNotBlank()) {
                                    phoneNumber = last
                                } else {
                                    Toast.makeText(context, "Enter a number", Toast.LENGTH_SHORT).show()
                                }
                                return@LiquidCallButton
                            }

                            pendingNumber = phoneNumber
                            callPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CALL_PHONE,
                                    Manifest.permission.READ_PHONE_STATE
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.4f) // Use 40% of screen width
                            .height(70.dp),
                        shape = ContinuousCapsule
                    )
                }
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Spacer(modifier = Modifier.height(92.dp))
            }
        }
    }
}

@Composable
private fun DialKey(
    label: String,
    subLabel: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val backdrop = remember {
        object : Backdrop {
            override val isCoordinatesDependent: Boolean = false
            override fun DrawScope.drawBackdrop(
                density: Density,
                coordinates: LayoutCoordinates?,
                layerBlock: (GraphicsLayerScope.() -> Unit)?
            ) {
                // Intentionally empty
            }
        }
    }
    val shape = RoundedCornerShape(30.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)

            // ✅ backdrop first with correct shape
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(18f.dp.toPx(), 36f.dp.toPx())
                },
                onDrawSurface = {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.08f),
                        cornerRadius = CornerRadius(20.dp.toPx())
                    )
                }
            )

            // ✅ clip AFTER backdrop so ripple is also clipped properly
            .clip(shape)

            // ✅ clickable after clip so ripple uses rounded shape
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    bounded = true,
                    color = Color(0xFF2196F3),
                    radius = 60.dp
                ),
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Text(
            text = label,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        if (subLabel.isNotEmpty()) {
            Text(
                text = subLabel,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

private fun placeCall(context: Context, number: String) {
    if (number.isBlank()) return
    CallManager.startCall(context, number, callerName = number)
    val intent = Intent(context, CallActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    context.startActivity(intent)
}


@Preview
@Composable
private fun PreviewFunction() {
    DialPadScreen()
}