package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import com.example.data.model.GeajaoConstants
import com.example.data.model.SecretPanelMode
import com.example.data.model.UiState
import com.example.data.repository.GeajaoRepository
import com.example.notifications.GeajaoNotificationHelper
import com.example.ui.components.AppIconManager
import com.example.ui.components.CadetRoleBadgePill
import com.example.ui.components.FirstTimeUserGuidePreferences
import com.example.ui.components.GeajaoAppIconEmblem
import com.example.ui.components.GeajaoUserGuideOverlay
import com.example.ui.components.MukhoshLogoEmblem
import com.example.ui.components.MukhoshLogoPreferences
import com.example.ui.components.VerificationStatusChip
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CadetIdVerificationGateScreen
import com.example.ui.screens.CallCommsScreen
import com.example.ui.screens.CommandCenterScreen
import com.example.ui.screens.DiscoveryFeedScreen
import com.example.ui.screens.EncryptedChatScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.GeajaoAiBotScreen
import com.example.ui.screens.GeajaoSignInScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.signOutCadet
import com.example.ui.theme.CyanTactical
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemePreferences
import com.example.ui.viewmodel.GeajaoViewModel
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class GeajaoTab(val title: String, val icon: ImageVector, val testTag: String) {
    FEED("Feed", Icons.Default.DynamicFeed, "nav_tab_feed"),
    FRIENDS("Friends", Icons.Default.Group, "nav_tab_friends"),
    CHAT("Chat", Icons.Default.Lock, "nav_tab_chat"),
    CALLS("Calls", Icons.Default.Call, "nav_tab_calls"),
    ANALYTICS("Analytics", Icons.Default.Analytics, "nav_tab_analytics"),
    COMMAND("Command", Icons.Default.AdminPanelSettings, "nav_tab_command"),
    PROFILE("Profile", Icons.Default.Person, "nav_tab_profile")
}

