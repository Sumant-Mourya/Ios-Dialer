package com.amigo.dialer.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.amigo.dialer.R
import com.amigo.dialer.contacts.ContactsRepository
import com.amigo.dialer.contacts.ContactsScreen
import com.amigo.dialer.contacts.favourite.FavoritesScreen
import com.amigo.dialer.dialpad.DialPadScreen
import com.amigo.dialer.recentcall.RecentsScreen
import com.amigo.dialer.recentcall.RecentCallRepository
import com.amigo.dialer.call.CallManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

sealed class BottomTab(val icon: ImageVector, val label: String) {
    data object Dialer : BottomTab(Icons.Filled.Dialpad, "Dialer")
    data object Favorites : BottomTab(Icons.Filled.Favorite, "Favorites")
    data object Recents : BottomTab(Icons.Filled.History, "Recents")
    data object Contacts : BottomTab(Icons.Filled.Contacts, "Contacts")
}

@Composable
fun HomeScreen() {
    var selectedTab by remember { mutableStateOf<BottomTab>(BottomTab.Recents) }
    var lastNonDialerTab by remember { mutableStateOf<BottomTab>(BottomTab.Recents) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contactsRepository = remember { ContactsRepository(context) }
    val recentCallRepository = remember { RecentCallRepository(context) }
    var syncStarted by remember { mutableStateOf(false) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                try {
                    contactsRepository.syncContacts()
                } catch (_: Exception) {
                    // Ignore sync failures at startup
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            syncStarted = true
            scope.launch {
                try {
                    contactsRepository.syncContacts()
                } catch (_: Exception) {
                    // Ignore sync failures at startup
                }
            }
        } else {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    LaunchedEffect(syncStarted) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission && !syncStarted) {
            syncStarted = true
            scope.launch {
                try {
                    contactsRepository.syncContacts()
                } catch (_: Exception) {
                    // Ignore sync failures at startup
                }
            }
        }
    }

    // Sync recents immediately when a call ends
    LaunchedEffect(Unit) {
        android.util.Log.d("HomeScreen", "LaunchedEffect started - listening for callEndedEvent")
        CallManager.callEndedEvent.collectLatest {
            android.util.Log.d(
                "HomeScreen",
                "Call ended event received in HomeScreen, syncing recents..."
            )
            val hasCallLogPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
            android.util.Log.d("HomeScreen", "Has call log permission: $hasCallLogPerm")
            if (hasCallLogPerm) {
                recentCallRepository.syncFromDevice()
                android.util.Log.d("HomeScreen", "Recents sync completed from HomeScreen")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedTab == BottomTab.Dialer) {
            BackHandler { selectedTab = lastNonDialerTab }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Keep all screens alive, just control visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Dialer screen
                if (selectedTab == BottomTab.Dialer) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(3f)
                    ) {
                        DialPadScreen(
                            onNavigateBack = { selectedTab = lastNonDialerTab }
                        )
                    }
                }

                // Favorites screen - always composed but visibility controlled
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (selectedTab == BottomTab.Favorites) 1f else 0f)
                        .graphicsLayer {
                            alpha = if (selectedTab == BottomTab.Favorites) 1f else 0f
                        }
                        .pointerInput(selectedTab) {
                            if (selectedTab != BottomTab.Favorites) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                ) {
                    if (selectedTab == BottomTab.Favorites) {
                        lastNonDialerTab = BottomTab.Favorites
                    }
                    FavoritesScreen(isActive = selectedTab == BottomTab.Favorites)
                }

                // Recents screen - always composed but visibility controlled
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (selectedTab == BottomTab.Recents) 1f else 0f)
                        .graphicsLayer {
                            alpha = if (selectedTab == BottomTab.Recents) 1f else 0f
                        }
                        .pointerInput(selectedTab) {
                            if (selectedTab != BottomTab.Recents) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                ) {
                    if (selectedTab == BottomTab.Recents) {
                        lastNonDialerTab = BottomTab.Recents
                    }
                    RecentsScreen(isActive = selectedTab == BottomTab.Recents)
                }

                // Contacts screen - always composed, visibility controlled
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (selectedTab == BottomTab.Contacts) 1f else 0f)
                        .graphicsLayer {
                            alpha = if (selectedTab == BottomTab.Contacts) 1f else 0f
                        }
                        .pointerInput(selectedTab) {
                            if (selectedTab != BottomTab.Contacts) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                ) {
                    if (selectedTab == BottomTab.Contacts) {
                        lastNonDialerTab = BottomTab.Contacts
                    }
                    ContactsScreen(isActive = selectedTab == BottomTab.Contacts)
                }
            }
        }

        BottomNavigationBar(
            selected = selectedTab,
            onSelect = { tab ->
                selectedTab = tab
                if (tab != BottomTab.Dialer) {
                    lastNonDialerTab = tab
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}

@Composable
fun BottomNavigationBar(
    selected: BottomTab,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomTab.Dialer,
        BottomTab.Favorites,
        BottomTab.Recents,
        BottomTab.Contacts
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { }
            )
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                drawLine(
                    color = Color.White,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = strokeWidth
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
    {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Block clicks on pill background */ }
                )
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { tab ->
                val isSelected = tab == selected

                if (isSelected) {
                    SelectedNavItem(tab = tab, onSelect = { onSelect(tab) })
                } else {
                    UnselectedNavItem(tab = tab, onSelect = { onSelect(tab) })
                }
            }
        }
    }
}


@Composable
private fun SelectedNavItem(tab: BottomTab, onSelect: () -> Unit) {
    val backdrop = object : Backdrop {
        override val isCoordinatesDependent: Boolean = false
        override fun DrawScope.drawBackdrop(
            density: Density,
            coordinates: LayoutCoordinates?,
            layerBlock: (GraphicsLayerScope.() -> Unit)?
        ) {
            // no-op
        }
    }

    Row(
        modifier = Modifier
            .clip(ContinuousCapsule())
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule() },
                effects = {
                    vibrancy()
                    blur(22f.dp.toPx())
                    lens(28f.dp.toPx(), 56f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.22f))
                }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = Color.White,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Color(0xFF2196F3), bounded = true)
                ) { onSelect() }
                .padding(4.dp)
        )
        Text(
            text = tab.label,
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun UnselectedNavItem(tab: BottomTab, onSelect: () -> Unit) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color(0xFF2196F3), bounded = true)
            ) { onSelect() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = Color.White,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .padding(4.dp)
        )
    }
}



