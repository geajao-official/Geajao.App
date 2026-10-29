package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.GifBox
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.data.api.PublicGifApiService
import com.example.data.api.PublicGifItem
import com.example.data.crypto.TacticalCryptoEngine
import com.example.data.model.CadetProfile
import com.example.data.model.ChatRoom
import com.example.data.model.EncryptedMessage
import com.example.data.model.Friendship
import com.example.data.model.GeajaoAiMessage
import com.example.data.model.GeajaoAiState
import com.example.data.model.GeajaoConstants
import com.example.data.model.UiState
import com.example.ui.components.Base64ImagePreview
import com.example.ui.components.CadetAvatarCircle
import com.example.ui.components.CadetRoleBadgePill
import com.example.ui.components.VoiceMessagePlayerBubble
import com.example.ui.components.VoiceMessageRecorderPanel
import com.example.ui.theme.BrassGold
import com.example.ui.theme.CrimsonHonor
import com.example.ui.theme.CyanTactical
import com.example.ui.theme.EmeraldVerified

@Composable
fun EncryptedChatScreen(
    currentUserId: String,
    allCadetsState: UiState<List<CadetProfile>>,
    friendshipsState: UiState<List<Friendship>>,
    chatRoomsState: UiState<List<ChatRoom>>,
    activeChatId: String?,
    activeMessagesState: UiState<List<EncryptedMessage>>,
    nowEpochMs: Long,
    isGeajaoAiOpen: Boolean = false,
    aiMessages: List<GeajaoAiMessage> = emptyList(),
    aiState: GeajaoAiState = GeajaoAiState.Idle,
    publicGifResultsState: UiState<List<PublicGifItem>> = UiState.Success(emptyList()),
    onToggleGeajaoAi: (Boolean) -> Unit = {},
    onSendAiPrompt: (String) -> Unit = {},
    onClearAiHistory: () -> Unit = {},
    onSearchPublicGifs: (String) -> Unit = {},
    onSelectChatRoom: (String?) -> Unit,
    onStartChatWithCadet: (CadetProfile, Long) -> Unit,
    onSendFriendRequest: (CadetProfile) -> Unit,
    onAcceptFriendRequest: (Friendship) -> Unit,
    onRemoveOrDeclineFriend: (Friendship) -> Unit,
    onSetDisappearingTimer: (String, Long) -> Unit,
    onSendEncryptedMessage: (ChatRoom, String, String, String) -> Unit,
    onDeleteMessage: (String, String) -> Unit
) {
    val activeRoom = remember(chatRoomsState, activeChatId) {
        (chatRoomsState as? UiState.Success)?.data?.find { it.id == activeChatId }
    }

    if (isGeajaoAiOpen) {
        GeajaoAiBotScreen(
            messages = aiMessages,
            aiState = aiState,
            onSendPrompt = onSendAiPrompt,
            onClearHistory = onClearAiHistory,
            onBack = { onToggleGeajaoAi(false) },
            showBackButton = true
        )
    } else if (activeRoom != null) {
        BackHandler {
            onSelectChatRoom(null)
        }
        EncryptedConversationDetailView(
            currentUserId = currentUserId,
            chatRoom = activeRoom,
            messagesState = activeMessagesState,
            nowEpochMs = nowEpochMs,
            publicGifResultsState = publicGifResultsState,
            onSearchPublicGifs = onSearchPublicGifs,
            onOpenGeajaoAi = { onToggleGeajaoAi(true) },
            onBack = { onSelectChatRoom(null) },
            onSetDisappearingTimer = { ttl -> onSetDisappearingTimer(activeRoom.id, ttl) },
            onSendMessage = { text, mediaType, mediaBase64 ->
                onSendEncryptedMessage(activeRoom, text, mediaType, mediaBase64)
            },
            onDeleteMessage = { msgId -> onDeleteMessage(activeRoom.id, msgId) }
        )
    } else {
        EncryptedChatListOverview(
            currentUserId = currentUserId,
            allCadetsState = allCadetsState,
            friendshipsState = friendshipsState,
            chatRoomsState = chatRoomsState,
            onOpenGeajaoAi = { onToggleGeajaoAi(true) },
            onOpenRoom = { onSelectChatRoom(it.id) },
            onStartNewChat = onStartChatWithCadet,
            onSendFriendRequest = onSendFriendRequest,
            onAcceptFriendRequest = onAcceptFriendRequest,
            onRemoveOrDeclineFriend = onRemoveOrDeclineFriend
        )
    }
}

