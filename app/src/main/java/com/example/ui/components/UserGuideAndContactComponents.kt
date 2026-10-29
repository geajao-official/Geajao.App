package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.GeajaoConstants
import com.example.ui.theme.BrassGold
import com.example.ui.theme.CadetNavyDeep
import com.example.ui.theme.CyanTactical
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.JetBrainsMonoFontFamily

object FirstTimeUserGuidePreferences {
    private const val PREFS_NAME = "geajao_user_guide_prefs"
    private const val KEY_HAS_SEEN_GUIDE = "has_seen_first_time_user_guide_v1"

    fun shouldShowFirstTimeGuide(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_HAS_SEEN_GUIDE, false)
    }

    fun markFirstTimeGuideCompleted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAS_SEEN_GUIDE, true).apply()
    }
}

private data class UserGuideStep(
    val stepNumber: Int,
    val badgeLabel: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val bulletPoints: List<String>
)

private val GEAJAO_GUIDE_STEPS = listOf(
    UserGuideStep(
        stepNumber = 1,
        badgeLabel = "STEP 1 • WELCOME & COLLEGES",
        title = "Welcome to Geajao Cadet Network",
        subtitle = "The exclusive social & encrypted communications hub for Cadets and Ex-Cadets across all 12 Cadet Colleges.",
        icon = Icons.Default.School,
        accentColor = BrassGold,
        bulletPoints = listOf(
            "Supports all 12 Cadet Colleges: 1) CCR, 2) BCC, 3) PCC, 4) RCC, 5) MCC, 6) FCC, 7) JCC, 8) SCC, 9) CCC, 10) FGCC, 11) MGCC, 12) JGCC.",
            "Choose your Cadet Status as either a) Cadet or b) Ex-cadet during registration or in your Profile.",
            "Anyone can hold an Admin or Moderator role alongside their Cadet or Ex-cadet badge."
        )
    ),
    UserGuideStep(
        stepNumber = 2,
        badgeLabel = "STEP 2 • ID CARD VERIFICATION",
        title = "Mandatory ID Card Verification Gate",
        subtitle = "Every member publishes their Cadet ID Card for verification before entering the main community.",
        icon = Icons.Default.Badge,
        accentColor = EmeraldVerified,
        bulletPoints = listOf(
            "Upload a photo of your official Cadet ID Card from your gallery or generate an official Digital Cadet ID Card.",
            "Until your ID Card is published and approved, the app stays on the Verifying Page.",
            "Admin & Moderator Shortcut: Tap the Geajao Emblem 6 times to open the Auth Passcode prompt — enter 5246 for Moderator Panel or A54h for Admin Panel."
        )
    ),
    UserGuideStep(
        stepNumber = 3,
        badgeLabel = "STEP 3 • COMMUNITY & FRIENDS",
        title = "Feed, Friend-Making Hub & Badges",
        subtitle = "Connect with batchmates, filter by Cadet College, and share updates.",
        icon = Icons.Default.AdminPanelSettings,
        accentColor = CyanTactical,
        bulletPoints = listOf(
            "Browse the Community Feed by Recent, Trending, or Friends posts, and react with Salutes.",
            "Use the Friends tab (Friend-Making Hub) to search Cadets by name, batch, or Cadet College (CCR..JGCC) and send Friend Requests.",
            "Real-time push notifications alert you immediately when you receive a new Friend Request or Private Message."
        )
    ),
    UserGuideStep(
        stepNumber = 4,
        badgeLabel = "STEP 4 • ENCRYPTED CHAT, VOICE & GEAJAO.AI",
        title = "Friends-Only Chat, Voice Messages, GIFs & Geajao.AI",
        subtitle = "End-to-end AES-256-GCM private messaging exclusively between accepted Friends, plus your AI companion.",
        icon = Icons.Default.SmartToy,
        accentColor = BrassGold,
        bulletPoints = listOf(
            "Only accepted Friends can chat with each other using AES-256-GCM encryption and configurable disappearing message timers.",
            "Record and send short Voice Messages with built-in waveform playback & speed controls, plus live Public GIFs and Cadet Stickers.",
            "Tap the Geajao.AI bot icon in the top bar or Chat tab anytime for ISSB prep, fitness routines, Cadet trivia, and post ideas."
        )
    ),
    UserGuideStep(
        stepNumber = 5,
        badgeLabel = "STEP 5 • OFFICIAL CONTACT INFO",
        title = "Official Support & Contact Channels",
        subtitle = "Reach out to the official Geajao team anytime via Instagram or Gmail for verification help, feedback, or support.",
        icon = Icons.Default.SupportAgent,
        accentColor = EmeraldVerified,
        bulletPoints = listOf(
            "1) Instagram: ${GeajaoConstants.OFFICIAL_INSTAGRAM_URL}",
            "2) Gmail: ${GeajaoConstants.OFFICIAL_GMAIL_ADDRESS}",
            "Use the buttons below to open Instagram directly, send an email via Gmail, or copy the contact details."
        )
    )
)