class MainActivity : ComponentActivity() {
    private var pendingNavigateTab by mutableStateOf<String?>(null)
    private var pendingTargetChatId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)
        ThemePreferences.init(this)
        MukhoshLogoPreferences.init(this)
        GeajaoNotificationHelper.ensureNotificationChannels(this)
        extractNotificationDeepLink(intent)

        enableEdgeToEdge()
        setContent {
            val isDarkMode by ThemePreferences.isDarkModeFlow.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isDarkMode) {
                GeajaoRootContainer(
                    pendingNavigateTab = pendingNavigateTab,
                    pendingTargetChatId = pendingTargetChatId,
                    onConsumeDeepLink = {
                        pendingNavigateTab = null
                        pendingTargetChatId = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
        extractNotificationDeepLink(intent)
    }

    private fun extractNotificationDeepLink(intent: Intent?) {
        val navTab = intent?.getStringExtra(GeajaoNotificationHelper.EXTRA_NAVIGATE_TAB)
        val chatId = intent?.getStringExtra(GeajaoNotificationHelper.EXTRA_TARGET_CHAT_ID)
        if (!navTab.isNullOrBlank()) {
            pendingNavigateTab = navTab
            pendingTargetChatId = chatId
        }
    }
}

@Composable
fun GeajaoRootContainer(
    pendingNavigateTab: String? = null,
    pendingTargetChatId: String? = null,
    onConsumeDeepLink: () -> Unit = {}
) {
    var isWindowSynced by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Allow initial wmsSync-VRI and edge-to-edge VRI SurfaceSyncGroup frames to commit immediately (<15ms)
        androidx.compose.runtime.withFrameNanos { }
        androidx.compose.runtime.withFrameNanos { }
        isWindowSynced = true
    }

    if (!isWindowSynced) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val context = LocalContext.current
    LaunchedEffect(context) {
        ThemePreferences.init(context)
        MukhoshLogoPreferences.init(context)
    }
    val rootAuthScope = rememberCoroutineScope()
    var currentUserId by remember { mutableStateOf(Firebase.auth.currentUser?.uid) }
    var showUserGuideOverlay by rememberSaveable {
        mutableStateOf(FirstTimeUserGuidePreferences.shouldShowFirstTimeGuide(context))
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUserId = auth.currentUser?.uid
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val uid = currentUserId
        if (uid == null) {
            GeajaoSignInScreen(
                onAuthSuccess = {
                    currentUserId = Firebase.auth.currentUser?.uid
                },
                onOpenUserGuide = {
                    showUserGuideOverlay = true
                },
                externalScope = rootAuthScope
            )
        } else {
            val dbId = context.getString(R.string.firestore_database_id)
            val firestore = remember(dbId) {
                FirebaseFirestore.getInstance(Firebase.app, dbId)
            }
            val repository = remember(firestore) {
                GeajaoRepository(db = firestore, auth = Firebase.auth)
            }
            val vm: GeajaoViewModel = viewModel(
                key = "geajao_vm_$uid",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return GeajaoViewModel(repository = repository, currentUserId = uid) as T
                    }
                }
            )
            GeajaoAuthenticatedShell(
                viewModel = vm,
                onSignedOut = { currentUserId = null },
                authScope = rootAuthScope,
                pendingNavigateTab = pendingNavigateTab,
                pendingTargetChatId = pendingTargetChatId,
                onConsumeDeepLink = onConsumeDeepLink,
                onOpenUserGuide = { showUserGuideOverlay = true }
            )
        }

        if (showUserGuideOverlay) {
            GeajaoUserGuideOverlay(
                onDismiss = {
                    FirstTimeUserGuidePreferences.markFirstTimeGuideCompleted(context)
                    showUserGuideOverlay = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeajaoAuthenticatedShell(
    viewModel: GeajaoViewModel,
    onSignedOut: () -> Unit,
    authScope: kotlinx.coroutines.CoroutineScope = rememberCoroutineScope(),
    pendingNavigateTab: String? = null,
    pendingTargetChatId: String? = null,
    onConsumeDeepLink: () -> Unit = {},
    onOpenUserGuide: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = authScope
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val profileState by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val allCadetsState by viewModel.allCadets.collectAsStateWithLifecycle()
    val friendshipsState by viewModel.friendships.collectAsStateWithLifecycle()
    val feedPostsState by viewModel.feedPosts.collectAsStateWithLifecycle()
    val chatRoomsState by viewModel.chatRooms.collectAsStateWithLifecycle()
    val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeChatMessages.collectAsStateWithLifecycle()
    val callSessionsState by viewModel.callSessions.collectAsStateWithLifecycle()
    val activeCallId by viewModel.activeCallId.collectAsStateWithLifecycle()
    val selectedFeedTab by viewModel.selectedFeedTab.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isModerator by viewModel.isModerator.collectAsStateWithLifecycle()
    val secretPanelMode by viewModel.secretPanelMode.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    val nowEpochMs by viewModel.nowEpochMs.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val isGeajaoAiOpen by viewModel.isGeajaoAiOpen.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val publicGifResults by viewModel.publicGifResults.collectAsStateWithLifecycle()
    val incomingPushNotifications by viewModel.incomingPushNotifications.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by rememberSaveable { mutableStateOf(GeajaoTab.FEED) }
    var showVerificationGateModal by rememberSaveable { mutableStateOf(false) }
    var showSecretPasscodeDialog by rememberSaveable { mutableStateOf(false) }
    var iconTapCount by remember { mutableIntStateOf(0) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled; FCM token sync proceeds regardless
    }

    // Request POST_NOTIFICATIONS runtime permission on Android 13+ and sync FCM registration token
    LaunchedEffect(viewModel.currentUserId) {
        GeajaoNotificationHelper.ensureNotificationChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !GeajaoNotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val fcmToken = GeajaoNotificationHelper.fetchCurrentFcmToken(context, viewModel.currentUserId)
        if (!fcmToken.isNullOrBlank()) {
            viewModel.syncFcmToken(fcmToken)
        }
    }

    // Deliver real-time system notifications for incoming private messages and friend requests
    LaunchedEffect(incomingPushNotifications, activeChatId, currentTab) {
        incomingPushNotifications.forEach { event ->
            val isViewingSameChat = currentTab == GeajaoTab.CHAT &&
                !isGeajaoAiOpen &&
                activeChatId != null &&
                activeChatId == event.targetId
            if (!isViewingSameChat) {
                when (event.type) {
                    "FRIEND_REQUEST", "FRIEND_ACCEPTED" -> {
                        GeajaoNotificationHelper.showFriendRequestNotification(
                            context = context,
                            requesterName = event.senderName,
                            friendshipId = event.targetId.ifBlank { event.id },
                            isAccepted = event.type == "FRIEND_ACCEPTED"
                        )
                    }
                    else -> {
                        GeajaoNotificationHelper.showPrivateMessageNotification(
                            context = context,
                            senderName = event.senderName,
                            senderBadge = event.senderBadge,
                            messagePreview = event.body,
                            chatId = event.targetId,
                            notificationIdSeed = event.id.ifBlank { event.targetId }
                        )
                    }
                }
            }
            viewModel.markPushNotificationDelivered(event.id)
        }
    }

    // Handle deep-link navigation when user taps a push notification in the status bar
    LaunchedEffect(pendingNavigateTab, pendingTargetChatId) {
        when (pendingNavigateTab) {
            GeajaoNotificationHelper.TAB_TARGET_CHAT -> {
                viewModel.setGeajaoAiOpen(false)
                currentTab = GeajaoTab.CHAT
                if (!pendingTargetChatId.isNullOrBlank()) {
                    viewModel.openChatRoom(pendingTargetChatId)
                }
                onConsumeDeepLink()
            }
            GeajaoNotificationHelper.TAB_TARGET_FRIENDS -> {
                currentTab = GeajaoTab.FRIENDS
                onConsumeDeepLink()
            }
        }
    }

    val isSecretPanelUnlocked = secretPanelMode != SecretPanelMode.LOCKED

    LaunchedEffect(isSecretPanelUnlocked) {
        if (!isSecretPanelUnlocked && currentTab == GeajaoTab.COMMAND) {
            currentTab = GeajaoTab.FEED
        }
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    if (showSecretPasscodeDialog) {
        SecretCommandPasscodeDialog(
            onDismiss = { showSecretPasscodeDialog = false },
            onSubmitPasscode = { passcode ->
                viewModel.unlockSecretPanelWithPasscode(passcode) {
                    showSecretPasscodeDialog = false
                    showVerificationGateModal = false
                    currentTab = GeajaoTab.COMMAND
                }
            }
        )
    }

    val currentProfile = (profileState as? UiState.Success)?.data
    val hasAdminAuthority = isAdmin || currentProfile?.hasAdminBadge == true || secretPanelMode == SecretPanelMode.ADMIN
    val hasModOrAdminAuthority = hasAdminAuthority || isModerator || currentProfile?.hasModeratorBadge == true || secretPanelMode == SecretPanelMode.MODERATOR
    val effectiveUserBadges = remember(currentProfile, hasAdminAuthority, isModerator, secretPanelMode) {
        buildList {
            if (hasAdminAuthority) add(GeajaoConstants.BADGE_ADMIN)
            if (isModerator ||
                currentProfile?.badges?.contains(GeajaoConstants.BADGE_MODERATOR) == true ||
                currentProfile?.primaryBadge == GeajaoConstants.BADGE_MODERATOR ||
                secretPanelMode == SecretPanelMode.MODERATOR
            ) {
                add(GeajaoConstants.BADGE_MODERATOR)
            }
            addAll(currentProfile?.allEffectiveBadges ?: listOf(GeajaoConstants.BADGE_CADET))
        }.distinct()
    }

    // Strict ID Card Verification Gate:
    // Without publishing an ID Card and being approved, the user MUST remain on the Verifying Page
    // (unless they explicitly unlocked the Moderator/Admin Panel via the 6-click passcode in this session).
    val hasPublishedIdCard = currentProfile != null && currentProfile.idCardImageBase64.isNotBlank()
    val isVerifiedWithIdCard = hasPublishedIdCard && currentProfile?.isApproved == true

    LaunchedEffect(isVerifiedWithIdCard, isSecretPanelUnlocked) {
        if (!isVerifiedWithIdCard && isSecretPanelUnlocked) {
            currentTab = GeajaoTab.COMMAND
        }
    }

    if ((!isVerifiedWithIdCard && !isSecretPanelUnlocked) || showVerificationGateModal) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showVerificationGateModal && (isVerifiedWithIdCard || isSecretPanelUnlocked)) {
                    BackHandler { showVerificationGateModal = false }
                }
                CadetIdVerificationGateScreen(
                    existingProfile = currentProfile,
                    selectedAppIconKey = appConfig.selectedIconKey,
                    customIconBase64 = appConfig.customIconBase64,
                    mukhoshLogoBase64 = appConfig.mukhoshLogoBase64,
                    onUploadMukhoshLogo = { encodedLogo ->
                        viewModel.uploadMukhoshLogo(encodedLogo)
                    },
                    onSubmitProfile = { name, number, batch, college, bio, idCardBase64, note, cadetStatus ->
                        viewModel.submitProfileAndIdCard(
                            displayName = name,
                            cadetNumber = number,
                            batchYear = batch,
                            cadetCollege = college,
                            bio = bio,
                            idCardImageBase64 = idCardBase64,
                            idCardNote = note,
                            cadetStatusBadge = cadetStatus
                        )
                        if (isVerifiedWithIdCard) {
                            showVerificationGateModal = false
                        }
                    },
                    onOpenSecretPasscodePrompt = {
                        showSecretPasscodeDialog = true
                    },
                    onSignOut = {
                        signOutCadet(
                            credentialManager = credentialManager,
                            onSignOutComplete = onSignedOut,
                            scope = scope
                        )
                    },
                    onOpenUserGuide = onOpenUserGuide
                )
            }
        }
        return
    }

    val pendingReviewCount = ((allCadetsState as? UiState.Success)?.data ?: emptyList())
        .count { it.verificationStatus == GeajaoConstants.STATUS_PENDING }

    val incomingFriendRequestCount = ((friendshipsState as? UiState.Success)?.data ?: emptyList())
        .count { !it.isAccepted && it.receiverId == viewModel.currentUserId }

    val visibleTabs = remember(isVerifiedWithIdCard, isSecretPanelUnlocked) {
        if (!isVerifiedWithIdCard && isSecretPanelUnlocked) {
            listOf(GeajaoTab.COMMAND)
        } else {
            GeajaoTab.entries.filter { tab ->
                tab != GeajaoTab.COMMAND || isSecretPanelUnlocked
            }
        }
    }

    BackHandler(enabled = isVerifiedWithIdCard && currentTab != GeajaoTab.FEED) {
        currentTab = GeajaoTab.FEED
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Clicking the App Icon 6 times opens the Auth Passcode Dialog
                            GeajaoAppIconEmblem(
                                iconKey = appConfig.selectedIconKey,
                                customIconBase64 = appConfig.customIconBase64,
                                size = 38.dp,
                                modifier = Modifier
                                    .clickable {
                                        iconTapCount += 1
                                        if (iconTapCount >= GeajaoConstants.ICON_SECRET_TAP_THRESHOLD) {
                                            iconTapCount = 0
                                            showSecretPasscodeDialog = true
                                        }
                                    }
                                    .testTag("top_bar_emblem_6_tap_trigger")
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "GEAJAO",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    CadetRoleBadgePill(
                                        badge = currentProfile?.primaryBadge ?: GeajaoConstants.BADGE_CADET,
                                        additionalBadges = effectiveUserBadges
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    MukhoshLogoEmblem(
                                        mukhoshLogoBase64 = appConfig.mukhoshLogoBase64,
                                        size = 15.dp
                                    )
                                    Text(
                                        text = "${currentProfile?.cadetCollege ?: "CCR"} • ${GeajaoConstants.MADE_BY_MUKHOSH_TAG}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        val isDarkMode by ThemePreferences.isDarkModeFlow.collectAsStateWithLifecycle()
                        IconButton(
                            onClick = { ThemePreferences.toggleDarkMode(context) },
                            modifier = Modifier.testTag("top_bar_theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.setGeajaoAiOpen(true)
                                if (isVerifiedWithIdCard) {
                                    currentTab = GeajaoTab.CHAT
                                }
                            },
                            modifier = Modifier.testTag("top_bar_geajao_ai_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "Open Geajao.AI Bot",
                                tint = CyanTactical
                            )
                        }
                        if (isVerifiedWithIdCard) {
                            IconButton(
                                onClick = { currentTab = GeajaoTab.FRIENDS },
                                modifier = Modifier.testTag("top_bar_friends_button")
                            ) {
                                if (incomingFriendRequestCount > 0) {
                                    BadgedBox(
                                        badge = { Badge { Text("$incomingFriendRequestCount") } }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = "Open Friend-Making Page"
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Open Friend-Making Page"
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { showVerificationGateModal = true },
                            modifier = Modifier.testTag("open_id_card_modal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = "Inspect or Update Cadet ID Card"
                            )
                        }
                        IconButton(
                            onClick = onOpenUserGuide,
                            modifier = Modifier.testTag("top_bar_user_guide_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Open User Guide & Contact Info",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar {
                        visibleTabs.forEach { tab ->
                            val tabLabel = if (tab == GeajaoTab.COMMAND) {
                                if (secretPanelMode == SecretPanelMode.MODERATOR) "Mod Panel" else "Admin Panel"
                            } else {
                                tab.title
                            }
                            val badgeCount = when (tab) {
                                GeajaoTab.COMMAND -> pendingReviewCount
                                GeajaoTab.FRIENDS -> incomingFriendRequestCount
                                else -> 0
                            }
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = {
                                    if (badgeCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge { Text("$badgeCount") }
                                            }
                                        ) {
                                            Icon(tab.icon, contentDescription = tabLabel)
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = tabLabel)
                                    }
                                },
                                label = { Text(tabLabel) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        visibleTabs.forEach { tab ->
                            val tabLabel = if (tab == GeajaoTab.COMMAND) {
                                if (secretPanelMode == SecretPanelMode.MODERATOR) "Mod Panel" else "Admin Panel"
                            } else {
                                tab.title
                            }
                            val badgeCount = when (tab) {
                                GeajaoTab.COMMAND -> pendingReviewCount
                                GeajaoTab.FRIENDS -> incomingFriendRequestCount
                                else -> 0
                            }
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = {
                                    if (badgeCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge { Text("$badgeCount") }
                                            }
                                        ) {
                                            Icon(tab.icon, contentDescription = tabLabel)
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = tabLabel)
                                    }
                                },
                                label = { Text(tabLabel) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isGeajaoAiOpen && currentTab != GeajaoTab.CHAT) {
                        GeajaoAiBotScreen(
                            messages = aiMessages,
                            aiState = aiState,
                            onSendPrompt = { viewModel.sendMessageToGeajaoAi(it) },
                            onClearHistory = { viewModel.clearGeajaoAiHistory() },
                            onBack = { viewModel.setGeajaoAiOpen(false) },
                            showBackButton = true
                        )
                    } else {
                        when (currentTab) {
                            GeajaoTab.FEED -> DiscoveryFeedScreen(
                            currentUserId = viewModel.currentUserId,
                            currentProfile = currentProfile,
                            effectiveUserBadges = effectiveUserBadges,
                            allCadetsState = allCadetsState,
                            friendshipsState = friendshipsState,
                            feedPostsState = feedPostsState,
                            selectedFeedTab = selectedFeedTab,
                            onSelectFeedTab = { viewModel.selectFeedTab(it) },
                            onPublishPost = { content, mediaBase64, category ->
                                viewModel.publishFeedPost(content, mediaBase64, category)
                            },
                            onToggleSalute = { post ->
                                viewModel.toggleSalute(post)
                            },
                            onRecordCommentOrShare = { post, isComment ->
                                viewModel.recordEngagement(post, isComment)
                            },
                            onSendFriendRequest = { cadet ->
                                viewModel.sendFriendRequest(cadet)
                            },
                            onAcceptFriendRequest = { friendship ->
                                viewModel.acceptFriendRequest(friendship)
                            },
                            onRemoveOrDeclineFriend = { friendship ->
                                viewModel.removeOrDeclineFriend(friendship)
                            },
                            onStartChatWithCadet = { cadet ->
                                viewModel.setGeajaoAiOpen(false)
                                viewModel.startOrOpenDirectChat(cadet)
                                currentTab = GeajaoTab.CHAT
                            },
                            onStartCallWithCadet = { cadet, callType ->
                                viewModel.initiateVoiceOrVideoCall(cadet, callType)
                                currentTab = GeajaoTab.CALLS
                            },
                            onOpenFriendsPage = {
                                currentTab = GeajaoTab.FRIENDS
                            }
                        )

                        GeajaoTab.FRIENDS -> FriendsScreen(
                            currentUserId = viewModel.currentUserId,
                            allCadetsState = allCadetsState,
                            friendshipsState = friendshipsState,
                            onSendFriendRequest = { cadet ->
                                viewModel.sendFriendRequest(cadet)
                            },
                            onAcceptFriendRequest = { friendship ->
                                viewModel.acceptFriendRequest(friendship)
                            },
                            onRemoveOrDeclineFriend = { friendship ->
                                viewModel.removeOrDeclineFriend(friendship)
                            },
                            onStartChatWithCadet = { cadet ->
                                viewModel.setGeajaoAiOpen(false)
                                viewModel.startOrOpenDirectChat(cadet)
                                currentTab = GeajaoTab.CHAT
                            },
                            onStartCallWithCadet = { cadet, callType ->
                                viewModel.initiateVoiceOrVideoCall(cadet, callType)
                                currentTab = GeajaoTab.CALLS
                            }
                        )

                        GeajaoTab.CHAT -> EncryptedChatScreen(
                            currentUserId = viewModel.currentUserId,
                            allCadetsState = allCadetsState,
                            friendshipsState = friendshipsState,
                            chatRoomsState = chatRoomsState,
                            activeChatId = activeChatId,
                            activeMessagesState = activeMessagesState,
                            nowEpochMs = nowEpochMs,
                            isGeajaoAiOpen = isGeajaoAiOpen,
                            aiMessages = aiMessages,
                            aiState = aiState,
                            publicGifResultsState = publicGifResults,
                            onToggleGeajaoAi = { viewModel.setGeajaoAiOpen(it) },
                            onSendAiPrompt = { viewModel.sendMessageToGeajaoAi(it) },
                            onClearAiHistory = { viewModel.clearGeajaoAiHistory() },
                            onSearchPublicGifs = { viewModel.searchPublicGifs(it) },
                            onSelectChatRoom = { viewModel.openChatRoom(it) },
                            onStartChatWithCadet = { cadet, ttl ->
                                viewModel.setGeajaoAiOpen(false)
                                viewModel.startOrOpenDirectChat(cadet, ttl)
                            },
                            onSendFriendRequest = { cadet ->
                                viewModel.sendFriendRequest(cadet)
                            },
                            onAcceptFriendRequest = { friendship ->
                                viewModel.acceptFriendRequest(friendship)
                            },
                            onRemoveOrDeclineFriend = { friendship ->
                                viewModel.removeOrDeclineFriend(friendship)
                            },
                            onSetDisappearingTimer = { chatId, ttl ->
                                viewModel.setDisappearingTimer(chatId, ttl)
                            },
                            onSendEncryptedMessage = { room, text, mediaType, mediaBase64 ->
                                viewModel.sendEncryptedChatMessage(room, text, mediaType, mediaBase64)
                            },
                            onDeleteMessage = { chatId, msgId ->
                                viewModel.deleteEncryptedMessage(chatId, msgId)
                            }
                        )

                        GeajaoTab.CALLS -> CallCommsScreen(
                            currentUserId = viewModel.currentUserId,
                            allCadetsState = allCadetsState,
                            callSessionsState = callSessionsState,
                            activeCallId = activeCallId,
                            onInitiateCall = { cadet, callType ->
                                viewModel.initiateVoiceOrVideoCall(cadet, callType)
                            },
                            onSelectCallSession = { viewModel.selectCallSession(it) },
                            onUpdateCallState = { call, status, callerMuted, calleeMuted, videoEnabled ->
                                viewModel.updateCallState(
                                    callSession = call,
                                    newStatus = status,
                                    isCallerMuted = callerMuted,
                                    isCalleeMuted = calleeMuted,
                                    isVideoEnabled = videoEnabled
                                )
                            }
                        )

                        GeajaoTab.ANALYTICS -> AnalyticsScreen(
                            currentUserId = viewModel.currentUserId,
                            allCadetsState = allCadetsState,
                            feedPostsState = feedPostsState
                        )

                        GeajaoTab.COMMAND -> CommandCenterScreen(
                            allCadetsState = allCadetsState,
                            secretPanelMode = secretPanelMode,
                            hasAdminAuthority = hasAdminAuthority,
                            hasModOrAdminAuthority = hasModOrAdminAuthority,
                            selectedAppIconKey = appConfig.selectedIconKey,
                            customIconBase64 = appConfig.customIconBase64,
                            mukhoshLogoBase64 = appConfig.mukhoshLogoBase64,
                            onReviewCadetAndAssignBadges = { targetUid, approve, primaryBadge, badges, note, forceAdmin ->
                                viewModel.reviewCadetAndAssignBadges(
                                    targetUserId = targetUid,
                                    approve = approve,
                                    primaryBadge = primaryBadge,
                                    badges = badges,
                                    reviewerNote = note,
                                    forceAdminAuthority = forceAdmin
                                )
                            },
                            onSelectAppIcon = { iconKey ->
                                viewModel.selectAppIconAsAdmin(iconKey) { updatedKey ->
                                    AppIconManager.applyDeviceLauncherIconAlias(context, updatedKey)
                                }
                            },
                            onUploadCustomGalleryAppIcon = { encodedBase64 ->
                                viewModel.uploadCustomGalleryAppIconAsAdmin(encodedBase64)
                            },
                            onUploadMukhoshLogo = { encodedLogo ->
                                viewModel.uploadMukhoshLogo(encodedLogo)
                            },
                            onLockSecretCommandTab = {
                                viewModel.lockSecretCommandTab()
                                currentTab = GeajaoTab.FEED
                            }
                        )

                        GeajaoTab.PROFILE -> ProfileScreen(
                            profile = currentProfile,
                            mukhoshLogoBase64 = appConfig.mukhoshLogoBase64,
                            onUploadMukhoshLogo = { encodedLogo ->
                                viewModel.uploadMukhoshLogo(encodedLogo)
                            },
                            onSaveProfile = { name, number, batch, college, bio, idCardBase64, note, cadetStatus ->
                                viewModel.submitProfileAndIdCard(
                                    displayName = name,
                                    cadetNumber = number,
                                    batchYear = batch,
                                    cadetCollege = college,
                                    bio = bio,
                                    idCardImageBase64 = idCardBase64,
                                    idCardNote = note,
                                    cadetStatusBadge = cadetStatus
                                )
                            },
                            onSignOut = {
                                signOutCadet(
                                    credentialManager = credentialManager,
                                    onSignOutComplete = onSignedOut,
                                    scope = scope
                                )
                            },
                            onOpenUserGuide = onOpenUserGuide
                        )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecretCommandPasscodeDialog(
    onDismiss: () -> Unit,
    onSubmitPasscode: (String) -> Unit
) {
    var passcodeInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = "Auth Passcode",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Auth Passcode",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter your Moderator or Admin Auth Passcode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = passcodeInput,
                    onValueChange = { passcodeInput = it },
                    label = { Text("Auth Passcode") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("secret_command_passcode_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitPasscode(passcodeInput) },
                enabled = passcodeInput.isNotBlank(),
                modifier = Modifier.testTag("unlock_secret_command_confirm_button")
            ) {
                Text("Unlock Panel")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
