package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SecretPanelMode {
    LOCKED,
    MODERATOR,
    ADMIN
}

data class StickerOption(
    val code: String,
    val emoji: String,
    val title: String,
    val subtitle: String
)

data class GifOption(
    val code: String,
    val emoji: String,
    val title: String,
    val tag: String
)

object GeajaoConstants {
    const val CORPS_ID = "geajao_cadet_corps"
    const val MOD_SECRET_PASSCODE = "5246"
    const val ADMIN_SECRET_PASSCODE = "A54h"
    const val BOOTSTRAP_ADMIN_CODE = "GEAJAO-COMMANDANT-KEY"
    const val SECRET_COMMAND_PASSCODE = "A54h"
    const val BOOTSTRAP_ADMIN_EMAIL = "sinan01196243@gmail.com"
    const val ICON_SECRET_TAP_THRESHOLD = 6

    const val OFFICIAL_INSTAGRAM_URL = "https://www.instagram.com/_.mukhosh._?mdxt=MnF6ank1c3Yyb3h5"
    const val OFFICIAL_INSTAGRAM_HANDLE = "@_.mukhosh._"
    const val OFFICIAL_GMAIL_ADDRESS = "geajao.official@gmail.com"
    const val MADE_BY_MUKHOSH_TAG = "Made By MUKHOSH"

    const val STATUS_PENDING = "PENDING"
    const val STATUS_APPROVED = "APPROVED"
    const val STATUS_REJECTED = "REJECTED"

    const val BADGE_UNVERIFIED = "Unverified"
    const val BADGE_CADET = "Cadet"
    const val BADGE_EX_CADET = "Ex-cadet"
    const val BADGE_MODERATOR = "Moderator"
    const val BADGE_ADMIN = "Admin"

    const val ICON_GOLD_SHIELD = "GOLD_SHIELD"
    const val ICON_CRIMSON_STAR = "CRIMSON_STAR"
    const val ICON_EMERALD_EAGLE = "EMERALD_EAGLE"
    const val ICON_CYBER_STEALTH = "CYBER_STEALTH"
    const val ICON_CUSTOM_GALLERY = "CUSTOM_GALLERY"

    val APP_ICON_OPTIONS = listOf(
        ICON_GOLD_SHIELD to "Golden Academy Shield",
        ICON_CRIMSON_STAR to "Crimson Honor Star",
        ICON_EMERALD_EAGLE to "Emerald Tactical Vanguard",
        ICON_CYBER_STEALTH to "Stealth Night-Ops Crest"
    )

    val CADET_COLLEGES = listOf(
        "CCR",
        "BCC",
        "PCC",
        "RCC",
        "MCC",
        "FCC",
        "JCC",
        "SCC",
        "CCC",
        "FGCC",
        "MGCC",
        "JGCC"
    )

    /**
     * Normalizes any raw college string (and strips any legacy house/squadron value)
     * into one of the 12 official Cadet Colleges (CCR..JGCC).
     */
    fun normalizeCadetCollege(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return CADET_COLLEGES.first()
        val upper = trimmed.uppercase()
        if (upper in CADET_COLLEGES) return upper
        val matchedCollege = CADET_COLLEGES.firstOrNull { code ->
            Regex("\\b$code\\b", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)
        }
        return matchedCollege ?: CADET_COLLEGES.first()
    }

    /**
     * Sanitizes any batch/affiliation label on posts so legacy House/Squadron strings never appear.
     */
    fun sanitizeBatchLabel(rawBatch: String?): String {
        val raw = rawBatch?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        if (raw.contains("•")) {
            val parts = raw.split("•").map { it.trim() }.filter { it.isNotBlank() }
            val first = parts.firstOrNull().orEmpty()
            val college = normalizeCadetCollege(first)
            val rest = parts.drop(1).filterNot {
                it.contains("house", ignoreCase = true) || it.contains("squadron", ignoreCase = true)
            }
            return if (rest.isNotEmpty()) "$college • ${rest.joinToString(" • ")}" else college
        }
        if (raw.contains("house", ignoreCase = true) || raw.contains("squadron", ignoreCase = true)) {
            return normalizeCadetCollege(raw)
        }
        return raw
    }