fun openOfficialInstagram(context: Context) {
    val url = GeajaoConstants.OFFICIAL_INSTAGRAM_URL
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        copyToClipboard(context, "Instagram URL", url)
    }
}

fun openOfficialGmail(context: Context) {
    val email = GeajaoConstants.OFFICIAL_GMAIL_ADDRESS
    try {
        val mailtoIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, "Geajao Cadet Network Support / Inquiry")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(mailtoIntent)
    } catch (e: Exception) {
        copyToClipboard(context, "Official Gmail", email)
    }
}

fun copyToClipboard(context: Context, label: String, value: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, "$label copied: $value", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
    }
}

/**
 * In-window full-screen modal overlay for the First-Time User Guide & Official Contact Info.
 * Rendered inside the Compose Box hierarchy to avoid creating a separate WindowManager surface.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeajaoUserGuideOverlay(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentStepIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentStep = GEAJAO_GUIDE_STEPS[currentStepIndex.coerceIn(0, GEAJAO_GUIDE_STEPS.lastIndex)]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* consume background taps */ }
            .padding(16.dp)
            .testTag("first_time_user_guide_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, BrassGold)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
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
                        Surface(
                            shape = CircleShape,
                            color = BrassGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, BrassGold),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "User Guide",
                                    tint = BrassGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "GEAJAO • FIRST-TIME USER GUIDE",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = BrassGold
                            )
                            Text(
                                text = "Cadet Handbook & Official Contacts",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_user_guide_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close User Guide"
                        )
                    }
                }

                // Step Selector Pills (1..5)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GEAJAO_GUIDE_STEPS.forEachIndexed { idx, step ->
                        FilterChip(
                            selected = currentStepIndex == idx,
                            onClick = { currentStepIndex = idx },
                            label = {
                                Text(
                                    text = "${idx + 1}. ${
                                        when (idx) {
                                            0 -> "Colleges"
                                            1 -> "ID Verify"
                                            2 -> "Friends"
                                            3 -> "Chat & AI"
                                            else -> "Contact"
                                        }
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrassGold,
                                selectedLabelColor = CadetNavyDeep
                            ),
                            modifier = Modifier.testTag("user_guide_step_chip_${idx + 1}")
                        )
                    }
                }

                // Active Step Highlight Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
                    border = BorderStroke(1.dp, currentStep.accentColor.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = currentStep.accentColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = currentStep.icon,
                                        contentDescription = null,
                                        tint = currentStep.accentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = currentStep.badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = currentStep.accentColor
                                )
                                Text(
                                    text = currentStep.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = currentStep.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider()

                        currentStep.bulletPoints.forEach { bullet ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = currentStep.accentColor,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(16.dp)
                                )
                                Text(
                                    text = bullet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Always-visible Official Contact Infos section inside the User Guide
                OfficialContactChannelsSection(
                    onOpenInstagram = { openOfficialInstagram(context) },
                    onCopyInstagram = {
                        copyToClipboard(context, "Instagram URL", GeajaoConstants.OFFICIAL_INSTAGRAM_URL)
                    },
                    onOpenGmail = { openOfficialGmail(context) },
                    onCopyGmail = {
                        copyToClipboard(context, "Official Gmail", GeajaoConstants.OFFICIAL_GMAIL_ADDRESS)
                    }
                )

                // Step Navigation & Primary Dismiss Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentStepIndex > 0) currentStepIndex -= 1
                        },
                        enabled = currentStepIndex > 0,
                        modifier = Modifier.testTag("user_guide_prev_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev")
                    }

                    Text(
                        text = "${currentStepIndex + 1} / ${GEAJAO_GUIDE_STEPS.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (currentStepIndex < GEAJAO_GUIDE_STEPS.lastIndex) {
                        OutlinedButton(
                            onClick = { currentStepIndex += 1 },
                            modifier = Modifier.testTag("user_guide_next_button")
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(68.dp))
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("user_guide_finish_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrassGold,
                        contentColor = CadetNavyDeep
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Got It • Continue to Geajao",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun OfficialContactChannelsSection(
    onOpenInstagram: () -> Unit,
    onCopyInstagram: () -> Unit,
    onOpenGmail: () -> Unit,
    onCopyGmail: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("official_contact_channels_section"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = "Official Contact Info",
                    tint = BrassGold,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "OFFICIAL CONTACT INFOS",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = BrassGold
                )
            }

            // 1) Instagram Row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenInstagram() }
                    .testTag("contact_instagram_row"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, CyanTactical.copy(alpha = 0.5f))
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Instagram",
                            tint = CyanTactical,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "1) Instagram (${GeajaoConstants.OFFICIAL_INSTAGRAM_HANDLE})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = GeajaoConstants.OFFICIAL_INSTAGRAM_URL,
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanTactical,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onCopyInstagram,
                            modifier = Modifier.testTag("copy_instagram_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Instagram Link",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenInstagram,
                            modifier = Modifier.testTag("open_instagram_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open Instagram",
                                tint = CyanTactical,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2) Gmail Row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenGmail() }
                    .testTag("contact_gmail_row"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.5f))
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Gmail",
                            tint = EmeraldVerified,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "2) Official Gmail",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = GeajaoConstants.OFFICIAL_GMAIL_ADDRESS,
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldVerified,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onCopyGmail,
                            modifier = Modifier.testTag("copy_gmail_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Gmail Address",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenGmail,
                            modifier = Modifier.testTag("open_gmail_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Send Email via Gmail",
                                tint = EmeraldVerified,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Standalone Card for ProfileScreen, SignInScreen, and CadetIdVerificationGateScreen
 * showing Official Contact Infos (Instagram & Gmail), Made By MUKHOSH tag, and a button to launch the User Guide.
 */
@Composable
fun OfficialContactAndGuideCard(
    onOpenUserGuide: () -> Unit,
    mukhoshLogoBase64: String = "",
    onUploadMukhoshLogo: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("official_contact_and_guide_card"),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "User Guide & Contact Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "User Guide & Contact Infos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Official Geajao handbook, Instagram & Gmail support",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenUserGuide,
                    modifier = Modifier.testTag("open_user_guide_card_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("User Guide")
                }
            }

            OfficialContactChannelsSection(
                onOpenInstagram = { openOfficialInstagram(context) },
                onCopyInstagram = {
                    copyToClipboard(context, "Instagram URL", GeajaoConstants.OFFICIAL_INSTAGRAM_URL)
                },
                onOpenGmail = { openOfficialGmail(context) },
                onCopyGmail = {
                    copyToClipboard(context, "Official Gmail", GeajaoConstants.OFFICIAL_GMAIL_ADDRESS)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MadeByMukhoshBadge(
                    mukhoshLogoBase64 = mukhoshLogoBase64,
                    onUploadMukhoshLogo = onUploadMukhoshLogo
                )
            }
        }
    }
}

