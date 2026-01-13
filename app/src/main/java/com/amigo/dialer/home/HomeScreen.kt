package com.amigo.dialer.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.systemBars
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
import com.amigo.dialer.contacts.FavoritesScreen
import com.amigo.dialer.dialpad.DialPadScreen
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedTab == BottomTab.Dialer) {
            BackHandler { selectedTab = lastNonDialerTab }
        }

        // Keep all screens alive, just control visibility
        Box(modifier = Modifier.fillMaxSize()) {
            // Dialer screen
            if (selectedTab == BottomTab.Dialer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
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
            ) {
                if (selectedTab == BottomTab.Favorites) {
                    lastNonDialerTab = BottomTab.Favorites
                }
                FavoritesScreen()
            }

            // Recents screen - always composed but visibility controlled
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (selectedTab == BottomTab.Recents) 1f else 0f)
                    .graphicsLayer {
                        alpha = if (selectedTab == BottomTab.Recents) 1f else 0f
                    }
            ) {
                if (selectedTab == BottomTab.Recents) {
                    lastNonDialerTab = BottomTab.Recents
                }
                PlaceholderScreen(title = "Recents", subtitle = "Recent calls")
            }

            // Contacts screen - always composed, visibility controlled
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (selectedTab == BottomTab.Contacts) 1f else 0f)
                    .graphicsLayer {
                        alpha = if (selectedTab == BottomTab.Contacts) 1f else 0f
                    }
            ) {
                if (selectedTab == BottomTab.Contacts) {
                    lastNonDialerTab = BottomTab.Contacts
                }
                ContactsScreen()
            }
        }

        if (selectedTab != BottomTab.Dialer) {
            BottomNavPill(
                selected = selectedTab,
                onSelect = { tab ->
                    selectedTab = tab
                    if (tab != BottomTab.Dialer) {
                        lastNonDialerTab = tab
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            )
        }
    }
}

@Composable
private fun BottomNavPill(
    selected: BottomTab,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
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

    val items = listOf(BottomTab.Dialer, BottomTab.Favorites, BottomTab.Recents, BottomTab.Contacts)

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule() },
                effects = {
                    vibrancy()
                    blur(32f.dp.toPx())
                    lens(36f.dp.toPx(), 72f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.14f))
                }
            )
            // Block clicks from passing through the pill background
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
    Icon(
        imageVector = tab.icon,
        contentDescription = tab.label,
        tint = Color.White.copy(alpha = 0.7f),
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color(0xFF2196F3), bounded = true)
            ) { onSelect() }
            .padding(4.dp)
    )
}

@Composable
private fun PlaceholderScreen(title: String, subtitle: String) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = painterResource(R.drawable.bg),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                    .padding(vertical = 32.dp, horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }
}