@Composable
private fun EncryptedChatListOverview(
    currentUserId: String,
    allCadetsState: UiState<List<CadetProfile>>,
    friendshipsState: UiState<List<Friendship>>,
    chatRoomsState: UiState<List<ChatRoom>>,
    onOpenGeajaoAi: () -> Unit,
    onOpenRoom: (ChatRoom) -> Unit,
    onStartNewChat: (CadetProfile, Long) -> Unit,
    onSendFriendRequest: (CadetProfile) -> Unit,
    onAcceptFriendRequest: (Friendship) -> Unit,
    onRemoveOrDeclineFriend: (Friendship) -> Unit
) {
    val friendships = (friendshipsState as? UiState.Success)?.data ?: emptyList()
    val acceptedFriendUids = remember(friendships, currentUserId) {
        friendships.filter { it.isAccepted }
            .map { it.otherUid(currentUserId) }
            .toSet()
    }
    val incomingRequests = remember(friendships, currentUserId) {
        friendships.filter { !it.isAccepted && it.receiverId == currentUserId }
    }

    val allOtherCadets = (allCadetsState as? UiState.Success)?.data
        ?.filter { it.userId != currentUserId }
        ?: emptyList()

    val friendCadets = remember(allOtherCadets, acceptedFriendUids) {
        allOtherCadets.filter { acceptedFriendUids.contains(it.userId.ifBlank { it.id }) }
    }

    val nonFriendCadets = remember(allOtherCadets, acceptedFriendUids) {
        allOtherCadets.filter { !acceptedFriendUids.contains(it.userId.ifBlank { it.id }) }
    }

    // Filter chat rooms so only rooms with accepted friends are shown
    val rooms = ((chatRoomsState as? UiState.Success)?.data ?: emptyList()).filter { room ->
        val peerUid = room.participantUids.firstOrNull { it != currentUserId }
        peerUid != null && acceptedFriendUids.contains(peerUid)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EnhancedEncryption,
                            contentDescription = "AES-256-GCM",
                            tint = EmeraldVerified
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Friends-Only Encrypted Chat",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Only accepted Friends can chat with each other. Supports AES-256-GCM encryption, disappearing messages, photos, live public API GIFs, and Stickers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Pinned Official AI Bot: Geajao.AI
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenGeajaoAi() }
                    .testTag("open_geajao_ai_bot_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.5.dp, CyanTactical)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CyanTactical.copy(alpha = 0.2f),
                            border = BorderStroke(1.5.dp, CyanTactical),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = "Geajao.AI Bot",
                                    tint = CyanTactical,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Geajao.AI",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = CyanTactical.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "OFFICIAL AI BOT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanTactical,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Always Online • Ask about Cadet Colleges, ISSB, fitness, or message ideas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onOpenGeajaoAi,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanTactical,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("open_geajao_ai_bot_button")
                    ) {
                        Text("Chat AI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Incoming Friend Requests
        if (incomingRequests.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "INCOMING FRIEND REQUESTS (${incomingRequests.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        incomingRequests.forEach { req ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = req.requesterName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onAcceptFriendRequest(req) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldVerified)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Accept", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Accept")
                                    }
                                    OutlinedButton(onClick = { onRemoveOrDeclineFriend(req) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Decline", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Start Chat with Accepted Friends
        item {
            Text(
                text = "YOUR FRIENDS (${friendCadets.size}) • TAP TO CHAT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (friendCadets.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = BrassGold)
                        Text(
                            text = "You have no accepted Friends yet. Add a friend below to start chatting!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    friendCadets.forEach { friend ->
                        Button(
                            onClick = { onStartNewChat(friend, 300L) },
                            modifier = Modifier.testTag("start_friend_chat_${friend.userId}")
                        ) {
                            Text("💬 ${friend.displayName}")
                        }
                    }
                }
            }
        }

        // Active Encrypted Friend Channels
        item {
            Text(
                text = "ACTIVE FRIEND CHATS (${rooms.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (rooms.isNotEmpty()) {
            items(rooms, key = { it.id }) { room ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenRoom(room) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CadetAvatarCircle(
                            displayName = room.chatTitle,
                            badge = "Cadet"
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = room.chatTitle,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (room.disappearingTtlSeconds > 0L) {
                                    Surface(
                                        color = CrimsonHonor.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "⏱ ${formatTtlLabel(room.disappearingTtlSeconds)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CrimsonHonor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = room.lastMessagePreview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = room.encryptionKeyHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldVerified
                            )
                        }
                    }
                }
            }
        }

        // Add New Friends Section
        if (nonFriendCadets.isNotEmpty()) {
            item {
                Text(
                    text = "ADD FRIENDS TO UNLOCK CHAT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(nonFriendCadets, key = { "non_friend_${it.userId.ifBlank { it.id }}" }) { cadet ->
                val targetUid = cadet.userId.ifBlank { cadet.id }
                val pendingFriendship = friendships.find { it.participantUids.contains(targetUid) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            CadetAvatarCircle(
                                displayName = cadet.displayName,
                                badge = cadet.primaryBadge,
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = cadet.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${cadet.cadetNumber} • ${cadet.batchYear}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (pendingFriendship != null) {
                            if (pendingFriendship.receiverId == currentUserId) {
                                Button(
                                    onClick = { onAcceptFriendRequest(pendingFriendship) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldVerified)
                                ) {
                                    Text("Accept Friend")
                                }
                            } else {
                                OutlinedButton(onClick = {}, enabled = false) {
                                    Text("Request Sent")
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onSendFriendRequest(cadet) },
                                modifier = Modifier.testTag("chat_add_friend_$targetUid")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Friend")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EncryptedConversationDetailView(
    currentUserId: String,
    chatRoom: ChatRoom,
    messagesState: UiState<List<EncryptedMessage>>,
    nowEpochMs: Long,
    publicGifResultsState: UiState<List<PublicGifItem>>,
    onSearchPublicGifs: (String) -> Unit,
    onOpenGeajaoAi: () -> Unit,
    onBack: () -> Unit,
    onSetDisappearingTimer: (Long) -> Unit,
    onSendMessage: (String, String, String) -> Unit,
    onDeleteMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var messageInput by remember { mutableStateOf("") }
    var inspectedMessage by remember { mutableStateOf<EncryptedMessage?>(null) }
    var showMediaInputDialog by remember { mutableStateOf<String?>(null) }
    var customAttachmentPayload by remember { mutableStateOf("") }
    var showGifPicker by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var showVoiceRecorder by remember { mutableStateOf(false) }

    LaunchedEffect(showGifPicker) {
        if (showGifPicker) {
            onSearchPublicGifs("")
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            TacticalCryptoEngine.encodeUriToCompactBase64(context, uri)?.let { encoded ->
                onSendMessage(messageInput.ifBlank { "[Photo]" }, "IMAGE", encoded)
                messageInput = ""
            }
        }
    }

    val ttlOptions = listOf(
        0L to "Off",
        30L to "30s",
        300L to "5m",
        3600L to "1h",
        86400L to "24h"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chatRoom.chatTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = chatRoom.encryptionKeyHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldVerified
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Disappearing Timer",
                        tint = BrassGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Self-Destruct TTL:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ttlOptions.forEach { (seconds, label) ->
                        FilterChip(
                            selected = chatRoom.disappearingTtlSeconds == seconds,
                            onClick = { onSetDisappearingTimer(seconds) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        val messages = (messagesState as? UiState.Success)?.data ?: emptyList()
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.senderId == currentUserId
                val decryptedText = remember(msg.cipherText, msg.ivBase64, chatRoom.id) {
                    TacticalCryptoEngine.decryptMessage(msg.cipherText, msg.ivBase64, chatRoom.id)
                }
                val remainingSeconds = if (msg.expiresAtEpochMs > 0L) {
                    ((msg.expiresAtEpochMs - nowEpochMs) / 1000L).coerceAtLeast(0L)
                } else {
                    null
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                ) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 310.dp)
                            .clickable { inspectedMessage = msg },
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMine) 16.dp else 4.dp,
                            bottomEnd = if (isMine) 4.dp else 16.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMine) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (remainingSeconds != null) CrimsonHonor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = msg.senderName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                CadetRoleBadgePill(badge = msg.senderBadge)
                            }

                            when (msg.mediaType) {
                                "IMAGE" -> {
                                    Base64ImagePreview(
                                        dataUri = msg.mediaDataBase64,
                                        contentDescription = "Encrypted Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                    )
                                }
                                "GIF" -> {
                                    AnimatedGifBubblePreview(gifCode = msg.mediaDataBase64)
                                }
                                "STICKER" -> {
                                    StickerBubblePreview(stickerCode = msg.mediaDataBase64)
                                }
                                "VOICE_NOTE" -> {
                                    VoiceMessagePlayerBubble(
                                        mediaDataBase64 = msg.mediaDataBase64,
                                        transcriptOrLabel = decryptedText,
                                        messageId = msg.id.ifBlank { "voice_${msg.createdAt?.seconds ?: 0L}" }
                                    )
                                }
                                "GRID_COORDS" -> {
                                    Surface(
                                        color = EmeraldVerified.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = EmeraldVerified)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "📍 Location: ${msg.mediaDataBase64}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldVerified
                                            )
                                        }
                                    }
                                }
                            }

                            if (msg.mediaType != "STICKER" || decryptedText.isNotBlank()) {
                                Text(
                                    text = decryptedText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔒 Encrypted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldVerified
                                )
                                if (remainingSeconds != null) {
                                    Text(
                                        text = "🔥 ${remainingSeconds}s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CrimsonHonor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Multi-Media Attachment, GIFs, Stickers & Message Input Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showGifPicker = true },
                        modifier = Modifier.testTag("chat_open_gif_picker_button")
                    ) {
                        Icon(Icons.Default.GifBox, contentDescription = "GIFs", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GIFs", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { showStickerPicker = true },
                        modifier = Modifier.testTag("chat_open_sticker_picker_button")
                    ) {
                        Icon(Icons.Default.EmojiEmotions, contentDescription = "Stickers", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stickers", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onOpenGeajaoAi,
                        modifier = Modifier.testTag("chat_open_geajao_ai_button")
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = "Geajao.AI", tint = CyanTactical, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Geajao.AI", style = MaterialTheme.typography.labelSmall, color = CyanTactical)
                    }
                    OutlinedButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("chat_attach_photo_button")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Photo", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Photo", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            showVoiceRecorder = !showVoiceRecorder
                        },
                        modifier = Modifier.testTag("chat_send_voice_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Record Voice Message",
                            tint = if (showVoiceRecorder) CrimsonHonor else EmeraldVerified,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showVoiceRecorder) "Recording..." else "Voice Message",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            customAttachmentPayload = ""
                            showMediaInputDialog = "GRID_COORDS"
                        },
                        modifier = Modifier.testTag("chat_send_grid_button")
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Location", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Location", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (showVoiceRecorder) {
                    Spacer(modifier = Modifier.height(8.dp))
                    VoiceMessageRecorderPanel(
                        onSendVoiceNote = { summaryText, voiceDataUri ->
                            val caption = if (messageInput.isNotBlank()) {
                                "$summaryText • $messageInput"
                            } else {
                                summaryText
                            }
                            onSendMessage(caption, "VOICE_NOTE", voiceDataUri)
                            messageInput = ""
                            showVoiceRecorder = false
                        },
                        onCancel = {
                            showVoiceRecorder = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Write a message...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("encrypted_message_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { showVoiceRecorder = !showVoiceRecorder },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (showVoiceRecorder) CrimsonHonor.copy(alpha = 0.2f)
                                else EmeraldVerified.copy(alpha = 0.16f)
                            )
                            .testTag("chat_mic_record_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Record Short Voice Message",
                            tint = if (showVoiceRecorder) CrimsonHonor else EmeraldVerified
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                onSendMessage(messageInput, "NONE", "")
                                messageInput = ""
                            }
                        },
                        enabled = messageInput.isNotBlank(),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("send_encrypted_message_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }

    // Public API GIF Picker Modal
    if (showGifPicker) {
        PublicGifPickerDialog(
            publicGifResultsState = publicGifResultsState,
            onSearchPublicGifs = onSearchPublicGifs,
            onSelectPublicGif = { gifItem ->
                onSendMessage("GIF: ${gifItem.title}", "GIF", gifItem.toMediaPayload())
                showGifPicker = false
            },
            onSelectPresetGif = { presetCode, presetTitle ->
                onSendMessage("GIF: $presetTitle", "GIF", presetCode)
                showGifPicker = false
            },
            onDismiss = { showGifPicker = false }
        )
    }

    // Sticker Picker Modal
    if (showStickerPicker) {
        AlertDialog(
            onDismissRequest = { showStickerPicker = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEmotions, contentDescription = null, tint = EmeraldVerified)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send a Sticker")
                }
            },
            text = {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GeajaoConstants.CHAT_STICKERS.forEach { sticker ->
                        Surface(
                            modifier = Modifier
                                .width(105.dp)
                                .clickable {
                                    onSendMessage("${sticker.emoji} ${sticker.title}", "STICKER", sticker.code)
                                    showStickerPicker = false
                                }
                                .testTag("send_sticker_${sticker.code}"),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = sticker.emoji,
                                    style = MaterialTheme.typography.displayLarge
                                )
                                Text(
                                    text = sticker.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = sticker.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStickerPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showMediaInputDialog != null) {
        val mediaType = showMediaInputDialog!!
        AlertDialog(
            onDismissRequest = { showMediaInputDialog = null },
            title = {
                Text(if (mediaType == "VOICE_NOTE") "Attach Voice Memo" else "Attach Location")
            },
            text = {
                OutlinedTextField(
                    value = customAttachmentPayload,
                    onValueChange = { customAttachmentPayload = it },
                    label = {
                        Text(if (mediaType == "VOICE_NOTE") "Voice Memo Transcript / Duration" else "Location / Coordinates")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customAttachmentPayload.isNotBlank()) {
                            onSendMessage(customAttachmentPayload, mediaType, customAttachmentPayload)
                            showMediaInputDialog = null
                        }
                    },
                    enabled = customAttachmentPayload.isNotBlank()
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMediaInputDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (inspectedMessage != null) {
        val msg = inspectedMessage!!
        AlertDialog(
            onDismissRequest = { inspectedMessage = null },
            title = { Text("AES-256-GCM Cryptographic Proof") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Stored in Cloud Firestore (Zero Plaintext Exposure):",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrassGold
                    )
                    Text(
                        text = "Ciphertext (Base64):\n${msg.cipherText}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "96-bit GCM IV (Base64):\n${msg.ivBase64}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "TTL Seconds: ${msg.ttlSeconds}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonHonor
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedMessage = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteMessage(msg.id)
                        inspectedMessage = null
                    }
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = CrimsonHonor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Burn Message Now", color = CrimsonHonor)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PublicGifPickerDialog(
    publicGifResultsState: UiState<List<PublicGifItem>>,
    onSearchPublicGifs: (String) -> Unit,
    onSelectPublicGif: (PublicGifItem) -> Unit,
    onSelectPresetGif: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val gifImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryLabel by remember { mutableStateOf("Trending") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GifBox, contentDescription = null, tint = BrassGold)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Public GIF Reaction Picker", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Search live animated GIFs or tap a reaction category",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Public API Search Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search GIFs (e.g. salute, cheer, laugh)...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gif_picker_search_input")
                    )
                    Button(
                        onClick = {
                            selectedCategoryLabel = ""
                            onSearchPublicGifs(searchQuery)
                        },
                        modifier = Modifier.testTag("gif_picker_search_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search GIFs")
                    }
                }

                // Reaction Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PublicGifApiService.REACTION_CATEGORIES.forEach { (label, queryVal) ->
                        FilterChip(
                            selected = selectedCategoryLabel == label,
                            onClick = {
                                selectedCategoryLabel = label
                                searchQuery = queryVal
                                onSearchPublicGifs(queryVal)
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("gif_category_${label.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                // Public API Results Grid
                when (publicGifResultsState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = BrassGold
                                )
                                Text(
                                    text = "Fetching live GIFs from public API...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is UiState.Error -> {
                        Text(
                            text = publicGifResultsState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonHonor
                        )
                    }
                    is UiState.Success -> {
                        val apiGifs = publicGifResultsState.data
                        if (apiGifs.isNotEmpty()) {
                            Text(
                                text = "LIVE PUBLIC API GIFS (${apiGifs.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrassGold
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                apiGifs.forEachIndexed { idx, gifItem ->
                                    Surface(
                                        modifier = Modifier
                                            .width(126.dp)
                                            .clickable { onSelectPublicGif(gifItem) }
                                            .testTag("send_public_gif_$idx"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.55f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(86.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = gifItem.fallbackEmoji,
                                                    style = MaterialTheme.typography.headlineMedium
                                                )
                                                AsyncImage(
                                                    model = gifItem.previewUrl.ifBlank { gifItem.gifUrl },
                                                    imageLoader = gifImageLoader,
                                                    contentDescription = gifItem.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Text(
                                                text = gifItem.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Surface(
                                                color = BrassGold.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "GIF • ${gifItem.tag}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BrassGold,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Tactical Cadet Reactions (Instant Tap)
                Text(
                    text = "CADET INSTANT REACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldVerified
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GeajaoConstants.CHAT_GIFS.forEach { gif ->
                        Surface(
                            modifier = Modifier
                                .width(126.dp)
                                .clickable { onSelectPresetGif(gif.code, gif.title) }
                                .testTag("send_gif_${gif.code}"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = gif.emoji,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = gif.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AnimatedGifBubblePreview(gifCode: String) {
    val context = LocalContext.current
    val gifImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val publicGif = remember(gifCode) { PublicGifItem.fromMediaPayload(gifCode) }
    val presetGif = remember(gifCode) { GeajaoConstants.CHAT_GIFS.find { it.code == gifCode } }
    val infiniteTransition = rememberInfiniteTransition(label = "gif_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "gif_scale"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = BrassGold.copy(alpha = 0.16f),
        border = BorderStroke(1.5.dp, BrassGold)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (publicGif != null && publicGif.gifUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.scale(scale)) {
                        Text(
                            text = publicGif.fallbackEmoji,
                            style = MaterialTheme.typography.displayMedium
                        )
                    }
                    AsyncImage(
                        model = publicGif.gifUrl,
                        imageLoader = gifImageLoader,
                        contentDescription = publicGif.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    text = publicGif.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrassGold
                )
                Surface(
                    color = BrassGold,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "ANIMATED GIF • ${publicGif.tag}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.scale(scale)) {
                    Text(
                        text = presetGif?.emoji ?: "🎬✨",
                        style = MaterialTheme.typography.displayLarge
                    )
                }
                Text(
                    text = presetGif?.title ?: gifCode,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrassGold
                )
                Surface(
                    color = BrassGold,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "ANIMATED GIF • ${presetGif?.tag ?: "LIVE"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StickerBubblePreview(stickerCode: String) {
    val sticker = GeajaoConstants.CHAT_STICKERS.find { it.code == stickerCode }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = EmeraldVerified.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = sticker?.emoji ?: "🎖️",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = sticker?.title ?: stickerCode,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EmeraldVerified
            )
        }
    }
}

private fun formatTtlLabel(seconds: Long): String = when (seconds) {
    0L -> "Off"
    30L -> "30s"
    300L -> "5m"
    3600L -> "1h"
    86400L -> "24h"
    else -> "${seconds}s"
}
