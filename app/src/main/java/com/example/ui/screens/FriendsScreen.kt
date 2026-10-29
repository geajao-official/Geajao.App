package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CadetProfile
import com.example.data.model.Friendship
import com.example.data.model.GeajaoConstants
import com.example.data.model.UiState
import com.example.ui.components.CadetAvatarCircle
import com.example.ui.components.CadetRoleBadgePill
import com.example.ui.components.VerificationStatusChip
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.JetBrainsMonoFontFamily

private enum class FriendsSectionTab(val label: String) {
    FIND_FRIENDS("Find Friends"),
    REQUESTS("Requests"),
    MY_FRIENDS("My Friends")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FriendsScreen(
    currentUserId: String,
    allCadetsState: UiState<List<CadetProfile>>,
    friendshipsState: UiState<List<Friendship>>,
    onSendFriendRequest: (CadetProfile) -> Unit,
    onAcceptFriendRequest: (Friendship) -> Unit,
    onRemoveOrDeclineFriend: (Friendship) -> Unit,
    onStartChatWithCadet: (CadetProfile) -> Unit,
    onStartCallWithCadet: (CadetProfile, String) -> Unit
) {
    var selectedSection by remember { mutableStateOf(FriendsSectionTab.FIND_FRIENDS) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCollegeFilter by remember { mutableStateOf("All") }

    val friendships = (friendshipsState as? UiState.Success)?.data ?: emptyList()
    val allOtherCadets = remember(allCadetsState, currentUserId) {
        ((allCadetsState as? UiState.Success)?.data ?: emptyList())
            .filter { (it.userId.ifBlank { it.id }) != currentUserId }
    }

    val incomingRequests = remember(friendships, currentUserId) {
        friendships.filter { !it.isAccepted && it.receiverId == currentUserId }
    }
    val outgoingRequests = remember(friendships, currentUserId) {
        friendships.filter { !it.isAccepted && it.requesterId == currentUserId }
    }
    val acceptedFriendUids = remember(friendships, currentUserId) {
        friendships.filter { it.isAccepted }
            .map { it.otherUid(currentUserId) }
            .toSet()
    }
    val myFriendsList = remember(allOtherCadets, acceptedFriendUids) {
        allOtherCadets.filter { acceptedFriendUids.contains(it.userId.ifBlank { it.id }) }
    }

    val filteredDiscoverCadets = remember(allOtherCadets, searchQuery, selectedCollegeFilter) {
        allOtherCadets.filter { cadet ->
            val collegeMatch = selectedCollegeFilter == "All" ||
                cadet.cadetCollege.equals(selectedCollegeFilter, ignoreCase = true)
            val q = searchQuery.trim()
            val queryMatch = q.isBlank() ||
                cadet.displayName.contains(q, ignoreCase = true) ||
                cadet.cadetNumber.contains(q, ignoreCase = true) ||
                cadet.batchYear.contains(q, ignoreCase = true) ||
                cadet.cadetCollege.contains(q, ignoreCase = true) ||
                cadet.primaryBadge.contains(q, ignoreCase = true)
            collegeMatch && queryMatch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("friends_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Friend Making Hub",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Column {
                                Text(
                                    text = "Cadet Friend-Making Hub",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Find Cadets by College, send Friend Requests, and start encrypted chats",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Section Tabs: Find Friends | Requests | My Friends
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FriendsSectionTab.entries.forEach { tab ->
                            val badgeSuffix = when (tab) {
                                FriendsSectionTab.FIND_FRIENDS -> " (${allOtherCadets.size})"
                                FriendsSectionTab.REQUESTS -> " (${incomingRequests.size + outgoingRequests.size})"
                                FriendsSectionTab.MY_FRIENDS -> " (${myFriendsList.size})"
                            }
                            FilterChip(
                                selected = selectedSection == tab,
                                onClick = { selectedSection = tab },
                                label = {
                                    Text(
                                        text = tab.label + badgeSuffix,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("friends_subtab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Urgent Incoming Friend Requests Banner (Always visible if there are incoming requests)
        if (incomingRequests.isNotEmpty() && selectedSection != FriendsSectionTab.REQUESTS) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "INCOMING FRIEND REQUESTS (${incomingRequests.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.primary
                        )
                        incomingRequests.forEach { req ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = req.requesterName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Wants to be your Friend on Geajao",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onAcceptFriendRequest(req) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldVerified),
                                        modifier = Modifier.testTag("friends_accept_${req.requesterId}")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Accept", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Accept")
                                    }
                                    OutlinedButton(
                                        onClick = { onRemoveOrDeclineFriend(req) },
                                        modifier = Modifier.testTag("friends_decline_${req.requesterId}")
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

        when (selectedSection) {
            FriendsSectionTab.FIND_FRIENDS -> {
                // Search & College Filter Bar
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Search Cadets")
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear Search")
                                        }
                                    }
                                },
                                placeholder = {
                                    Text("Search by Name, Cadet #, Batch, or College (CCR..JGCC)")
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("friends_search_input")
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "Filter by Cadet College",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Filter by Cadet College:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedCollegeFilter == "All",
                                        onClick = { selectedCollegeFilter = "All" },
                                        label = { Text("All Colleges") },
                                        modifier = Modifier.testTag("filter_college_All")
                                    )
                                }
                                items(GeajaoConstants.CADET_COLLEGES) { college ->
                                    FilterChip(
                                        selected = selectedCollegeFilter == college,
                                        onClick = { selectedCollegeFilter = college },
                                        label = { Text(college) },
                                        modifier = Modifier.testTag("filter_college_$college")
                                    )
                                }
                            }
                        }
                    }
                }

                when (allCadetsState) {
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
                                    text = allCadetsState.message,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    is UiState.Success -> {
                        if (filteredDiscoverCadets.isEmpty()) {
                            item {
                                EmptyFriendsStateCard(
                                    title = if (allOtherCadets.isEmpty()) {
                                        "No Other Cadets Registered Yet"
                                    } else {
                                        "No Cadets Match Your Filter"
                                    },
                                    subtitle = if (allOtherCadets.isEmpty()) {
                                        "When other Cadets register on Geajao, they will appear here so you can send them a Friend Request."
                                    } else {
                                        "Try clearing your search query or selecting 'All Colleges' above."
                                    }
                                )
                            }
                        } else {
                            items(
                                items = filteredDiscoverCadets,
                                key = { "discover_${it.userId.ifBlank { it.id }}" }
                            ) { cadet ->
                                val targetUid = cadet.userId.ifBlank { cadet.id }
                                val existingFriendship = friendships.find {
                                    it.participantUids.contains(targetUid)
                                }
                                CadetDirectoryFriendRowCard(
                                    currentUserId = currentUserId,
                                    cadet = cadet,
                                    friendship = existingFriendship,
                                    onSendFriendRequest = { onSendFriendRequest(cadet) },
                                    onAcceptFriendRequest = { existingFriendship?.let(onAcceptFriendRequest) },
                                    onRemoveOrDeclineFriend = { existingFriendship?.let(onRemoveOrDeclineFriend) },
                                    onStartChat = { onStartChatWithCadet(cadet) },
                                    onStartVoiceCall = { onStartCallWithCadet(cadet, "VOICE") },
                                    onStartVideoCall = { onStartCallWithCadet(cadet, "VIDEO") }
                                )
                            }
                        }
                    }
                }
            }

            FriendsSectionTab.REQUESTS -> {
                item {
                    Text(
                        text = "INCOMING FRIEND REQUESTS (${incomingRequests.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (incomingRequests.isEmpty()) {
                    item {
                        EmptyFriendsStateCard(
                            title = "No Incoming Friend Requests",
                            subtitle = "When another Cadet sends you a Friend Request, you can accept or decline it here."
                        )
                    }
                } else {
                    items(incomingRequests, key = { "inc_${it.id}" }) { req ->
                        val senderCadet = allOtherCadets.find {
                            (it.userId.ifBlank { it.id }) == req.requesterId
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
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
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    CadetAvatarCircle(
                                        displayName = req.requesterName,
                                        badge = senderCadet?.primaryBadge ?: GeajaoConstants.BADGE_CADET,
                                        size = 46.dp
                                    )
                                    Column {
                                        Text(
                                            text = req.requesterName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (senderCadet != null) {
                                                "${senderCadet.cadetNumber.ifBlank { "Cadet" }} • ${senderCadet.batchYear} • College: ${senderCadet.cadetCollege}"
                                            } else {
                                                "Incoming Friend Request"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onAcceptFriendRequest(req) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldVerified),
                                        modifier = Modifier.testTag("req_tab_accept_${req.requesterId}")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Accept", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Accept")
                                    }
                                    OutlinedButton(
                                        onClick = { onRemoveOrDeclineFriend(req) },
                                        modifier = Modifier.testTag("req_tab_decline_${req.requesterId}")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Decline", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "SENT FRIEND REQUESTS (${outgoingRequests.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (outgoingRequests.isEmpty()) {
                    item {
                        EmptyFriendsStateCard(
                            title = "No Pending Sent Requests",
                            subtitle = "Switch to 'Find Friends' to browse Cadets by College and send Friend Requests."
                        )
                    }
                } else {
                    items(outgoingRequests, key = { "out_${it.id}" }) { req ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassEmpty,
                                        contentDescription = "Pending",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = req.receiverName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Awaiting response...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { onRemoveOrDeclineFriend(req) },
                                    modifier = Modifier.testTag("cancel_sent_req_${req.receiverId}")
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }

            FriendsSectionTab.MY_FRIENDS -> {
                if (myFriendsList.isEmpty()) {
                    item {
                        EmptyFriendsStateCard(
                            title = "You Have No Accepted Friends Yet",
                            subtitle = "Use the 'Find Friends' tab above to connect with Cadets from CCR, BCC, PCC, RCC, MCC, FCC, JCC, SCC, CCC, FGCC, MGCC, or JGCC. Only Friends can chat with each other!"
                        )
                    }
                } else {
                    items(myFriendsList, key = { "myfriend_${it.userId.ifBlank { it.id }}" }) { cadet ->
                        val targetUid = cadet.userId.ifBlank { cadet.id }
                        val existingFriendship = friendships.find {
                            it.participantUids.contains(targetUid)
                        }
                        CadetDirectoryFriendRowCard(
                            currentUserId = currentUserId,
                            cadet = cadet,
                            friendship = existingFriendship,
                            onSendFriendRequest = { onSendFriendRequest(cadet) },
                            onAcceptFriendRequest = { existingFriendship?.let(onAcceptFriendRequest) },
                            onRemoveOrDeclineFriend = { existingFriendship?.let(onRemoveOrDeclineFriend) },
                            onStartChat = { onStartChatWithCadet(cadet) },
                            onStartVoiceCall = { onStartCallWithCadet(cadet, "VOICE") },
                            onStartVideoCall = { onStartCallWithCadet(cadet, "VIDEO") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CadetDirectoryFriendRowCard(
    currentUserId: String,
    cadet: CadetProfile,
    friendship: Friendship?,
    onSendFriendRequest: () -> Unit,
    onAcceptFriendRequest: () -> Unit,
    onRemoveOrDeclineFriend: () -> Unit,
    onStartChat: () -> Unit,
    onStartVoiceCall: () -> Unit,
    onStartVideoCall: () -> Unit
) {
    val targetUid = cadet.userId.ifBlank { cadet.id }
    val isFriend = friendship?.isAccepted == true
    val isOutgoingPending = friendship != null && !friendship.isAccepted && friendship.requesterId == currentUserId
    val isIncomingPending = friendship != null && !friendship.isAccepted && friendship.receiverId == currentUserId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("friend_directory_card_$targetUid"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isFriend) EmeraldVerified.copy(alpha = 0.75f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    CadetAvatarCircle(
                        displayName = cadet.displayName,
                        badge = cadet.primaryBadge,
                        size = 50.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = cadet.displayName.ifBlank { "Cadet" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            CadetRoleBadgePill(
                                badge = cadet.primaryBadge,
                                additionalBadges = cadet.badges
                            )
                        }
                        Text(
                            text = "College: ${cadet.cadetCollege} • ${cadet.batchYear.ifBlank { "Cadet" }} • #${cadet.cadetNumber.ifBlank { "N/A" }}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (cadet.bio.isNotBlank()) {
                            Text(
                                text = cadet.bio,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                VerificationStatusChip(status = cadet.verificationStatus)
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                when {
                    isFriend -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onStartChat,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("friend_card_chat_$targetUid")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat")
                            }
                            IconButton(
                                onClick = onStartVoiceCall,
                                modifier = Modifier.testTag("friend_card_voice_$targetUid")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Voice Call",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = onStartVideoCall,
                                modifier = Modifier.testTag("friend_card_video_$targetUid")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Video Call",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = onRemoveOrDeclineFriend,
                                modifier = Modifier.testTag("friend_card_remove_$targetUid")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonRemove,
                                    contentDescription = "Remove Friend",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    isIncomingPending -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onAcceptFriendRequest,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldVerified),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("friend_card_accept_$targetUid")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Accept Friend Request")
                            }
                            OutlinedButton(
                                onClick = onRemoveOrDeclineFriend,
                                modifier = Modifier.testTag("friend_card_decline_$targetUid")
                            ) {
                                Text("Decline")
                            }
                        }
                    }

                    isOutgoingPending -> {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = "Request Sent",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Friend Request Sent",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = onRemoveOrDeclineFriend,
                                modifier = Modifier.testTag("friend_card_cancel_$targetUid")
                            ) {
                                Text("Cancel Request")
                            }
                        }
                    }

                    else -> {
                        Button(
                            onClick = onSendFriendRequest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("friend_card_add_$targetUid")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Friend")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFriendsStateCard(
    title: String,
    subtitle: String
) {
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
