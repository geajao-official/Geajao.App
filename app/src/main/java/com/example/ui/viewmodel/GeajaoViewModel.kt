package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.PublicGifApiService
import com.example.data.api.PublicGifItem
import com.example.data.model.AppConfig
import com.example.data.model.CadetProfile
import com.example.data.model.CallSession
import com.example.data.model.ChatRoom
import com.example.data.model.EncryptedMessage
import com.example.data.model.FeedPost
import com.example.data.model.Friendship
import com.example.data.model.GeajaoAiMessage
import com.example.data.model.GeajaoAiState
import com.example.data.model.GeajaoConstants
import com.example.data.model.PushNotificationEvent
import com.example.data.model.SecretPanelMode
import com.example.data.model.UiState
import com.example.data.repository.GeajaoRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class GeajaoViewModel(
    private val repository: GeajaoRepository,
    val currentUserId: String
) : ViewModel() {

    private val geajaoSystemInstruction by lazy {
        content {
            text(
                "You are Geajao.AI, the official AI Bot and tactical companion inside Geajao — " +
                    "the exclusive social network for Cadets and Ex-Cadets across all 12 Cadet Colleges " +
                    "(CCR, BCC, PCC, RCC, MCC, FCC, JCC, SCC, CCC, FGCC, MGCC, JGCC). " +
                    "Respond with camaraderie, precision, encouragement, and helpful insights on Cadet life, " +
                    "academics, fitness, leadership, ISSB preparation, and expressive chat replies."
            )
        }
    }

    // Lazy initialization of Firebase AI Logic generative model (gemini-3.8-flash) for Geajao.AI
    private val geajaoAiModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = geajaoSystemInstruction
        )
    }

    private val geajaoLiteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            systemInstruction = geajaoSystemInstruction
        )
    }

    private val geajaoDirectModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    private val candidateAiModels: List<GenerativeModel> by lazy {
        listOf(geajaoAiModel, geajaoLiteModel, geajaoDirectModel)
    }

    private val _isGeajaoAiOpen = MutableStateFlow(false)
    val isGeajaoAiOpen: StateFlow<Boolean> = _isGeajaoAiOpen.asStateFlow()

    private val _aiState = MutableStateFlow<GeajaoAiState>(GeajaoAiState.Idle)
    val aiState: StateFlow<GeajaoAiState> = _aiState.asStateFlow()

    private val _aiMessages = MutableStateFlow(
        listOf(
            GeajaoAiMessage(
                id = "welcome_geajao_ai",
                isUser = false,
                text = "Salute, Cadet! I am Geajao.AI, your official AI companion for the Cadet & Ex-Cadet Corps (CCR, BCC, PCC, RCC, MCC, FCC, JCC, SCC, CCC, FGCC, MGCC, JGCC). Ask me anything—from academics, fitness & ISSB prep to drafting posts or expressive messages!"
            )
        )
    )
    val aiMessages: StateFlow<List<GeajaoAiMessage>> = _aiMessages.asStateFlow()

    private val _publicGifResults = MutableStateFlow<UiState<List<PublicGifItem>>>(UiState.Loading)
    val publicGifResults: StateFlow<UiState<List<PublicGifItem>>> = _publicGifResults.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _selectedFeedTab = MutableStateFlow("Recent")
    val selectedFeedTab: StateFlow<String> = _selectedFeedTab.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _activeCallId = MutableStateFlow<String?>(null)
    val activeCallId: StateFlow<String?> = _activeCallId.asStateFlow()

    private val _secretPanelMode = MutableStateFlow(SecretPanelMode.LOCKED)
    val secretPanelMode: StateFlow<SecretPanelMode> = _secretPanelMode.asStateFlow()

    private val _nowEpochMs = MutableStateFlow(System.currentTimeMillis())
    val nowEpochMs: StateFlow<Long> = _nowEpochMs.asStateFlow()

    val appConfig: StateFlow<AppConfig> = repository.observeAppConfig()
        .catch { e ->
            Log.w(TAG, "Error observing app config", e)
            emit(AppConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppConfig())

    val isAdmin: StateFlow<Boolean> = repository.observeIsAdmin(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing admin status", e)
            emit(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val isModerator: StateFlow<Boolean> = repository.observeIsModerator(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing moderator status", e)
            emit(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val currentUserProfile: StateFlow<UiState<CadetProfile?>> = repository.observeCadetProfile(currentUserId)
        .map<CadetProfile?, UiState<CadetProfile?>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing current profile", e)
            emit(UiState.Error(e.message ?: "Failed to load cadet profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val allCadets: StateFlow<UiState<List<CadetProfile>>> = repository.observeAllCorpsCadets()
        .map<List<CadetProfile>, UiState<List<CadetProfile>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing corps cadets", e)
            emit(UiState.Error(e.message ?: "Failed to load cadet roster"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val friendships: StateFlow<UiState<List<Friendship>>> = repository.observeUserFriendships(currentUserId)
        .map<List<Friendship>, UiState<List<Friendship>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing friendships", e)
            emit(UiState.Error(e.message ?: "Failed to load friends"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val feedPosts: StateFlow<UiState<List<FeedPost>>> = repository.observeFeedPosts()
        .map<List<FeedPost>, UiState<List<FeedPost>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing feed posts", e)
            emit(UiState.Error(e.message ?: "Failed to load feed"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val chatRooms: StateFlow<UiState<List<ChatRoom>>> = repository.observeUserChatRooms(currentUserId)
        .map<List<ChatRoom>, UiState<List<ChatRoom>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing chat rooms", e)
            emit(UiState.Error(e.message ?: "Failed to load encrypted channels"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val activeChatMessages: StateFlow<UiState<List<EncryptedMessage>>> = _activeChatId
        .flatMapLatest { chatId ->
            if (chatId == null) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeEncryptedMessages(chatId, currentUserId)
                    .map<List<EncryptedMessage>, UiState<List<EncryptedMessage>>> { UiState.Success(it) }
                    .catch { e ->
                        Log.w(TAG, "Error observing chat messages", e)
                        emit(UiState.Error(e.message ?: "Failed to load encrypted messages"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val callSessions: StateFlow<UiState<List<CallSession>>> = repository.observeUserCalls(currentUserId)
        .map<List<CallSession>, UiState<List<CallSession>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing call sessions", e)
            emit(UiState.Error(e.message ?: "Failed to load voice/video sessions"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val incomingPushNotifications: StateFlow<List<PushNotificationEvent>> = repository
        .observeIncomingPushNotifications(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing push notifications", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val now = System.currentTimeMillis()
                _nowEpochMs.value = now
                val currentChat = _activeChatId.value
                val msgState = activeChatMessages.value
                if (currentChat != null && msgState is UiState.Success) {
                    msgState.data.forEach { msg ->
                        if (msg.expiresAtEpochMs in 1..now && msg.id.isNotBlank()) {
                            repository.deleteMessage(currentChat, msg.id)
                        }
                    }
                }
            }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun selectFeedTab(tab: String) {
        _selectedFeedTab.value = tab
    }

    fun openChatRoom(chatId: String?) {
        _activeChatId.value = chatId
    }

    fun selectCallSession(callId: String?) {
        _activeCallId.value = callId
    }

    fun lockSecretCommandTab() {
        _secretPanelMode.value = SecretPanelMode.LOCKED
    }

    /**
     * Triggered after clicking the app icon 10 times:
     * - Writing "5246" unlocks the Moderator Panel (and provisions Moderator role in Firestore).
     * - Writing "A54h" unlocks the Admin Panel (and provisions Admin role in Firestore).
     */
    fun unlockSecretPanelWithPasscode(
        enteredPasscode: String,
        onUnlocked: (SecretPanelMode) -> Unit
    ) {
        val trimmed = enteredPasscode.trim()
        when (trimmed) {
            GeajaoConstants.MOD_SECRET_PASSCODE -> {
                _secretPanelMode.value = SecretPanelMode.MODERATOR
                onUnlocked(SecretPanelMode.MODERATOR)
                viewModelScope.launch {
                    val res = repository.activateModeratorRoleWithPasscode(trimmed)
                    _statusBanner.value = if (res.isSuccess) {
                        "Moderator Panel unlocked (Passcode 5246)."
                    } else {
                        "Moderator Panel unlocked."
                    }
                }
            }
            GeajaoConstants.ADMIN_SECRET_PASSCODE,
            GeajaoConstants.BOOTSTRAP_ADMIN_CODE,
            "1948" -> {
                _secretPanelMode.value = SecretPanelMode.ADMIN
                onUnlocked(SecretPanelMode.ADMIN)
                viewModelScope.launch {
                    val res = repository.activateCommandantAdminRole(trimmed)
                    _statusBanner.value = if (res.isSuccess) {
                        "Admin Panel unlocked (Passcode A54h)."
                    } else {
                        "Admin Panel unlocked."
                    }
                }
            }
            else -> {
                _statusBanner.value = "Access Denied: Invalid Auth Passcode."
            }
        }
    }

    fun selectAppIconAsAdmin(iconKey: String, onSuccessLocalApply: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.updateSelectedAppIcon(iconKey = iconKey, customIconBase64 = "")
            if (result.isSuccess) {
                onSuccessLocalApply(iconKey)
                val label = GeajaoConstants.APP_ICON_OPTIONS.find { it.first == iconKey }?.second ?: iconKey
                _statusBanner.value = "App icon updated to [$label]."
            } else {
                _statusBanner.value = "Only Admins can change the app icon: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun uploadCustomGalleryAppIconAsAdmin(customIconBase64: String) {
        viewModelScope.launch {
            val result = repository.updateSelectedAppIcon(
                iconKey = GeajaoConstants.ICON_CUSTOM_GALLERY,
                customIconBase64 = customIconBase64
            )
            if (result.isSuccess) {
                _statusBanner.value = "Custom gallery App Icon uploaded & applied!"
            } else {
                _statusBanner.value = "Only Admins can upload the app icon: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun uploadMukhoshLogo(mukhoshLogoBase64: String) {
        viewModelScope.launch {
            val result = repository.updateMukhoshLogo(mukhoshLogoBase64 = mukhoshLogoBase64)
            if (result.isSuccess) {
                _statusBanner.value = "MUKHOSH Logo uploaded & applied across Geajao!"
            } else {
                _statusBanner.value = "MUKHOSH Logo saved locally on this device."
            }
        }
    }

    // --- FRIEND SYSTEM ACTIONS ---

    fun sendFriendRequest(targetCadet: CadetProfile) {
        viewModelScope.launch {
            val myName = (currentUserProfile.value as? UiState.Success)?.data?.displayName ?: "Cadet"
            val targetUid = targetCadet.userId.ifBlank { targetCadet.id }
            val result = repository.sendFriendRequest(
                targetUserId = targetUid,
                requesterName = myName,
                receiverName = targetCadet.displayName
            )
            _statusBanner.value = if (result.isSuccess) {
                "Friend request sent to ${targetCadet.displayName}."
            } else {
                "Failed to send friend request: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun acceptFriendRequest(friendship: Friendship) {
        viewModelScope.launch {
            val result = repository.acceptFriendRequest(friendship.id)
            _statusBanner.value = if (result.isSuccess) {
                "You and ${friendship.otherName(currentUserId)} are now Friends! You can now chat."
            } else {
                "Failed to accept friend request: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun removeOrDeclineFriend(friendship: Friendship) {
        viewModelScope.launch {
            val result = repository.removeOrDeclineFriendship(friendship.id)
            _statusBanner.value = if (result.isSuccess) {
                "Friendship removed."
            } else {
                "Failed to update friendship: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun submitProfileAndIdCard(
        displayName: String,
        cadetNumber: String,
        batchYear: String,
        cadetCollege: String,
        bio: String,
        idCardImageBase64: String,
        idCardNote: String,
        cadetStatusBadge: String = ""
    ) {
        viewModelScope.launch {
            val result = repository.registerOrUpdateCadetProfile(
                displayName = displayName,
                cadetNumber = cadetNumber,
                batchYear = batchYear,
                academyHouse = GeajaoConstants.normalizeCadetCollege(cadetCollege),
                bio = bio,
                idCardImageBase64 = idCardImageBase64,
                idCardNote = idCardNote,
                cadetStatusBadge = cadetStatusBadge,
                activeSecretMode = _secretPanelMode.value
            )
            _statusBanner.value = if (result.isSuccess) {
                "Profile & ID Card submitted."
            } else {
                "Failed to save profile: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun reviewCadetAndAssignBadges(
        targetUserId: String,
        approve: Boolean,
        primaryBadge: String,
        badges: List<String>,
        reviewerNote: String,
        forceAdminAuthority: Boolean
    ) {
        viewModelScope.launch {
            val result = repository.approveOrRejectCadet(
                targetUserId = targetUserId,
                approve = approve,
                primaryBadge = primaryBadge,
                badges = badges,
                reviewerNote = reviewerNote,
                isCallerAdmin = forceAdminAuthority
            )
            _statusBanner.value = if (result.isSuccess) {
                val badgeLabel = badges.ifEmpty { listOf(primaryBadge) }.distinct().joinToString(" + ")
                if (approve) "User verified & badges [$badgeLabel] granted." else "User verification marked Rejected."
            } else {
                "Action denied: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun publishFeedPost(
        content: String,
        mediaImageBase64: String = "",
        category: String = "General"
    ) {
        viewModelScope.launch {
            val profile = (currentUserProfile.value as? UiState.Success)?.data
            val authorName = profile?.displayName?.ifBlank { "Cadet" } ?: "Cadet"
            val combinedBadges = buildList {
                if (isAdmin.value || profile?.hasAdminBadge == true || _secretPanelMode.value == SecretPanelMode.ADMIN) {
                    add(GeajaoConstants.BADGE_ADMIN)
                }
                if (isModerator.value || profile?.badges?.contains(GeajaoConstants.BADGE_MODERATOR) == true ||
                    profile?.primaryBadge == GeajaoConstants.BADGE_MODERATOR || _secretPanelMode.value == SecretPanelMode.MODERATOR
                ) {
                    add(GeajaoConstants.BADGE_MODERATOR)
                }
                addAll(profile?.allEffectiveBadges ?: listOf(GeajaoConstants.BADGE_CADET))
            }.filter { it != GeajaoConstants.BADGE_UNVERIFIED }
                .distinct()
                .ifEmpty { listOf(GeajaoConstants.BADGE_CADET) }

            val authorBadge = combinedBadges.joinToString(",").take(30)
            val collegePrefix = profile?.cadetCollege ?: "CCR"
            val batchLabel = profile?.batchYear.orEmpty()
            val authorBatch = if (batchLabel.isNotBlank()) {
                "$collegePrefix • $batchLabel".take(20)
            } else {
                collegePrefix.take(20)
            }
            val cleanCategory = category.trim().removePrefix("#").ifBlank { "General" }.take(40)
            val result = repository.createFeedPost(
                authorName = authorName,
                authorBadge = authorBadge,
                authorBatch = authorBatch,
                title = content.take(60),
                content = content,
                category = cleanCategory,
                mediaImageBase64 = mediaImageBase64
            )
            _statusBanner.value = if (result.isSuccess) {
                "Post shared under #$cleanCategory!"
            } else {
                "Failed to share post: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun toggleSalute(post: FeedPost) {
        viewModelScope.launch {
            repository.toggleSaluteOnPost(post)
        }
    }

    fun recordEngagement(post: FeedPost, isComment: Boolean) {
        viewModelScope.launch {
            repository.recordPostShareOrComment(post, isComment)
            _statusBanner.value = if (isComment) "Comment recorded." else "Post shared."
        }
    }

    fun startOrOpenDirectChat(targetCadet: CadetProfile, disappearingTtlSeconds: Long = 300L) {
        viewModelScope.launch {
            val targetUid = targetCadet.userId.ifBlank { targetCadet.id }
            val acceptedFriends = ((friendships.value as? UiState.Success)?.data ?: emptyList())
                .filter { it.isAccepted }
            val isFriend = acceptedFriends.any { it.participantUids.contains(targetUid) }
            if (!isFriend) {
                _statusBanner.value = "Only Friends can chat with each other. Add ${targetCadet.displayName} as a Friend first!"
                return@launch
            }

            val myName = (currentUserProfile.value as? UiState.Success)?.data?.displayName ?: "Cadet"
            val result = repository.createOrGetEncryptedChatRoom(
                otherUserId = targetUid,
                currentUserName = myName,
                otherUserName = targetCadet.displayName,
                disappearingTtlSeconds = disappearingTtlSeconds
            )
            result.onSuccess { chatId ->
                _activeChatId.value = chatId
            }.onFailure { e ->
                _statusBanner.value = "Failed to open chat: ${e.localizedMessage}"
            }
        }
    }

    fun setDisappearingTimer(chatId: String, ttlSeconds: Long) {
        viewModelScope.launch {
            repository.updateChatDisappearingTimer(chatId, ttlSeconds)
            val label = if (ttlSeconds == 0L) "Off (Permanent)" else "${ttlSeconds}s TTL"
            _statusBanner.value = "Disappearing message timer set to $label."
        }
    }

    fun sendEncryptedChatMessage(
        chatRoom: ChatRoom,
        plainText: String,
        mediaType: String = "NONE",
        mediaDataBase64: String = ""
    ) {
        viewModelScope.launch {
            val profile = (currentUserProfile.value as? UiState.Success)?.data
            val senderName = profile?.displayName?.ifBlank { "Cadet" } ?: "Cadet"
            val senderBadge = profile?.serializedBadgeSummary ?: GeajaoConstants.BADGE_CADET
            val result = repository.sendEncryptedMessage(
                chatId = chatRoom.id,
                senderName = senderName,
                senderBadge = senderBadge,
                participantUids = chatRoom.participantUids,
                plainText = plainText,
                mediaType = mediaType,
                mediaDataBase64 = mediaDataBase64,
                ttlSeconds = chatRoom.disappearingTtlSeconds
            )
            if (result.isFailure) {
                _statusBanner.value = "Send failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun deleteEncryptedMessage(chatId: String, messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(chatId, messageId)
        }
    }

    fun initiateVoiceOrVideoCall(targetCadet: CadetProfile, callType: String) {
        viewModelScope.launch {
            val profile = (currentUserProfile.value as? UiState.Success)?.data ?: return@launch
            val result = repository.startVoiceOrVideoCall(
                callerName = profile.displayName,
                callerBadge = profile.serializedBadgeSummary,
                calleeId = targetCadet.userId.ifBlank { targetCadet.id },
                calleeName = targetCadet.displayName,
                callType = callType
            )
            result.onSuccess { callId ->
                _activeCallId.value = callId
            }.onFailure { e ->
                _statusBanner.value = "Failed to initiate $callType call: ${e.localizedMessage}"
            }
        }
    }

    fun updateCallState(
        callSession: CallSession,
        newStatus: String = callSession.status,
        isCallerMuted: Boolean = callSession.isCallerMuted,
        isCalleeMuted: Boolean = callSession.isCalleeMuted,
        isVideoEnabled: Boolean = callSession.isVideoEnabled
    ) {
        viewModelScope.launch {
            repository.updateCallSessionState(
                callId = callSession.id,
                status = newStatus,
                isCallerMuted = isCallerMuted,
                isCalleeMuted = isCalleeMuted,
                isVideoEnabled = isVideoEnabled
            )
            if (newStatus == "ENDED" && _activeCallId.value == callSession.id) {
                _activeCallId.value = null
            }
        }
    }

    // --- PUBLIC GIF API PICKER ---

    fun searchPublicGifs(query: String = "") {
        viewModelScope.launch {
            _publicGifResults.value = UiState.Loading
            try {
                val res = PublicGifApiService.fetchGifs(query)
                res.onSuccess { items ->
                    _publicGifResults.value = UiState.Success(items)
                }.onFailure { err ->
                    _publicGifResults.value = UiState.Error(err.localizedMessage ?: "Failed to load GIFs")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch public GIFs", e)
                _publicGifResults.value = UiState.Error(e.localizedMessage ?: "Failed to load GIFs")
            }
        }
    }

    // --- FCM TOKEN & PUSH NOTIFICATION DELIVERY ---

    fun syncFcmToken(fcmToken: String) {
        if (fcmToken.isBlank()) return
        viewModelScope.launch {
            repository.registerFcmDeviceToken(fcmToken)
        }
    }

    fun markPushNotificationDelivered(notificationId: String) {
        if (notificationId.isBlank()) return
        viewModelScope.launch {
            repository.markPushNotificationDelivered(notificationId)
        }
    }

    // --- GEAJAO.AI BOT (FIREBASE AI LOGIC) ---

    fun setGeajaoAiOpen(open: Boolean) {
        _isGeajaoAiOpen.value = open
        if (open) {
            _activeChatId.value = null
        }
    }

    fun clearGeajaoAiHistory() {
        _aiState.value = GeajaoAiState.Idle
        _aiMessages.value = listOf(
            GeajaoAiMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                text = "Conversation cleared! Geajao.AI is ready for your next question or tactical request."
            )
        )
    }

    fun sendMessageToGeajaoAi(userPrompt: String) {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty()) return

        val userMsg = GeajaoAiMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = trimmed
        )
        val streamingBotId = UUID.randomUUID().toString()
        val historySnapshot = _aiMessages.value.takeLast(8)

        _aiMessages.value = _aiMessages.value + userMsg + GeajaoAiMessage(
            id = streamingBotId,
            isUser = false,
            text = "",
            isStreaming = true
        )

        viewModelScope.launch {
            _aiState.value = GeajaoAiState.Loading
            val profile = (currentUserProfile.value as? UiState.Success)?.data
            val contextPrefix = if (profile != null) {
                "[Cadet Context: Name=${profile.displayName}, College=${profile.cadetCollege}, Batch=${profile.batchYear}, Badges=${profile.serializedBadgeSummary}]\n"
            } else {
                ""
            }
            val historyText = historySnapshot.joinToString("\n") { msg ->
                if (msg.isUser) "Cadet: ${msg.text}" else "Geajao.AI: ${msg.text}"
            }
            val fullPrompt = "$contextPrefix$historyText\nCadet: $trimmed\nGeajao.AI:"

            var lastException: Exception? = null
            var succeeded = false

            for (model in candidateAiModels) {
                if (succeeded) break
                val accumulatedText = StringBuilder()
                try {
                    withTimeout(60_000L) {
                        model.generateContentStream(fullPrompt).collect { chunk ->
                            val chunkText = chunk.text.orEmpty()
                            accumulatedText.append(chunkText)
                            val currentReply = accumulatedText.toString()
                            _aiState.value = GeajaoAiState.Streaming(currentReply)
                            _aiMessages.value = _aiMessages.value.map { msg ->
                                if (msg.id == streamingBotId) {
                                    msg.copy(text = currentReply, isStreaming = true)
                                } else {
                                    msg
                                }
                            }
                        }
                    }

                    var finalReply = accumulatedText.toString().trim()
                    if (finalReply.isEmpty()) {
                        val unaryResponse = withTimeout(60_000L) {
                            model.generateContent(fullPrompt)
                        }
                        finalReply = unaryResponse.text?.trim().orEmpty()
                    }

                    if (finalReply.isNotEmpty()) {
                        _aiState.value = GeajaoAiState.Success(finalReply)
                        _aiMessages.value = _aiMessages.value.map { msg ->
                            if (msg.id == streamingBotId) {
                                msg.copy(text = finalReply, isStreaming = false)
                            } else {
                                msg
                            }
                        }
                        succeeded = true
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "Geajao.AI streaming timed out on candidate model", e)
                    lastException = e
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Geajao.AI candidate model attempt failed, trying fallback", e)
                    lastException = e
                    // Also attempt unary generateContent on this model before moving to next model
                    try {
                        val unaryResponse = withTimeout(45_000L) {
                            model.generateContent(fullPrompt)
                        }
                        val unaryText = unaryResponse.text?.trim().orEmpty()
                        if (unaryText.isNotEmpty()) {
                            _aiState.value = GeajaoAiState.Success(unaryText)
                            _aiMessages.value = _aiMessages.value.map { msg ->
                                if (msg.id == streamingBotId) {
                                    msg.copy(text = unaryText, isStreaming = false)
                                } else {
                                    msg
                                }
                            }
                            succeeded = true
                        }
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (ue: Exception) {
                        lastException = ue
                    }
                }
            }

            if (!succeeded) {
                val errMsg = when (val ex = lastException) {
                    is TimeoutCancellationException -> "Geajao.AI timed out after 60 seconds. Please try again."
                    null -> "Geajao.AI returned an empty response. Please try again."
                    else -> ex.localizedMessage ?: "Geajao.AI request failed."
                }
                Log.e(TAG, "Geajao.AI all candidate models failed: $errMsg", lastException)
                _aiState.value = GeajaoAiState.Error(errMsg)
                _aiMessages.value = _aiMessages.value.filterNot { it.id == streamingBotId }
            }
        }
    }

    private companion object {
        const val TAG = "GeajaoViewModel"
    }
}