    val ALL_ASSIGNABLE_BADGES_ADMIN = listOf(BADGE_CADET, BADGE_EX_CADET, BADGE_MODERATOR, BADGE_ADMIN)
    val ALL_ASSIGNABLE_BADGES_MOD = listOf(BADGE_CADET, BADGE_EX_CADET)
    val CADET_STATUS_BADGES = listOf(BADGE_CADET, BADGE_EX_CADET)

    /**
     * Updates a user's Cadet/Ex-cadet status while preserving any existing Admin/Moderator badges.
     */
    fun applyCadetStatusBadge(
        existingPrimary: String,
        existingBadges: List<String>,
        newCadetStatus: String
    ): Pair<String, List<String>> {
        val cleanStatus = if (newCadetStatus == BADGE_EX_CADET) BADGE_EX_CADET else BADGE_CADET
        val currentAll = (existingBadges + existingPrimary.split(","))
            .map { it.trim() }
            .filter { it in ALL_ASSIGNABLE_BADGES_ADMIN }
            .distinct()
        val authorityBadges = currentAll.filter { it == BADGE_ADMIN || it == BADGE_MODERATOR }
        val updatedBadges = (authorityBadges + cleanStatus).distinct()
        val updatedPrimary = when {
            updatedBadges.contains(BADGE_ADMIN) -> BADGE_ADMIN
            updatedBadges.contains(BADGE_MODERATOR) -> BADGE_MODERATOR
            else -> cleanStatus
        }
        return updatedPrimary to updatedBadges
    }

    /**
     * Toggles or assigns an Admin or Moderator badge while preserving the user's Cadet or Ex-cadet badge.
     */
    fun toggleAuthorityBadge(
        existingPrimary: String,
        existingBadges: List<String>,
        authorityBadge: String
    ): Pair<String, List<String>> {
        val currentAll = (existingBadges + existingPrimary.split(","))
            .map { it.trim() }
            .filter { it in ALL_ASSIGNABLE_BADGES_ADMIN }
            .distinct()
        val cadetStatus = if (currentAll.contains(BADGE_EX_CADET)) BADGE_EX_CADET else BADGE_CADET
        val currentAuthority = currentAll.filter { it == BADGE_ADMIN || it == BADGE_MODERATOR }.toMutableSet()
        if (currentAuthority.contains(authorityBadge)) {
            currentAuthority.remove(authorityBadge)
        } else {
            currentAuthority.add(authorityBadge)
        }
        val orderedAuthority = listOf(BADGE_ADMIN, BADGE_MODERATOR).filter { currentAuthority.contains(it) }
        val updatedBadges = (orderedAuthority + cadetStatus).distinct()
        val updatedPrimary = when {
            updatedBadges.contains(BADGE_ADMIN) -> BADGE_ADMIN
            updatedBadges.contains(BADGE_MODERATOR) -> BADGE_MODERATOR
            else -> cadetStatus
        }
        return updatedPrimary to updatedBadges
    }

    val FEED_TABS = listOf(
        "Recent",
        "Trending",
        "Friends"
    )

    val FEED_TRENDING_TAGS = listOf(
        "General",
        "Trending",
        "AcademyLife",
        "Reunion",
        "Sports",
        "ISSB",
        "Announcement"
    )

    val CHAT_STICKERS = listOf(
        StickerOption("STICKER_SALUTE", "🫡", "Salute", "Respect"),
        StickerOption("STICKER_BRAVO_ZULU", "🎖️", "Bravo Zulu", "Well Done"),
        StickerOption("STICKER_FIRE_SQUAD", "🔥", "On Fire", "Lit"),
        StickerOption("STICKER_GOLD_STAR", "⭐", "Gold Star", "Honor"),
        StickerOption("STICKER_IRON_SHIELD", "🛡️", "Shield", "Got Your Back"),
        StickerOption("STICKER_BROTHERS", "🤝", "Squad", "Friends Forever"),
        StickerOption("STICKER_LAUGH", "😂", "Haha", "So Funny"),
        StickerOption("STICKER_RESPECT", "❤️", "Much Love", "Heart"),
        StickerOption("STICKER_ROCKET", "🚀", "Full Send", "Let's Go"),
        StickerOption("STICKER_CROWN", "👑", "Top Rank", "Legend")
    )

