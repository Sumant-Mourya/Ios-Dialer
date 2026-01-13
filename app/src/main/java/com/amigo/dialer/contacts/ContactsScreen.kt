package com.amigo.dialer.contacts

import android.Manifest
import android.content.Intent
import android.widget.Toast
import android.content.pm.PackageManager
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.amigo.dialer.R
import com.amigo.dialer.call.CallActivity
import com.amigo.dialer.call.CallManager
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars

@Composable
fun ContactsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val repository = remember { ContactsRepository(context) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    
    var syncCounter by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    
    // Auto-focus and show keyboard when search becomes active
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) {
            focusManager.clearFocus()
        }
    }
    
    // Use produceState to handle search query changes smoothly
    val activeQuery by produceState(initialValue = "", searchQuery, syncCounter) {
        kotlinx.coroutines.delay(300) // Debounce
        value = searchQuery
    }
    
    // Single stable paging flow that switches based on query
    val contactsPagingItems = remember(activeQuery, syncCounter) {
        if (activeQuery.isBlank()) {
            repository.getContactsPaged()
        } else {
            repository.searchContactsPaged(activeQuery)
        }
    }.collectAsLazyPagingItems()
    
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCallPermission by remember { mutableStateOf(false) }

    // Call permission launcher
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCallPermission = permissions[Manifest.permission.CALL_PHONE] == true
        if (!hasCallPermission) {
            Toast.makeText(context, "Call permission is required to make calls", Toast.LENGTH_SHORT).show()
        }
    }

    val backdrop = remember {
        object : Backdrop {
            override val isCoordinatesDependent: Boolean = false
            override fun DrawScope.drawBackdrop(
                density: Density,
                coordinates: LayoutCoordinates?,
                layerBlock: (androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit)?
            ) {
                // Empty implementation
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        android.util.Log.d("ContactsScreen", "Permission granted: $isGranted")
        if (isGranted) {
            // Trigger pager refresh to load from Room
            syncCounter++
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }

        // Request call permissions
        callPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background Image
        Image(
            painter = painterResource(R.drawable.bg),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Log item count for debugging
        LaunchedEffect(contactsPagingItems.itemCount) {
            android.util.Log.d("ContactsScreen", "LazyPagingItems itemCount: ${contactsPagingItems.itemCount}")
        }

        if (contactsPagingItems.itemCount == 0 && !isSearchActive) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (hasPermission) "No contacts found" else "Grant permission to view contacts",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.systemBars))
                }

                item {
                    if (!isSearchActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Contacts",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Row(
                                modifier = Modifier
                                    .clickable { isSearchActive = true }
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { ContinuousRoundedRectangle(12.dp) },
                                        effects = {
                                            vibrancy()
                                            blur(8f.dp.toPx())
                                            lens(16f.dp.toPx(), 32f.dp.toPx())
                                        },
                                        onDrawSurface = {
                                            drawRect(Color.White.copy(alpha = 0.05f))
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Search",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { ContinuousRoundedRectangle(16.dp) },
                                    effects = {
                                        vibrancy()
                                        blur(8f.dp.toPx())
                                        lens(16f.dp.toPx(), 32f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.08f))
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                            
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search contacts...",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 16.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            
                            IconButton(
                                onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close search",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                items(
                    count = contactsPagingItems.itemCount,
                    key = contactsPagingItems.itemKey { it.id }
                ) { index ->
                    val contact = contactsPagingItems[index]
                    contact?.let {
                        ContactItem(
                            contact = it,
                            onClick = {
                                if (hasCallPermission) {
                                    // Start call using CallManager
                                    CallManager.startCall(
                                        context = context,
                                        phoneNumber = it.phoneNumber,
                                        callerName = it.name
                                    )
                                    
                                    // Launch CallActivity
                                    val intent = Intent(context, CallActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Please grant call permission to make calls",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    callPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.CALL_PHONE,
                                            Manifest.permission.READ_PHONE_STATE
                                        )
                                    )
                                }
                            }
                        )
                    }
                }

                // Show no results message when searching with no matches
                if (isSearchActive && contactsPagingItems.itemCount == 0) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No contacts found",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    Spacer(modifier = Modifier.height(88.dp))
                }
            }
        }
    }
}
