package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.crypto.TacticalCryptoEngine
import com.example.data.model.GeajaoConstants
import com.example.ui.theme.BrassGold
import com.example.ui.theme.BrassGoldContainer
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonHonor
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanTactical
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.VioletContainer
import com.example.ui.theme.VioletModerator

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CadetRoleBadgePill(
    badge: String,
    modifier: Modifier = Modifier,
    additionalBadges: List<String> = emptyList()
) {
    val effectiveBadges = remember(badge, additionalBadges) {
        val raw = (additionalBadges + badge.split(","))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val hasVerifiedRole = raw.any { it != GeajaoConstants.BADGE_UNVERIFIED }
        val filtered = if (hasVerifiedRole) {
            raw.filter { it != GeajaoConstants.BADGE_UNVERIFIED }
        } else {
            raw
        }
        val order = listOf(
            GeajaoConstants.BADGE_ADMIN,
            GeajaoConstants.BADGE_MODERATOR,
            GeajaoConstants.BADGE_EX_CADET,
            GeajaoConstants.BADGE_CADET,
            GeajaoConstants.BADGE_UNVERIFIED
        )
        filtered.sortedBy { b -> order.indexOf(b).let { if (it == -1) 99 else it } }
            .ifEmpty { listOf(GeajaoConstants.BADGE_CADET) }
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        effectiveBadges.forEach { singleBadge ->
            SingleRoleBadgePill(badge = singleBadge)
        }
    }
}

@Composable
private fun SingleRoleBadgePill(
    badge: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, fgColor, icon) = when (badge) {
        GeajaoConstants.BADGE_ADMIN -> Triple(CrimsonContainer, CrimsonHonor, Icons.Default.AdminPanelSettings)
        GeajaoConstants.BADGE_MODERATOR -> Triple(VioletContainer, VioletModerator, Icons.Default.GppGood)
        GeajaoConstants.BADGE_EX_CADET -> Triple(BrassGoldContainer, BrassGold, Icons.Default.MilitaryTech)
        GeajaoConstants.BADGE_CADET -> Triple(EmeraldContainer, EmeraldVerified, Icons.Default.VerifiedUser)
        else -> Triple(CyanContainer, CyanTactical, Icons.Default.Shield)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bgColor,
        border = BorderStroke(1.dp, fgColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = badge,
                tint = fgColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = badge.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = fgColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VerificationStatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg, label) = when (status) {
        GeajaoConstants.STATUS_APPROVED -> Triple(EmeraldContainer, EmeraldVerified, "ID VERIFIED")
        GeajaoConstants.STATUS_REJECTED -> Triple(CrimsonContainer, CrimsonHonor, "ID REJECTED")
        else -> Triple(BrassGoldContainer, BrassGold, "PENDING REVIEW")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun CadetAvatarCircle(
    displayName: String,
    badge: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val initials = remember(displayName) {
        displayName.trim().split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifEmpty { "CD" }
    }
    val primaryToken = remember(badge) {
        badge.split(",").map { it.trim() }.firstOrNull().orEmpty()
    }
    val ringColor = when (primaryToken) {
        GeajaoConstants.BADGE_ADMIN -> CrimsonHonor
        GeajaoConstants.BADGE_MODERATOR -> VioletModerator
        GeajaoConstants.BADGE_EX_CADET -> BrassGold
        GeajaoConstants.BADGE_CADET -> EmeraldVerified
        else -> CyanTactical
    }

    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(2.dp, ringColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelLarge,
                color = ringColor
            )
        }
    }
}

@Composable
fun Base64ImagePreview(
    dataUri: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val imageBitmap = androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = dataUri
    ) {
        value = if (dataUri.isBlank()) {
            null
        } else {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                TacticalCryptoEngine.decodeBase64DataUriToBitmap(dataUri)?.asImageBitmap()
            }
        }
    }.value
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (dataUri.isBlank()) "No Image Attached" else "Loading Image...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