    val CHAT_GIFS = listOf(
        GifOption("GIF_SALUTE_RESPECT", "🫡✨", "Sharp Salute", "RESPECT"),
        GifOption("GIF_CELEBRATION", "🎉🥳", "Squad Celebration", "PARTY"),
        GifOption("GIF_LAUGHING", "🤣💥", "Rolling Laughing", "FUNNY"),
        GifOption("GIF_MIND_BLOWN", "🤯⚡", "Mind Blown", "WOW"),
        GifOption("GIF_HIGH_FIVE", "🙌🔥", "Tactical High Five", "TEAM"),
        GifOption("GIF_THUMBS_UP", "👍🌟", "Approved & Ready", "OKAY"),
        GifOption("GIF_ON_MY_WAY", "🏃💨", "On My Way!", "HUSTLE"),
        GifOption("GIF_APPLAUSE", "👏🏆", "Standing Ovation", "BRAVO")
    )

    fun deterministicFriendshipId(uidA: String, uidB: String): String {
        val sorted = listOf(uidA, uidB).sorted()
        return "friend_${sorted[0]}_${sorted[1]}".take(120)
    }
}

@IgnoreExtraProperties
data class AdminRole(
    @DocumentId val id: String = "",
    val uid: String = "",
    val grantedBy: String = "",
    val bootstrapCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ModeratorRole(
    @DocumentId val id: String = "",
    val uid: String = "",
    val grantedBy: String = "",
    val bootstrapCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "",
    val configId: String = "global",
    val selectedIconKey: String = GeajaoConstants.ICON_GOLD_SHIELD,
    val customIconBase64: String = "",
    val mukhoshLogoBase64: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CadetProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val corpsId: String = GeajaoConstants.CORPS_ID,
    val displayName: String = "",
    val cadetNumber: String = "",
    val batchYear: String = "",
    val academyHouse: String = "",
    val bio: String = "",
    val verificationStatus: String = GeajaoConstants.STATUS_PENDING,
    val primaryBadge: String = GeajaoConstants.BADGE_UNVERIFIED,
    val badges: List<String> = emptyList(),
    val idCardImageBase64: String = "",
    val idCardNote: String = "",
    val followersCount: Long = 0L,
    val profileViews: Long = 0L,
    val totalEngagement: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val cadetCollege: String
        get() = GeajaoConstants.normalizeCadetCollege(academyHouse)

    val isApproved: Boolean
        get() = verificationStatus == GeajaoConstants.STATUS_APPROVED

    val hasAdminBadge: Boolean
        get() = primaryBadge == GeajaoConstants.BADGE_ADMIN || badges.contains(GeajaoConstants.BADGE_ADMIN)

    val hasModeratorBadge: Boolean
        get() = hasAdminBadge || primaryBadge == GeajaoConstants.BADGE_MODERATOR || badges.contains(GeajaoConstants.BADGE_MODERATOR)

    val cadetStatusBadge: String
        get() = when {
            badges.contains(GeajaoConstants.BADGE_EX_CADET) || primaryBadge == GeajaoConstants.BADGE_EX_CADET ->
                GeajaoConstants.BADGE_EX_CADET
            badges.contains(GeajaoConstants.BADGE_CADET) || primaryBadge == GeajaoConstants.BADGE_CADET ->
                GeajaoConstants.BADGE_CADET
            else -> GeajaoConstants.BADGE_CADET
        }

    val allEffectiveBadges: List<String>
        get() {
            val raw = (badges + primaryBadge.split(","))
                .map { it.trim() }
                .filter { it.isNotBlank() && it != GeajaoConstants.BADGE_UNVERIFIED }
                .distinct()
            if (!isApproved && raw.none { it == GeajaoConstants.BADGE_ADMIN || it == GeajaoConstants.BADGE_MODERATOR }) {
                val pendingCadetChoice = raw.firstOrNull {
                    it == GeajaoConstants.BADGE_EX_CADET || it == GeajaoConstants.BADGE_CADET
                }
                return if (pendingCadetChoice != null) {
                    listOf(GeajaoConstants.BADGE_UNVERIFIED, pendingCadetChoice)
                } else {
                    listOf(GeajaoConstants.BADGE_UNVERIFIED)
                }
            }
            val ordered = mutableListOf<String>()
            if (raw.contains(GeajaoConstants.BADGE_ADMIN)) ordered.add(GeajaoConstants.BADGE_ADMIN)
            if (raw.contains(GeajaoConstants.BADGE_MODERATOR)) ordered.add(GeajaoConstants.BADGE_MODERATOR)
            if (raw.contains(GeajaoConstants.BADGE_EX_CADET)) {
                ordered.add(GeajaoConstants.BADGE_EX_CADET)
            } else {
                ordered.add(GeajaoConstants.BADGE_CADET)
            }
            return ordered.distinct()
        }

    val serializedBadgeSummary: String
        get() = allEffectiveBadges
            .filter { it != GeajaoConstants.BADGE_UNVERIFIED }
            .ifEmpty { listOf(GeajaoConstants.BADGE_CADET) }
            .joinToString(",")
            .take(30)
}

@IgnoreExtraProperties
data class Friendship(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val requesterId: String = "",
    val requesterName: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isAccepted: Boolean
        get() = status == "ACCEPTED"

    fun otherUid(myUid: String): String =
        participantUids.firstOrNull { it != myUid } ?: if (requesterId == myUid) receiverId else requesterId

    fun otherName(myUid: String): String =
        if (requesterId == myUid) receiverName else requesterName
}

@IgnoreExtraProperties
data class FeedPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorBadge: String = GeajaoConstants.BADGE_CADET,
    val authorBatch: String = "",
    val corpsId: String = GeajaoConstants.CORPS_ID,
    val title: String = "",
    val content: String = "",
    val category: String = "General",
    val mediaImageBase64: String = "",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val viewsCount: Long = 0L,
    val sharesCount: Long = 0L,
    val likedByUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatRoom(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val participantNames: String = "",
    val chatTitle: String = "",
    val lastMessagePreview: String = "",
    val disappearingTtlSeconds: Long = 0L,
    val isEncrypted: Boolean = true,
    val encryptionKeyHint: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EncryptedMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderBadge: String = GeajaoConstants.BADGE_CADET,
    val participantUids: List<String> = emptyList(),
    val cipherText: String = "",
    val ivBase64: String = "",
    val mediaType: String = "NONE",
    val mediaDataBase64: String = "",
    val ttlSeconds: Long = 0L,
    val expiresAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerBadge: String = GeajaoConstants.BADGE_CADET,
    val calleeId: String = "",
    val calleeName: String = "",
    val participantUids: List<String> = emptyList(),
    val callType: String = "VIDEO",
    val status: String = "RINGING",
    val encryptionFingerprint: String = "",
    val isCallerMuted: Boolean = false,
    val isCalleeMuted: Boolean = false,
    val isVideoEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class GeajaoAiMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

sealed interface GeajaoAiState {
    data object Idle : GeajaoAiState
    data object Loading : GeajaoAiState
    data class Streaming(val partialText: String) : GeajaoAiState
    data class Success(val text: String) : GeajaoAiState
    data class Error(val message: String) : GeajaoAiState
}

@IgnoreExtraProperties
data class FcmTokenRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fcmToken: String = "",
    val platform: String = "ANDROID",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PushNotificationEvent(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderBadge: String = "",
    val type: String = "PRIVATE_MESSAGE", // "PRIVATE_MESSAGE" | "FRIEND_REQUEST" | "FRIEND_ACCEPTED"
    val title: String = "",
    val body: String = "",
    val targetId: String = "",
    val isDelivered: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)


