package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Switch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.crypto.TacticalCryptoEngine
import com.example.data.model.CadetProfile
import com.example.data.model.GeajaoConstants
import com.example.ui.components.Base64ImagePreview
import com.example.ui.components.CadetAvatarCircle
import com.example.ui.components.CadetRoleBadgePill
import com.example.ui.components.MadeByMukhoshBadge
import com.example.ui.components.MadeByMukhoshBrandCard
import com.example.ui.components.OfficialContactAndGuideCard
import com.example.ui.components.VerificationStatusChip
import com.example.ui.theme.ThemePreferences

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profile: CadetProfile?,
    mukhoshLogoBase64: String = "",
    onUploadMukhoshLogo: (String) -> Unit = {},
    onSaveProfile: (
        displayName: String,
        cadetNumber: String,
        batchYear: String,
        cadetCollege: String,
        bio: String,
        idCardBase64: String,
        idCardNote: String,
        cadetStatusBadge: String
    ) -> Unit,
    onSignOut: () -> Unit,
    onOpenUserGuide: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDarkMode by ThemePreferences.isDarkModeFlow.collectAsStateWithLifecycle()

    var displayName by remember(profile?.displayName) {
        mutableStateOf(profile?.displayName.orEmpty())
    }
    var cadetNumber by remember(profile?.cadetNumber) {
        mutableStateOf(profile?.cadetNumber.orEmpty())
    }
    var batchYear by remember(profile?.batchYear) {
        mutableStateOf(profile?.batchYear.orEmpty())
    }
    var selectedCollege by remember(profile?.cadetCollege) {
        mutableStateOf(profile?.cadetCollege ?: GeajaoConstants.CADET_COLLEGES.first())
    }
    var selectedCadetStatus by remember(profile?.primaryBadge, profile?.badges) {
        mutableStateOf(profile?.cadetStatusBadge ?: GeajaoConstants.BADGE_CADET)
    }
    var bio by remember(profile?.bio) {
        mutableStateOf(profile?.bio.orEmpty())
    }
    var idCardNote by remember(profile?.idCardNote) {
        mutableStateOf(profile?.idCardNote.orEmpty())
    }
    var idCardBase64 by remember(profile?.idCardImageBase64) {
        mutableStateOf(profile?.idCardImageBase64.orEmpty())
    }

    val idCardPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            TacticalCryptoEngine.encodeUriToCompactBase64(context, uri)?.let { encoded ->
                idCardBase64 = encoded
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("profile_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CadetAvatarCircle(
                        displayName = displayName.ifBlank { "Cadet" },
                        badge = profile?.primaryBadge ?: "Cadet",
                        size = 84.dp
                    )
                    Text(
                        text = displayName.ifBlank { "Unnamed Cadet" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CadetRoleBadgePill(
                            badge = profile?.primaryBadge ?: "Cadet",
                            additionalBadges = profile?.badges ?: emptyList()
                        )
                        VerificationStatusChip(status = profile?.verificationStatus ?: "PENDING")
                    }
                    MadeByMukhoshBadge(
                        mukhoshLogoBase64 = mukhoshLogoBase64
                    )
                }
            }
        }

        // User Settings: Global Dark Mode / Light Mode Theme Toggle
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_settings_theme_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Theme Mode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "User Settings • Theme Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isDarkMode) {
                                        "Active: Dark Mode (Tactical Navy UI)"
                                    } else {
                                        "Active: Light Mode (Academy Parchment UI)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { enabled ->
                                ThemePreferences.setDarkMode(context, enabled)
                            },
                            modifier = Modifier.testTag("dark_mode_toggle_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = isDarkMode,
                            onClick = { ThemePreferences.setDarkMode(context, true) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = "Dark Mode",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Dark Mode") },
                            modifier = Modifier.testTag("theme_chip_dark")
                        )
                        FilterChip(
                            selected = !isDarkMode,
                            onClick = { ThemePreferences.setDarkMode(context, false) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LightMode,
                                    contentDescription = "Light Mode",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Light Mode") },
                            modifier = Modifier.testTag("theme_chip_light")
                        )
                    }
                }
            }
        }

        // Made By MUKHOSH Brand Tag (Logo changeable in Admin Page)
        item {
            MadeByMukhoshBrandCard(
                mukhoshLogoBase64 = mukhoshLogoBase64,
                isAdminEditable = false
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Customize Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Personal Profile Customization",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = cadetNumber,
                            onValueChange = { cadetNumber = it },
                            label = { Text("Cadet Number") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_number_input")
                        )
                        OutlinedTextField(
                            value = batchYear,
                            onValueChange = { batchYear = it },
                            label = { Text("Batch / Intake") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_batch_input")
                        )
                    }

                    // Cadet Status Selector (a) Cadet or b) Ex-cadet) — works alongside Admin / Moderator
                    Text(
                        text = "Cadet Status (Anyone can be Admin/Moderator AND also Cadet or Ex-cadet): $selectedCadetStatus",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedCadetStatus == GeajaoConstants.BADGE_CADET,
                            onClick = { selectedCadetStatus = GeajaoConstants.BADGE_CADET },
                            label = { Text("a) Cadet") },
                            modifier = Modifier.testTag("profile_status_chip_cadet")
                        )
                        FilterChip(
                            selected = selectedCadetStatus == GeajaoConstants.BADGE_EX_CADET,
                            onClick = { selectedCadetStatus = GeajaoConstants.BADGE_EX_CADET },
                            label = { Text("b) Ex-cadet") },
                            modifier = Modifier.testTag("profile_status_chip_ex_cadet")
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Cadet College",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Cadet College: $selectedCollege",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_college_selector"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GeajaoConstants.CADET_COLLEGES.forEachIndexed { index, college ->
                            FilterChip(
                                selected = selectedCollege == college,
                                onClick = { selectedCollege = college },
                                label = { Text("${index + 1}) $college") },
                                modifier = Modifier.testTag("profile_college_chip_$college")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_bio_input")
                    )

                    HorizontalDivider()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = "Cadet ID Card",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Uploaded Cadet ID Card",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Base64ImagePreview(
                        dataUri = idCardBase64,
                        contentDescription = "Cadet ID Card",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            idCardPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_update_id_card_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Upload ID Card")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (idCardBase64.isBlank()) "Upload ID Card Photo" else "Replace ID Card Photo")
                    }

                    Button(
                        onClick = {
                            onSaveProfile(
                                displayName,
                                cadetNumber,
                                batchYear,
                                selectedCollege,
                                bio,
                                idCardBase64,
                                idCardNote,
                                selectedCadetStatus
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_save_button")
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }

        item {
            OfficialContactAndGuideCard(
                onOpenUserGuide = onOpenUserGuide
            )
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}
