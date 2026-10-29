package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.CadetProfile
import com.example.data.model.CallSession
import com.example.data.model.UiState
import com.example.ui.components.CadetAvatarCircle
import com.example.ui.components.CadetRoleBadgePill
import com.example.ui.theme.BrassGold
import com.example.ui.theme.CrimsonHonor
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay

@Composable
fun CallCommsScreen(
    currentUserId: String,
    allCadetsState: UiState<List<CadetProfile>>,
    callSessionsState: UiState<List<CallSession>>,
    activeCallId: String?,
    onInitiateCall: (CadetProfile, String) -> Unit,
    onSelectCallSession: (String?) -> Unit,
    onUpdateCallState: (
        callSession: CallSession,
        newStatus: String,
        isCallerMuted: Boolean,
        isCalleeMuted: Boolean,
        isVideoEnabled: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCameraPerm = perms[Manifest.permission.CAMERA] ?: hasCameraPerm
        hasAudioPerm = perms[Manifest.permission.RECORD_AUDIO] ?: hasAudioPerm
    }

    val sessions = (callSessionsState as? UiState.Success)?.data ?: emptyList()
    val ongoingCall = remember(sessions, activeCallId) {
        sessions.firstOrNull { it.id == activeCallId && it.status != "ENDED" }
            ?: sessions.firstOrNull { it.status == "RINGING" || it.status == "ACTIVE" }
    }

    if (ongoingCall != null) {
        ActiveCallHUD(
            call = ongoingCall,
            currentUserId = currentUserId,
            hasCameraPermission = hasCameraPerm,
            hasAudioPermission = hasAudioPerm,
            onRequestPermissions = {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                )
            },
            onUpdateCallState = onUpdateCallState,
            onEndCall = {
                onUpdateCallState(
                    ongoingCall,
                    "ENDED",
                    ongoingCall.isCallerMuted,
                    ongoingCall.isCalleeMuted,
                    ongoingCall.isVideoEnabled
                )
                onSelectCallSession(null)
            }
        )
    } else {
        val peers = (allCadetsState as? UiState.Success)?.data
            ?.filter { it.userId != currentUserId }
            ?: emptyList()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("call_comms_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Tactical Comms",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tactical Voice & Video Signaling",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Real-time Firestore signaling with live CameraX feed, microphone controls, and instant incoming call alerts across verified Cadets.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!hasCameraPerm || !hasAudioPerm) {
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                    )
                                },
                                modifier = Modifier.testTag("grant_comms_permissions_button")
                            ) {
                                Text("Grant Camera & Mic Hardware Access")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "CORPS COMMS DIRECTORY (${peers.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (peers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No other cadets online yet.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Once additional cadets register in Geajao, you can place direct Voice and Video calls to them from this directory.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(peers, key = { it.userId.ifBlank { it.id } }) { cadet ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("call_peer_${cadet.userId}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CadetAvatarCircle(
                                displayName = cadet.displayName,
                                badge = cadet.primaryBadge,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = cadet.displayName.ifBlank { "Cadet" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    CadetRoleBadgePill(badge = cadet.primaryBadge)
                                }
                                Text(
                                    text = "${cadet.cadetNumber.ifBlank { "Unassigned" }} • College: ${cadet.cadetCollege}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledIconButton(
                                onClick = {
                                    if (!hasAudioPerm) {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                    }
                                    onInitiateCall(cadet, "VOICE")
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                modifier = Modifier.testTag("voice_call_button_${cadet.userId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Start Voice Call"
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledIconButton(
                                onClick = {
                                    if (!hasCameraPerm || !hasAudioPerm) {
                                        permissionLauncher.launch(
                                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                        )
                                    }
                                    onInitiateCall(cadet, "VIDEO")
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("video_call_button_${cadet.userId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Start Video Call"
                                )
                            }
                        }
                    }
                }
            }

            if (sessions.isNotEmpty()) {
                item {
                    Text(
                        text = "RECENT SIGNALING LOGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(sessions.take(8), key = { it.id }) { call ->
                    val peerName = if (call.callerId == currentUserId) call.calleeName else call.callerName
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (call.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                                    contentDescription = call.callType,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "${call.callType} Call • $peerName",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Status: ${call.status} • ${call.encryptionFingerprint}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveCallHUD(
    call: CallSession,
    currentUserId: String,
    hasCameraPermission: Boolean,
    hasAudioPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onUpdateCallState: (
        callSession: CallSession,
        newStatus: String,
        isCallerMuted: Boolean,
        isCalleeMuted: Boolean,
        isVideoEnabled: Boolean
    ) -> Unit,
    onEndCall: () -> Unit
) {
    val isVideoCall = call.callType == "VIDEO"
    val isCaller = call.callerId == currentUserId
    val isIncomingRinging = call.status == "RINGING" && call.calleeId == currentUserId
    val peerName = if (isCaller) call.calleeName else call.callerName
    val isMyMicMuted = if (isCaller) call.isCallerMuted else call.isCalleeMuted
    var useFrontCamera by remember { mutableStateOf(true) }
    var elapsedSec by remember { mutableLongStateOf(0L) }

    LaunchedEffect(call.id, call.status) {
        if (call.status == "ACTIVE") {
            val startMs = call.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
            while (true) {
                elapsedSec = ((System.currentTimeMillis() - startMs).coerceAtLeast(0L)) / 1000L
                delay(1000L)
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "call_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070D17))
            .testTag("active_call_hud")
    ) {
        if (isVideoCall && call.isVideoEnabled && hasCameraPermission) {
            CameraXLivePreview(
                useFrontCamera = useFrontCamera,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    CadetAvatarCircle(
                        displayName = peerName,
                        badge = call.callerBadge,
                        size = 112.dp
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = peerName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (call.status == "RINGING") {
                        if (isIncomingRinging) "INCOMING ${call.callType} SIGNAL..." else "DIALING CORPS FREQUENCY..."
                    } else {
                        "ENCRYPTED ${call.callType} LINK • %02d:%02d".format(elapsedSec / 60, elapsedSec % 60)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = BrassGold
                )

                if (!hasCameraPermission || !hasAudioPermission) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRequestPermissions) {
                        Text("Enable Hardware Camera / Mic")
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            shape = RoundedCornerShape(999.dp),
            color = Color.Black.copy(alpha = 0.65f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "$peerName • ${call.status}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF101A2B).copy(alpha = 0.92f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isIncomingRinging) {
                    FilledIconButton(
                        onClick = {
                            onUpdateCallState(
                                call,
                                "ACTIVE",
                                call.isCallerMuted,
                                call.isCalleeMuted,
                                call.isVideoEnabled
                            )
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .testTag("accept_call_button"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = EmeraldVerified,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Accept Call")
                    }
                } else if (call.status == "RINGING") {
                    Button(
                        onClick = {
                            onUpdateCallState(
                                call,
                                "ACTIVE",
                                call.isCallerMuted,
                                call.isCalleeMuted,
                                call.isVideoEnabled
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldVerified),
                        modifier = Modifier.testTag("connect_call_now_button")
                    ) {
                        Text("Establish Link")
                    }
                }

                FilledIconButton(
                    onClick = {
                        val nextCallerMuted = if (isCaller) !call.isCallerMuted else call.isCallerMuted
                        val nextCalleeMuted = if (!isCaller) !call.isCalleeMuted else call.isCalleeMuted
                        onUpdateCallState(
                            call,
                            call.status,
                            nextCallerMuted,
                            nextCalleeMuted,
                            call.isVideoEnabled
                        )
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("toggle_mute_button"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isMyMicMuted) CrimsonHonor else Color(0xFF1E2D46),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isMyMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Toggle Microphone"
                    )
                }

                if (isVideoCall) {
                    FilledIconButton(
                        onClick = {
                            onUpdateCallState(
                                call,
                                call.status,
                                call.isCallerMuted,
                                call.isCalleeMuted,
                                !call.isVideoEnabled
                            )
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("toggle_camera_button"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (!call.isVideoEnabled) CrimsonHonor else Color(0xFF1E2D46),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (call.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Toggle Camera"
                        )
                    }

                    FilledIconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("switch_camera_button"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF1E2D46),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Front/Back Camera"
                        )
                    }
                }

                FilledIconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(60.dp)
                        .testTag("end_call_button"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = CrimsonHonor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call"
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraXLivePreview(
    useFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(useFrontCamera, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        cameraProviderFuture.addListener({
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val selector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
            }
        }, executor)

        onDispose {
            runCatching {
                cameraProviderFuture.get().unbindAll()
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
