package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.crypto.TacticalCryptoEngine
import com.example.data.model.CadetProfile
import com.example.data.model.GeajaoConstants
import com.example.data.model.SecretPanelMode
import com.example.data.model.UiState
import com.example.ui.components.Base64ImagePreview
import com.example.ui.components.CadetAvatarCircle
import com.example.ui.components.CadetRoleBadgePill
import com.example.ui.components.GeajaoAppIconEmblem
import com.example.ui.components.MadeByMukhoshBrandCard
import com.example.ui.components.VerificationStatusChip
import com.example.ui.theme.CrimsonHonor
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommandCenterScreen(
    allCadetsState: UiState<List<CadetProfile>>,
    secretPanelMode: SecretPanelMode,
    hasAdminAuthority: Boolean,
    hasModOrAdminAuthority: Boolean,
    selectedAppIconKey: String,
    customIconBase64: String,
    mukhoshLogoBase64: String = "",
    onReviewCadetAndAssignBadges: (
        targetUserId: String,
        approve: Boolean,
        primaryBadge: String,
        badges: List<String>,
        reviewerNote: String,
        forceAdminAuthority: Boolean
    ) -> Unit,
    onSelectAppIcon: (String) -> Unit,
    onUploadCustomGalleryAppIcon: (String) -> Unit,
    onUploadMukhoshLogo: (String) -> Unit = {},
    onLockSecretCommandTab: () -> Unit
) {
    val context = LocalContext.current
    val directory = (allCadetsState as? UiState.Success)?.data ?: emptyList()
    val pendingUsers = directory.filter { it.verificationStatus == GeajaoConstants.STATUS_PENDING }

    // If opened via passcode 5246 (Moderator Panel), enforce Moderator scope even if user has other roles;
    // If opened via passcode A54h (Admin Panel), grant full Admin scope.
    val isAdminPanelActive = secretPanelMode == SecretPanelMode.ADMIN || (secretPanelMode != SecretPanelMode.MODERATOR && hasAdminAuthority)
    val isModOrAdminActive = isAdminPanelActive || secretPanelMode == SecretPanelMode.MODERATOR || hasModOrAdminAuthority

    val galleryIconPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            TacticalCryptoEngine.encodeUriToCompactBase64(context, uri)?.let { encoded ->
                onUploadCustomGalleryAppIcon(encoded)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("command_center_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Panel Header Card (Moderator Panel 5246 vs Admin Panel A54h)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isAdminPanelActive) Icons.Default.AdminPanelSettings else Icons.Default.Security,
                                contentDescription = "Secret Panel",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = if (isAdminPanelActive) {
                                        "Admin Panel (Passcode A54h)"
                                    } else {
                                        "Moderator Panel (Passcode 5246)"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAdminPanelActive) {
                                        "FULL ADMIN AUTHORITY: All Badges + Gallery App Icon & MUKHOSH Logo"
                                    } else {
                                        "MODERATOR AUTHORITY: ID Approval + a) Cadet & b) Ex-cadet Badges"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onLockSecretCommandTab,
                            modifier = Modifier.testTag("lock_secret_command_tab_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock Panel")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock")
                        }
                    }

                    Text(
                        text = if (isAdminPanelActive) {
                            "• Inspect uploaded Cadet ID cards, approve or reject pending users.\n" +
                                "• Assign a) Cadet, b) Ex-cadet, c) Admin, and Moderator badges.\n" +
                                "• Upload a custom App Icon or change the MUKHOSH Logo from the device gallery."
                        } else {
                            "• Inspect uploaded Cadet ID cards, approve or reject pending users.\n" +
                                "• Assign a) Cadet and b) Ex-cadet badges."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Admin-Only App Icon Manager (Upload from Gallery + Presets)
        if (isAdminPanelActive) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_app_icon_selector_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "App Icon Manager",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "ADMIN APP ICON MANAGER (GALLERY UPLOAD)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Upload a custom App Icon from your gallery or choose an emblem below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Upload App Icon from Gallery Button
                        Button(
                            onClick = {
                                galleryIconPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_app_icon_from_gallery_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload App Icon from Gallery")
                        }

                        if (selectedAppIconKey == GeajaoConstants.ICON_CUSTOM_GALLERY && customIconBase64.isNotBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
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
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        GeajaoAppIconEmblem(
                                            iconKey = GeajaoConstants.ICON_CUSTOM_GALLERY,
                                            customIconBase64 = customIconBase64,
                                            size = 48.dp
                                        )
                                        Column {
                                            Text(
                                                text = "Custom Gallery App Icon",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Uploaded by Admin from Gallery",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("ACTIVE") },
                                        leadingIcon = {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Active")
                                        }
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GeajaoConstants.APP_ICON_OPTIONS.forEach { (iconKey, iconLabel) ->
                                val isSelected = selectedAppIconKey == iconKey
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectAppIcon(iconKey) }
                                        .testTag("select_app_icon_$iconKey"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    },
                                    border = if (isSelected) {
                                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        null
                                    }
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
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            GeajaoAppIconEmblem(
                                                iconKey = iconKey,
                                                size = 44.dp
                                            )
                                            Column {
                                                Text(
                                                    text = iconLabel,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Code: $iconKey",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            AssistChip(
                                                onClick = { },
                                                label = { Text("ACTIVE") },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Active Icon"
                                                    )
                                                }
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

        // Made By MUKHOSH Brand Tag & Logo Manager (Changeable in Admin Page)
        item {
            MadeByMukhoshBrandCard(
                mukhoshLogoBase64 = mukhoshLogoBase64,
                onUploadMukhoshLogo = onUploadMukhoshLogo,
                isAdminEditable = isAdminPanelActive
            )
        }

        // Pending ID Card Verification Queue
        item {
            Text(
                text = "PENDING ID CARD VERIFICATIONS (${pendingUsers.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = JetBrainsMonoFontFamily
            )
        }

        if (pendingUsers.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Queue Clear",
                            tint = EmeraldVerified
                        )
                        Text(
                            text = "All submitted Cadet ID cards have been reviewed.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(pendingUsers, key = { "pending_${it.userId.ifBlank { it.id }}" }) { user ->
                CadetReviewAndBadgeCard(
                    user = user,
                    hasAdminAuthority = isAdminPanelActive,
                    hasModOrAdminAuthority = isModOrAdminActive,
                    onReviewCadetAndAssignBadges = onReviewCadetAndAssignBadges
                )
            }
        }

        // Full Corps Roster & Role Badge Assignment
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "MEMBER ROSTER & ROLE BADGE REGISTRY (${directory.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = JetBrainsMonoFontFamily
            )
        }

        items(directory, key = { "roster_${it.userId.ifBlank { it.id }}" }) { user ->
            CadetReviewAndBadgeCard(
                user = user,
                hasAdminAuthority = isAdminPanelActive,
                hasModOrAdminAuthority = isModOrAdminActive,
                onReviewCadetAndAssignBadges = onReviewCadetAndAssignBadges
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CadetReviewAndBadgeCard(
    user: CadetProfile,
    hasAdminAuthority: Boolean,
    hasModOrAdminAuthority: Boolean,
    onReviewCadetAndAssignBadges: (
        targetUserId: String,
        approve: Boolean,
        primaryBadge: String,
        badges: List<String>,
        reviewerNote: String,
        forceAdminAuthority: Boolean
    ) -> Unit
) {
    val targetUid = user.userId.ifBlank { user.id }
    val effectiveBadges = user.allEffectiveBadges

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("command_user_card_$targetUid"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CadetAvatarCircle(
                    displayName = user.displayName,
                    badge = user.primaryBadge,
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        verticalArrangement = Arrangement.Center,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = user.displayName.ifBlank { "Cadet Applicant" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        CadetRoleBadgePill(
                            badge = user.primaryBadge,
                            additionalBadges = user.badges
                        )
                    }
                    Text(
                        text = "${user.cadetNumber.ifBlank { "No Service #" }} • ${user.batchYear.ifBlank { "Unassigned Batch" }} • College: ${user.cadetCollege}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                VerificationStatusChip(status = user.verificationStatus)
            }

            if (user.idCardImageBase64.isNotBlank()) {
                Text(
                    text = "Submitted Cadet ID Card Evidence:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Base64ImagePreview(
                    dataUri = user.idCardImageBase64,
                    contentDescription = "Uploaded Cadet ID Card for ${user.displayName}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "No ID card image uploaded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (hasModOrAdminAuthority) {
                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val (approvePrimary, approveBadges) = GeajaoConstants.applyCadetStatusBadge(
                        existingPrimary = user.primaryBadge,
                        existingBadges = user.badges,
                        newCadetStatus = user.cadetStatusBadge
                    )
                    Button(
                        onClick = {
                            onReviewCadetAndAssignBadges(
                                targetUid,
                                true,
                                approvePrimary,
                                approveBadges,
                                "Approved",
                                hasAdminAuthority
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldVerified),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("approve_user_$targetUid")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Approve")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve ID")
                    }

                    OutlinedButton(
                        onClick = {
                            onReviewCadetAndAssignBadges(
                                targetUid,
                                false,
                                GeajaoConstants.BADGE_UNVERIFIED,
                                emptyList(),
                                "Rejected",
                                hasAdminAuthority
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonHonor),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reject_user_$targetUid")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Reject")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject")
                    }
                }

                Text(
                    text = if (hasAdminAuthority) {
                        "ASSIGN BADGES (Anyone can be Admin/Moderator AND also a) Cadet or b) Ex-cadet)"
                    } else {
                        "ASSIGN CADET STATUS (a) Cadet or b) Ex-cadet — preserves Admin/Moderator role)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // a) Cadet - available to both Moderator and Admin, preserves Admin/Moderator
                    val isCadetActive = effectiveBadges.contains(GeajaoConstants.BADGE_CADET)
                    AssistChip(
                        onClick = {
                            val (newPrimary, newBadges) = GeajaoConstants.applyCadetStatusBadge(
                                existingPrimary = user.primaryBadge,
                                existingBadges = user.badges,
                                newCadetStatus = GeajaoConstants.BADGE_CADET
                            )
                            onReviewCadetAndAssignBadges(
                                targetUid,
                                true,
                                newPrimary,
                                newBadges,
                                "Assigned Cadet badge",
                                hasAdminAuthority
                            )
                        },
                        label = { Text("a) Cadet") },
                        leadingIcon = {
                            Icon(Icons.Default.MilitaryTech, contentDescription = "Cadet Badge")
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isCadetActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        modifier = Modifier.testTag("assign_badge_cadet_$targetUid")
                    )

                    // b) Ex-cadet - available to both Moderator and Admin, preserves Admin/Moderator
                    val isExCadetActive = effectiveBadges.contains(GeajaoConstants.BADGE_EX_CADET)
                    AssistChip(
                        onClick = {
                            val (newPrimary, newBadges) = GeajaoConstants.applyCadetStatusBadge(
                                existingPrimary = user.primaryBadge,
                                existingBadges = user.badges,
                                newCadetStatus = GeajaoConstants.BADGE_EX_CADET
                            )
                            onReviewCadetAndAssignBadges(
                                targetUid,
                                true,
                                newPrimary,
                                newBadges,
                                "Assigned Ex-cadet badge",
                                hasAdminAuthority
                            )
                        },
                        label = { Text("b) Ex-cadet") },
                        leadingIcon = {
                            Icon(Icons.Default.MilitaryTech, contentDescription = "Ex-cadet Badge")
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isExCadetActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        modifier = Modifier.testTag("assign_badge_ex_cadet_$targetUid")
                    )

                    // c) Admin & Moderator - available in Admin Panel (A54h), preserves Cadet or Ex-cadet
                    if (hasAdminAuthority) {
                        val isModActive = effectiveBadges.contains(GeajaoConstants.BADGE_MODERATOR)
                        AssistChip(
                            onClick = {
                                val (newPrimary, newBadges) = GeajaoConstants.toggleAuthorityBadge(
                                    existingPrimary = user.primaryBadge,
                                    existingBadges = user.badges,
                                    authorityBadge = GeajaoConstants.BADGE_MODERATOR
                                )
                                onReviewCadetAndAssignBadges(
                                    targetUid,
                                    true,
                                    newPrimary,
                                    newBadges,
                                    "Updated Moderator & ${user.cadetStatusBadge} badges",
                                    true
                                )
                            },
                            label = { Text("Moderator") },
                            leadingIcon = {
                                Icon(Icons.Default.Security, contentDescription = "Moderator Badge")
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isModActive) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            modifier = Modifier.testTag("assign_badge_moderator_$targetUid")
                        )

                        val isAdminActive = effectiveBadges.contains(GeajaoConstants.BADGE_ADMIN)
                        AssistChip(
                            onClick = {
                                val (newPrimary, newBadges) = GeajaoConstants.toggleAuthorityBadge(
                                    existingPrimary = user.primaryBadge,
                                    existingBadges = user.badges,
                                    authorityBadge = GeajaoConstants.BADGE_ADMIN
                                )
                                onReviewCadetAndAssignBadges(
                                    targetUid,
                                    true,
                                    newPrimary,
                                    newBadges,
                                    "Updated Admin & ${user.cadetStatusBadge} badges",
                                    true
                                )
                            },
                            label = { Text("c) Admin") },
                            leadingIcon = {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Badge")
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isAdminActive) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            modifier = Modifier.testTag("assign_badge_admin_$targetUid")
                        )
                    }
                }
            }
        }
    }
}
