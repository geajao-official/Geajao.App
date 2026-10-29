package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.crypto.TacticalCryptoEngine
import com.example.data.model.CadetProfile
import com.example.data.model.FeedPost
import com.example.data.model.Friendship
import com.example.data.model.GeajaoConstants
import com.example.data.model.UiState
import com.example.ui.components.Base64ImagePreview
import com.example.ui.components.CadetAvatarCircle
import com.example.ui.components.CadetRoleBadgePill
import com.example.ui.components.VerificationStatusChip
import com.example.ui.theme.BrassGold
import com.example.ui.theme.CadetNavyDeep
import com.example.ui.theme.CrimsonHonor
import com.example.ui.theme.CyanTactical
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoveryFeedScreen(
    currentUserId: String,
    currentProfile: CadetProfile?,
    effectiveUserBadges: List<String> = emptyList(),
    allCadetsState: UiState<List<CadetProfile>>,
    friendshipsState: UiState<List<Friendship>>,
    feedPostsState: UiState<List<FeedPost>>,
    selectedFeedTab: String,
    onSelectFeedTab: (String) -> Unit,
    onPublishPost: (content: String, mediaBase64: String, category: String) -> Unit,
    onToggleSalute: (FeedPost) -> Unit,
    onRecordCommentOrShare: (FeedPost, Boolean) -> Unit,
    onSendFriendRequest: (CadetProfile) -> Unit,
    onAcceptFriendRequest: (Friendship) -> Unit,
    onRemoveOrDeclineFriend: (Friendship) -> Unit,
    onStartChatWithCadet: (CadetProfile) -> Unit,
    onStartCallWithCadet: (CadetProfile, String) -> Unit,
    onOpenFriendsPage: () -> Unit = {}
) {
    val context = LocalContext.current
    var postContent by remember { mutableStateOf("") }
    var postMediaBase64 by remember { mutableStateOf("") }
    var selectedPostCategory by remember { mutableStateOf(GeajaoConstants.FEED_TRENDING_TAGS.first()) }

    // Discovery & Filtering States
    var userSearchQuery by remember { mutableStateOf("") }
    var selectedCollegeFilter by remember { mutableStateOf("ALL") }
    var selectedTagFilter by remember { mutableStateOf("ALL") }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            TacticalCryptoEngine.encodeUriToCompactBase64(context, uri)?.let {
                postMediaBase64 = it
            }
        }
    }

    val friendships = (friendshipsState as? UiState.Success)?.data ?: emptyList()
    val acceptedFriendUids = remember(friendships, currentUserId) {
        friendships.filter { it.isAccepted }
            .map { it.otherUid(currentUserId) }
            .toSet()
    }
    val incomingRequests = remember(friendships, currentUserId) {
        friendships.filter { !it.isAccepted && it.receiverId == currentUserId }
    }

    val allCadets = (allCadetsState as? UiState.Success)?.data ?: emptyList()
    val cadetByUid = remember(allCadets) {
        allCadets.associateBy { it.userId.ifBlank { it.id } }
    }
    val otherCadets = remember(allCadets, currentUserId) {
        allCadets.filter { it.userId != currentUserId && it.id != currentUserId }
    }

    // Filtered Cadets by College Affiliation & Search Query
    val filteredDiscoveryCadets = remember(otherCadets, selectedCollegeFilter, userSearchQuery) {
        val q = userSearchQuery.trim().lowercase()
        otherCadets.filter { cadet ->
            val college = cadet.cadetCollege
            val matchesCollege = selectedCollegeFilter == "ALL" || college.equals(selectedCollegeFilter, ignoreCase = true)
            val matchesQuery = q.isBlank() ||
                cadet.displayName.lowercase().contains(q) ||
                cadet.cadetNumber.lowercase().contains(q) ||
                cadet.batchYear.lowercase().contains(q) ||
                college.lowercase().contains(q)
            matchesCollege && matchesQuery
        }
    }

    val myBadgesList = remember(currentProfile, effectiveUserBadges) {
        (effectiveUserBadges + (currentProfile?.allEffectiveBadges ?: emptyList()))
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf(GeajaoConstants.BADGE_CADET) }
    }
    val myCollege = currentProfile?.cadetCollege ?: "CCR"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("discovery_feed_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Spacious My Cadet Identity, Active Badges & Post Composer Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feed_my_identity_and_composer_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Row: Avatar + Name + Verification Chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            CadetAvatarCircle(
                                displayName = currentProfile?.displayName ?: "Cadet",
                                badge = myBadgesList.firstOrNull() ?: GeajaoConstants.BADGE_CADET,
                                size = 46.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentProfile?.displayName?.ifBlank { "Cadet Officer" } ?: "Cadet Officer",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = buildString {
                                        append("College: $myCollege")
                                        val num = currentProfile?.cadetNumber.orEmpty()
                                        if (num.isNotBlank()) append(" • #$num")
                                        val batch = currentProfile?.batchYear.orEmpty()
                                        if (batch.isNotBlank()) append(" • $batch")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        VerificationStatusChip(
                            status = currentProfile?.verificationStatus ?: GeajaoConstants.STATUS_APPROVED
                        )
                    }

                    // Dedicated Full-Width Wrapping Box for User's Badges (Never Truncated!)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feed_my_badges_banner"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MilitaryTech,
                                    contentDescription = "My Active Badges",
                                    tint = BrassGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "MY ACTIVE BADGES & COLLEGE AFFILIATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("feed_my_badges_row"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CadetRoleBadgePill(
                                    badge = currentProfile?.primaryBadge ?: GeajaoConstants.BADGE_CADET,
                                    additionalBadges = myBadgesList
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = CyanTactical.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, CyanTactical.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = "Cadet College",
                                            tint = CyanTactical,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = myCollege,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanTactical
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    // Post Composer Input
                    OutlinedTextField(
                        value = postContent,
                        onValueChange = { postContent = it },
                        placeholder = { Text("Share an update, tactical insight, or #tag with the Cadet Network...") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_content_input")
                    )

                    // Post Tag / Category Selector Row
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Select Post Tag: #$selectedPostCategory",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val composerTags = remember(myCollege) {
                                (GeajaoConstants.FEED_TRENDING_TAGS + myCollege).distinct()
                            }
                            composerTags.forEach { tag ->
                                FilterChip(
                                    selected = selectedPostCategory == tag,
                                    onClick = { selectedPostCategory = tag },
                                    label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("composer_tag_chip_${tag.lowercase()}")
                                )
                            }
                        }
                    }

                    if (postMediaBase64.isNotBlank()) {
                        Base64ImagePreview(
                            dataUri = postMediaBase64,
                            contentDescription = "Attached Post Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                mediaPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("attach_post_media_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (postMediaBase64.isBlank()) "Photo" else "Change Photo")
                        }

                        Button(
                            onClick = {
                                onPublishPost(postContent, postMediaBase64, selectedPostCategory)
                                postContent = ""
                                postMediaBase64 = ""
                            },
                            enabled = postContent.isNotBlank() || postMediaBase64.isNotBlank(),
                            modifier = Modifier.testTag("publish_post_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Post #$selectedPostCategory")
                        }
                    }
                }
            }
        }

        // 2. Incoming Friend Requests Banner (if any)
        if (incomingRequests.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "FRIEND REQUESTS (${incomingRequests.size})",
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
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldVerified),
                                        modifier = Modifier.testTag("accept_friend_${req.requesterId}")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Accept", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Accept")
                                    }
                                    OutlinedButton(
                                        onClick = { onRemoveOrDeclineFriend(req) },
                                        modifier = Modifier.testTag("decline_friend_${req.requesterId}")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Decline", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Cadet Discovery & Search by College Affiliation (CCR..JGCC)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feed_cadet_discovery_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DISCOVER CADETS BY COLLEGE AFFILIATION",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Search cadets by name, batch, or Cadet College (1) CCR .. 12) JGCC)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onOpenFriendsPage,
                            modifier = Modifier.testTag("open_friend_making_page_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("All Friends")
                        }
                    }

                    // Search Bar for Cadets & College Affiliation
                    OutlinedTextField(
                        value = userSearchQuery,
                        onValueChange = { userSearchQuery = it },
                        placeholder = { Text("Search users by name, college (e.g. CCR, BCC, FCC), or batch...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search Users")
                        },
                        trailingIcon = {
                            if (userSearchQuery.isNotBlank()) {
                                IconButton(onClick = { userSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feed_user_search_input")
                    )

                    // College Affiliation Filter Chips (ALL + 1) CCR .. 12) JGCC)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("feed_college_affiliation_row"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCollegeFilter == "ALL",
                            onClick = { selectedCollegeFilter = "ALL" },
                            label = { Text("All Colleges (${otherCadets.size})") },
                            modifier = Modifier.testTag("feed_college_filter_ALL")
                        )
                        GeajaoConstants.CADET_COLLEGES.forEachIndexed { idx, college ->
                            FilterChip(
                                selected = selectedCollegeFilter == college,
                                onClick = {
                                    selectedCollegeFilter = if (selectedCollegeFilter == college) "ALL" else college
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                label = { Text("${idx + 1}) $college") },
                                modifier = Modifier.testTag("feed_college_filter_$college")
                            )
                        }
                    }

                    if (filteredDiscoveryCadets.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = if (selectedCollegeFilter != "ALL" || userSearchQuery.isNotBlank()) {
                                    "No cadets matched filter [${if (selectedCollegeFilter == "ALL") userSearchQuery else selectedCollegeFilter}]. Try selecting 'All Colleges'."
                                } else {
                                    "No other registered cadets in the directory yet."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredDiscoveryCadets, key = { it.userId.ifBlank { it.id } }) { cadet ->
                                val targetUid = cadet.userId.ifBlank { cadet.id }
                                val existingFriendship = friendships.find {
                                    it.participantUids.contains(targetUid)
                                }
                                CadetFriendCard(
                                    currentUserId = currentUserId,
                                    cadet = cadet,
                                    friendship = existingFriendship,
                                    onAddFriend = { onSendFriendRequest(cadet) },
                                    onAcceptFriend = { existingFriendship?.let(onAcceptFriendRequest) },
                                    onChat = { onStartChatWithCadet(cadet) },
                                    onVoiceCall = { onStartCallWithCadet(cadet, "VOICE") },
                                    onVideoCall = { onStartCallWithCadet(cadet, "VIDEO") }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Discovery Feed Mode Tabs (Recent, Trending, Friends) + Tag Filter Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Feed Filter",
                                tint = BrassGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "DISCOVERY FEED • SORT & FILTER BY TAGS",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (selectedTagFilter != "ALL" || selectedCollegeFilter != "ALL") {
                            Text(
                                text = "Reset Filters",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanTactical,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        selectedTagFilter = "ALL"
                                        selectedCollegeFilter = "ALL"
                                    }
                                    .testTag("reset_feed_filters_button")
                            )
                        }
                    }

                    // Primary Feed Mode Tabs: Recent, Trending, Friends
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GeajaoConstants.FEED_TABS.forEach { tab ->
                            FilterChip(
                                selected = selectedFeedTab == tab,
                                onClick = { onSelectFeedTab(tab) },
                                leadingIcon = if (tab == "Trending") {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                label = {
                                    Text(
                                        text = if (tab == "Friends") "Friends (${acceptedFriendUids.size})" else tab,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("feed_tab_${tab.lowercase()}")
                            )
                        }
                    }

                    // Filter Content by Recent or Trending Tags
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("feed_trending_tags_row"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedTagFilter == "ALL",
                            onClick = { selectedTagFilter = "ALL" },
                            label = { Text("All Tags", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("feed_tag_filter_all")
                        )
                        GeajaoConstants.FEED_TRENDING_TAGS.forEach { tag ->
                            FilterChip(
                                selected = selectedTagFilter.equals(tag, ignoreCase = true),
                                onClick = {
                                    selectedTagFilter = if (selectedTagFilter.equals(tag, ignoreCase = true)) "ALL" else tag
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Tag,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                },
                                label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("feed_tag_filter_${tag.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // 5. Feed Posts List (Recent, Trending, Friends + Tag & College Filtering)
        when (feedPostsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = feedPostsState.message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            is UiState.Success -> {
                val baseSortedPosts = when (selectedFeedTab) {
                    "Trending" -> feedPostsState.data.sortedByDescending {
                        (it.likesCount * 3L) + (it.commentsCount * 4L) + (it.sharesCount * 5L) + it.viewsCount
                    }
                    "Friends" -> feedPostsState.data.filter { post ->
                        acceptedFriendUids.contains(post.authorId) || post.authorId == currentUserId
                    }
                    else -> feedPostsState.data.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }

                val displayedPosts = baseSortedPosts.filter { post ->
                    val authorProfile = cadetByUid[post.authorId]
                    val authorCollege = authorProfile?.cadetCollege
                        ?: GeajaoConstants.normalizeCadetCollege(post.authorBatch.substringBefore("•").trim())
                    val matchesCollege = selectedCollegeFilter == "ALL" ||
                        authorCollege.equals(selectedCollegeFilter, ignoreCase = true) ||
                        post.category.equals(selectedCollegeFilter, ignoreCase = true) ||
                        post.content.contains("#$selectedCollegeFilter", ignoreCase = true)

                    val matchesTag = selectedTagFilter == "ALL" ||
                        post.category.equals(selectedTagFilter, ignoreCase = true) ||
                        post.content.contains("#$selectedTagFilter", ignoreCase = true)

                    matchesCollege && matchesTag
                }

                if (displayedPosts.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = when {
                                        selectedTagFilter != "ALL" -> "No Posts Tagged #$selectedTagFilter Yet"
                                        selectedCollegeFilter != "ALL" -> "No Posts from $selectedCollegeFilter Yet"
                                        selectedFeedTab == "Friends" -> "No Friends Posts Yet"
                                        else -> "No Posts in $selectedFeedTab Yet"
                                    },
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = if (selectedFeedTab == "Friends") {
                                        "Open the Friend-Making Page to find Cadets by College and add Friends."
                                    } else {
                                        "Write a post above or reset tag/college filters to view all community updates."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (selectedFeedTab == "Friends") {
                                    Button(
                                        onClick = onOpenFriendsPage,
                                        modifier = Modifier.testTag("empty_friends_open_hub_button")
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Open Friend-Making Page")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(displayedPosts, key = { it.id }) { post ->
                        val authorProfile = if (post.authorId == currentUserId) {
                            currentProfile
                        } else {
                            cadetByUid[post.authorId]
                        }
                        val authorAllBadges = remember(post.authorId, post.authorBadge, authorProfile, myBadgesList, currentUserId) {
                            if (post.authorId == currentUserId) {
                                (myBadgesList + post.authorBadge.split(",")).map { it.trim() }.filter { it.isNotBlank() }.distinct()
                            } else {
                                (authorProfile?.allEffectiveBadges.orEmpty() + post.authorBadge.split(","))
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .distinct()
                            }
                        }
                        val authorCollege = authorProfile?.cadetCollege
                            ?: GeajaoConstants.normalizeCadetCollege(post.authorBatch.substringBefore("•").trim())
                        val isTopTrending = selectedFeedTab == "Trending" ||
                            ((post.likesCount * 3L) + (post.commentsCount * 4L) + (post.sharesCount * 5L) >= 6L)

                        FeedPostCard(
                            post = post,
                            authorBadges = authorAllBadges,
                            authorCollege = authorCollege,
                            isTrendingHighlight = isTopTrending,
                            currentUserId = currentUserId,
                            onSelectTag = { clickedTag ->
                                selectedTagFilter = clickedTag
                            },
                            onSalute = { onToggleSalute(post) },
                            onComment = { onRecordCommentOrShare(post, true) },
                            onShare = { onRecordCommentOrShare(post, false) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CadetFriendCard(
    currentUserId: String,
    cadet: CadetProfile,
    friendship: Friendship?,
    onAddFriend: () -> Unit,
    onAcceptFriend: () -> Unit,
    onChat: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    val isFriend = friendship?.isAccepted == true
    val isOutgoingPending = friendship != null && !friendship.isAccepted && friendship.requesterId == currentUserId
    val isIncomingPending = friendship != null && !friendship.isAccepted && friendship.receiverId == currentUserId
    val collegeName = cadet.cadetCollege

    Card(
        modifier = Modifier.width(250.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isFriend) EmeraldVerified.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CadetAvatarCircle(
                    displayName = cadet.displayName,
                    badge = cadet.primaryBadge,
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cadet.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$collegeName • ${cadet.batchYear.ifBlank { "Cadet" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanTactical,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            // Full-width wrapping FlowRow for Cadet's Role Badges so none are cut off
            CadetRoleBadgePill(
                badge = cadet.primaryBadge,
                additionalBadges = cadet.allEffectiveBadges,
                modifier = Modifier.fillMaxWidth()
            )

            when {
                isFriend -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onChat,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp).testTag("chat_friend_${cadet.userId}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Chat with Friend",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chat", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = onVoiceCall, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Voice Call",
                                tint = EmeraldVerified,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onVideoCall, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video Call",
                                tint = BrassGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                isIncomingPending -> {
                    Button(
                        onClick = onAcceptFriend,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldVerified),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Accept Friend", style = MaterialTheme.typography.labelSmall)
                    }
                }
                isOutgoingPending -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Request Sent", style = MaterialTheme.typography.labelSmall)
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = onAddFriend,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("add_friend_${cadet.userId}")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Friend", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedPostCard(
    post: FeedPost,
    authorBadges: List<String>,
    authorCollege: String,
    isTrendingHighlight: Boolean,
    currentUserId: String,
    onSelectTag: (String) -> Unit,
    onSalute: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit
) {
    val isLiked = post.likedByUids.contains(currentUserId)
    val dateStr = remember(post.createdAt) {
        val date = post.createdAt?.toDate()
        if (date != null) {
            SimpleDateFormat("MMM d • HH:mm", Locale.getDefault()).format(date)
        } else {
            "Just now"
        }
    }
    val tagLabel = post.category.ifBlank { "General" }.removePrefix("#")

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isTrendingHighlight) BrassGold.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Author Header Row (Avatar + Name + College/Batch + Date)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CadetAvatarCircle(
                    displayName = post.authorName,
                    badge = authorBadges.firstOrNull() ?: post.authorBadge
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = buildString {
                            if (authorCollege.isNotBlank() && !post.authorBatch.contains(authorCollege)) {
                                append("$authorCollege • ")
                            }
                            if (post.authorBatch.isNotBlank()) {
                                append("${post.authorBatch} • ")
                            }
                            append(dateStr)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isTrendingHighlight) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = BrassGold.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, BrassGold)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Trending",
                                tint = BrassGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "TRENDING",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrassGold
                            )
                        }
                    }
                }
            }

            // Dedicated Full-Width Wrapping Row for Author's Badges & Post Tag (Never Cut Off!)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CadetRoleBadgePill(
                    badge = post.authorBadge,
                    additionalBadges = authorBadges
                )

                if (authorCollege.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CyanTactical.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, CyanTactical.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "🏫 $authorCollege",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanTactical,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.clickable { onSelectTag(tagLabel) },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "#$tagLabel",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f)
            )

            if (post.mediaImageBase64.isNotBlank()) {
                Base64ImagePreview(
                    dataUri = post.mediaImageBase64,
                    contentDescription = "Post Media",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSalute() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Like Post",
                        tint = if (isLiked) BrassGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount} Salutes",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isLiked) BrassGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onComment() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = "Comments",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.commentsCount} Comments",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onShare() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.sharesCount} Shares",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Views",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.viewsCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
