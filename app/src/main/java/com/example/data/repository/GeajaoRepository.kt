package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.crypto.TacticalCryptoEngine
import com.example.data.model.AppConfig
import com.example.data.model.CadetProfile
import com.example.data.model.CallSession
import com.example.data.model.ChatRoom
import com.example.data.model.EncryptedMessage
import com.example.data.model.FeedPost
import com.example.data.model.Friendship
import com.example.data.model.GeajaoConstants
import com.example.data.model.PushNotificationEvent
import com.example.data.model.SecretPanelMode
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GeajaoRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- GLOBAL APP CONFIG (ADMIN-SELECTED OR GALLERY-UPLOADED APP ICON) ---

    fun observeAppConfig(): Flow<AppConfig> {
        val path = "app_config/global"
        return db.collection("app_config").document("global")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: AppConfig()
                } else {
                    AppConfig()
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun updateSelectedAppIcon(
        iconKey: String,
        customIconBase64: String = ""
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val allowedKeys = GeajaoConstants.APP_ICON_OPTIONS.map { it.first } + GeajaoConstants.ICON_CUSTOM_GALLERY
        require(iconKey in allowedKeys) {
            "Invalid app icon key"
        }
        val docRef = db.collection("app_config").document("global")
        try {
            val existingSnap = docRef.get().await()
            val existingMukhosh = if (existingSnap.exists()) {
                existingSnap.getString("mukhoshLogoBase64").orEmpty()
            } else {
                ""
            }
            val payload = mutableMapOf<String, Any>(
                "configId" to "global",
                "selectedIconKey" to iconKey,
                "customIconBase64" to customIconBase64.take(150000),
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingMukhosh.isNotBlank()) {
                payload["mukhoshLogoBase64"] = existingMukhosh.take(150000)
            }
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateMukhoshLogo(
        mukhoshLogoBase64: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("app_config").document("global")
        val trimmedLogo = mukhoshLogoBase64.take(150000)
        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updates = mapOf(
                    "mukhoshLogoBase64" to trimmedLogo,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "configId" to "global",
                    "selectedIconKey" to GeajaoConstants.ICON_GOLD_SHIELD,
                    "customIconBase64" to "",
                    "mukhoshLogoBase64" to trimmedLogo,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- ROLE & PROFILE OBSERVATION ---

    fun observeIsAdmin(userId: String): Flow<Boolean> {
        val path = "admins/$userId"
        return db.collection("admins").document(userId)
            .snapshots()
            .map { snapshot -> snapshot.exists() }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeIsModerator(userId: String): Flow<Boolean> {
        val path = "moderators/$userId"
        return db.collection("moderators").document(userId)
            .snapshots()
            .map { snapshot -> snapshot.exists() }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeCadetProfile(userId: String): Flow<CadetProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(CadetProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?.let { profile ->
                            profile.copy(
                                academyHouse = GeajaoConstants.normalizeCadetCollege(profile.academyHouse)
                            )
                        }
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeAllCorpsCadets(): Flow<List<CadetProfile>> {
        val path = "users"
        return db.collection("users")
            .whereEqualTo("corpsId", GeajaoConstants.CORPS_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CadetProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .map { profile ->
                        profile.copy(
                            academyHouse = GeajaoConstants.normalizeCadetCollege(profile.academyHouse)
                        )
                    }
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    // --- CADET ID CARD REGISTRATION & PROFILE CUSTOMIZATION ---

    suspend fun registerOrUpdateCadetProfile(
        displayName: String,
        cadetNumber: String,
        batchYear: String,
        academyHouse: String,
        bio: String,
        idCardImageBase64: String,
        idCardNote: String,
        cadetStatusBadge: String = "",
        activeSecretMode: SecretPanelMode = SecretPanelMode.LOCKED
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val existingSnap = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }

        val chosenCadetStatus = if (cadetStatusBadge == GeajaoConstants.BADGE_EX_CADET) {
            GeajaoConstants.BADGE_EX_CADET
        } else {
            GeajaoConstants.BADGE_CADET
        }

        // If user unlocked Moderator (5246) or Admin (A54h) in this session, ensure role doc exists
        if (activeSecretMode == SecretPanelMode.ADMIN) {
            try {
                val adminRef = db.collection("admins").document(uid)
                val snap = adminRef.get().await()
                if (!snap.exists()) {
                    adminRef.set(
                        mapOf(
                            "uid" to uid,
                            "grantedBy" to uid,
                            "bootstrapCode" to GeajaoConstants.ADMIN_SECRET_PASSCODE,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } catch (_: Exception) {
            }
        } else if (activeSecretMode == SecretPanelMode.MODERATOR) {
            try {
                val modRef = db.collection("moderators").document(uid)
                val snap = modRef.get().await()
                if (!snap.exists()) {
                    modRef.set(
                        mapOf(
                            "uid" to uid,
                            "grantedBy" to uid,
                            "bootstrapCode" to GeajaoConstants.MOD_SECRET_PASSCODE,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } catch (_: Exception) {
            }
        }

        val currentEmail = auth.currentUser?.email?.lowercase().orEmpty()
        val isBootstrappedAdmin = currentEmail == GeajaoConstants.BOOTSTRAP_ADMIN_EMAIL.lowercase() &&
            auth.currentUser?.isEmailVerified == true
        val adminSnap = try {
            db.collection("admins").document(uid).get().await()
        } catch (e: Exception) {
            null
        }
        val modSnap = try {
            db.collection("moderators").document(uid).get().await()
        } catch (e: Exception) {
            null
        }
        val isCallerAdmin = isBootstrappedAdmin || adminSnap?.exists() == true || activeSecretMode == SecretPanelMode.ADMIN
        val isCallerMod = modSnap?.exists() == true || activeSecretMode == SecretPanelMode.MODERATOR
        val hasSubmittedIdCard = idCardImageBase64.isNotBlank()

        if (!existingSnap.exists()) {
            val initialBadges = buildList {
                if (isCallerAdmin && hasSubmittedIdCard) add(GeajaoConstants.BADGE_ADMIN)
                if (isCallerMod && hasSubmittedIdCard) add(GeajaoConstants.BADGE_MODERATOR)
                if (cadetStatusBadge.isNotBlank() || isCallerAdmin || isCallerMod) {
                    add(chosenCadetStatus)
                }
            }.distinct()

            val initialStatus = if ((isCallerAdmin || isCallerMod) && hasSubmittedIdCard) {
                GeajaoConstants.STATUS_APPROVED
            } else {
                GeajaoConstants.STATUS_PENDING
            }
            val initialPrimaryBadge = when {
                isCallerAdmin && hasSubmittedIdCard -> GeajaoConstants.BADGE_ADMIN
                isCallerMod && hasSubmittedIdCard -> GeajaoConstants.BADGE_MODERATOR
                else -> GeajaoConstants.BADGE_UNVERIFIED
            }

            val normalizedCollege = GeajaoConstants.normalizeCadetCollege(academyHouse)
            val payload = mapOf(
                "userId" to uid,
                "corpsId" to GeajaoConstants.CORPS_ID,
                "displayName" to displayName.trim().take(80),
                "cadetNumber" to cadetNumber.trim().take(40),
                "batchYear" to batchYear.trim().take(20),
                "academyHouse" to normalizedCollege.take(60),
                "bio" to bio.trim().take(300),
                "verificationStatus" to initialStatus,
                "primaryBadge" to initialPrimaryBadge,
                "badges" to initialBadges,
                "idCardImageBase64" to idCardImageBase64.take(150000),
                "idCardNote" to idCardNote.trim().take(200),
                "followersCount" to 0L,
                "profileViews" to 0L,
                "totalEngagement" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        } else {
            val existingProfile = existingSnap.toObject(CadetProfile::class.java)
            val existingPrimary = existingProfile?.primaryBadge ?: GeajaoConstants.BADGE_UNVERIFIED
            val existingBadges = existingProfile?.badges ?: emptyList()
            val normalizedCollege = GeajaoConstants.normalizeCadetCollege(academyHouse)

            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName.trim().take(80),
                "cadetNumber" to cadetNumber.trim().take(40),
                "batchYear" to batchYear.trim().take(20),
                "academyHouse" to normalizedCollege.take(60),
                "bio" to bio.trim().take(300),
                "idCardImageBase64" to idCardImageBase64.take(150000),
                "idCardNote" to idCardNote.trim().take(200),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if ((isCallerAdmin || isCallerMod) && hasSubmittedIdCard) {
                val updatedBadges = buildList {
                    if (isCallerAdmin || existingBadges.contains(GeajaoConstants.BADGE_ADMIN)) {
                        add(GeajaoConstants.BADGE_ADMIN)
                    }
                    if (isCallerMod || existingBadges.contains(GeajaoConstants.BADGE_MODERATOR)) {
                        add(GeajaoConstants.BADGE_MODERATOR)
                    }
                    add(chosenCadetStatus)
                }.distinct()
                val updatedPrimary = when {
                    isCallerAdmin || existingPrimary == GeajaoConstants.BADGE_ADMIN -> GeajaoConstants.BADGE_ADMIN
                    isCallerMod || existingPrimary == GeajaoConstants.BADGE_MODERATOR -> GeajaoConstants.BADGE_MODERATOR
                    else -> chosenCadetStatus
                }
                updates["verificationStatus"] = GeajaoConstants.STATUS_APPROVED
                updates["primaryBadge"] = updatedPrimary
                updates["badges"] = updatedBadges
            } else {
                if (existingProfile?.verificationStatus == GeajaoConstants.STATUS_REJECTED && hasSubmittedIdCard) {
                    updates["verificationStatus"] = GeajaoConstants.STATUS_PENDING
                }
                if (cadetStatusBadge.isNotBlank()) {
                    val authorityBadges = existingBadges.filter {
                        it == GeajaoConstants.BADGE_ADMIN || it == GeajaoConstants.BADGE_MODERATOR
                    }
                    val updatedBadges = (authorityBadges + chosenCadetStatus).distinct()
                    val updatedPrimary = if (existingPrimary == GeajaoConstants.BADGE_UNVERIFIED) {
                        GeajaoConstants.BADGE_UNVERIFIED
                    } else if (existingPrimary == GeajaoConstants.BADGE_ADMIN || existingPrimary == GeajaoConstants.BADGE_MODERATOR) {
                        existingPrimary
                    } else {
                        chosenCadetStatus
                    }
                    updates["primaryBadge"] = updatedPrimary
                    updates["badges"] = updatedBadges
                }
            }

            try {
                docRef.update(updates).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        }
    }

    /**
     * Activates Moderator privileges when the secret Moderator passcode ('5246') is entered after clicking the icon 6 times.
     * Preserves the user's Cadet or Ex-cadet badge (and Admin badge if present).
     */
    suspend fun activateModeratorRoleWithPasscode(
        passcode: String = GeajaoConstants.MOD_SECRET_PASSCODE
    ): Result<Unit> = runCatching {
        require(passcode == GeajaoConstants.MOD_SECRET_PASSCODE) {
            "Invalid Moderator passcode."
        }
        val uid = requireUserId()
        val modRef = db.collection("moderators").document(uid)
        val existingMod = try {
            modRef.get().await()
        } catch (_: Exception) {
            null
        }
        if (existingMod == null || !existingMod.exists()) {
            val modPayload = mapOf(
                "uid" to uid,
                "grantedBy" to uid,
                "bootstrapCode" to passcode,
                "createdAt" to FieldValue.serverTimestamp()
            )
            try {
                modRef.set(modPayload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, modRef.path)
                throw e
            }
        }

        val userRef = db.collection("users").document(uid)
        val snap = userRef.get().await()
        if (snap.exists() && !snap.getString("idCardImageBase64").isNullOrBlank()) {
            val existingProfile = snap.toObject(CadetProfile::class.java)
            val cadetStatus = existingProfile?.cadetStatusBadge ?: GeajaoConstants.BADGE_CADET
            val hasAdmin = existingProfile?.hasAdminBadge == true
            val combinedBadges = buildList {
                if (hasAdmin) add(GeajaoConstants.BADGE_ADMIN)
                add(GeajaoConstants.BADGE_MODERATOR)
                add(cadetStatus)
            }.distinct()
            val newPrimary = if (hasAdmin) GeajaoConstants.BADGE_ADMIN else GeajaoConstants.BADGE_MODERATOR
            try {
                userRef.update(
                    mapOf(
                        "verificationStatus" to GeajaoConstants.STATUS_APPROVED,
                        "primaryBadge" to newPrimary,
                        "badges" to combinedBadges,
                        "idCardNote" to "Moderator Panel Clearance",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, userRef.path)
                throw e
            }
        }
    }

    /**
     * Activates Admin privileges when the secret Admin passcode ('A54h') is entered after clicking the icon 6 times.
     * Preserves the user's Cadet or Ex-cadet badge (and Moderator badge if present).
     */
    suspend fun activateCommandantAdminRole(
        passcode: String = GeajaoConstants.ADMIN_SECRET_PASSCODE
    ): Result<Unit> = runCatching {
        require(
            passcode == GeajaoConstants.ADMIN_SECRET_PASSCODE ||
                passcode == GeajaoConstants.BOOTSTRAP_ADMIN_CODE ||
                passcode == "1948"
        ) {
            "Invalid Admin passcode."
        }
        val uid = requireUserId()
        val adminRef = db.collection("admins").document(uid)
        val existingAdmin = try {
            adminRef.get().await()
        } catch (_: Exception) {
            null
        }
        if (existingAdmin == null || !existingAdmin.exists()) {
            val adminPayload = mapOf(
                "uid" to uid,
                "grantedBy" to uid,
                "bootstrapCode" to passcode,
                "createdAt" to FieldValue.serverTimestamp()
            )
            try {
                adminRef.set(adminPayload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, adminRef.path)
                throw e
            }
        }

        val userRef = db.collection("users").document(uid)
        val snap = userRef.get().await()
        if (snap.exists() && !snap.getString("idCardImageBase64").isNullOrBlank()) {
            val existingProfile = snap.toObject(CadetProfile::class.java)
            val cadetStatus = existingProfile?.cadetStatusBadge ?: GeajaoConstants.BADGE_CADET
            val hasMod = existingProfile?.badges?.contains(GeajaoConstants.BADGE_MODERATOR) == true ||
                existingProfile?.primaryBadge == GeajaoConstants.BADGE_MODERATOR
            val combinedBadges = buildList {
                add(GeajaoConstants.BADGE_ADMIN)
                if (hasMod) add(GeajaoConstants.BADGE_MODERATOR)
                add(cadetStatus)
            }.distinct()
            try {
                userRef.update(
                    mapOf(
                        "verificationStatus" to GeajaoConstants.STATUS_APPROVED,
                        "primaryBadge" to GeajaoConstants.BADGE_ADMIN,
                        "badges" to combinedBadges,
                        "idCardNote" to "Commandant Admin Clearance",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, userRef.path)
                throw e
            }
        }
    }

    // --- ADMIN & MODERATOR APPROVAL & BADGE ASSIGNMENT ---

    suspend fun approveOrRejectCadet(
        targetUserId: String,
        approve: Boolean,
        primaryBadge: String,
        badges: List<String>,
        reviewerNote: String,
        isCallerAdmin: Boolean
    ): Result<Unit> = runCatching {
        val callerUid = requireUserId()
        val userRef = db.collection("users").document(targetUserId)

        val existingSnap = try {
            userRef.get().await()
        } catch (e: Exception) {
            null
        }
        val existingProfile = existingSnap?.toObject(CadetProfile::class.java)
        val existingBadges = existingProfile?.badges ?: emptyList()
        val existingPrimary = existingProfile?.primaryBadge ?: GeajaoConstants.BADGE_UNVERIFIED

        val finalBadges: List<String>
        val safePrimaryBadge: String

        if (!approve) {
            finalBadges = emptyList()
            safePrimaryBadge = GeajaoConstants.BADGE_UNVERIFIED
        } else if (isCallerAdmin) {
            val cleaned = (badges + primaryBadge)
                .filter { it in GeajaoConstants.ALL_ASSIGNABLE_BADGES_ADMIN }
                .distinct()
            val cadetStatus = when {
                cleaned.contains(GeajaoConstants.BADGE_EX_CADET) -> GeajaoConstants.BADGE_EX_CADET
                cleaned.contains(GeajaoConstants.BADGE_CADET) -> GeajaoConstants.BADGE_CADET
                existingBadges.contains(GeajaoConstants.BADGE_EX_CADET) ||
                    existingPrimary == GeajaoConstants.BADGE_EX_CADET -> GeajaoConstants.BADGE_EX_CADET
                else -> GeajaoConstants.BADGE_CADET
            }
            val authority = cleaned.filter {
                it == GeajaoConstants.BADGE_ADMIN || it == GeajaoConstants.BADGE_MODERATOR
            }
            finalBadges = (authority + cadetStatus).distinct().take(6)
            safePrimaryBadge = when {
                primaryBadge in GeajaoConstants.ALL_ASSIGNABLE_BADGES_ADMIN && finalBadges.contains(primaryBadge) -> primaryBadge
                finalBadges.contains(GeajaoConstants.BADGE_ADMIN) -> GeajaoConstants.BADGE_ADMIN
                finalBadges.contains(GeajaoConstants.BADGE_MODERATOR) -> GeajaoConstants.BADGE_MODERATOR
                else -> cadetStatus
            }
        } else {
            // Moderator can assign Cadet or Ex-cadet while preserving any existing Admin/Moderator badges on the target user
            val requestedCadetStatus = when {
                primaryBadge == GeajaoConstants.BADGE_EX_CADET || badges.contains(GeajaoConstants.BADGE_EX_CADET) ->
                    GeajaoConstants.BADGE_EX_CADET
                primaryBadge == GeajaoConstants.BADGE_CADET || badges.contains(GeajaoConstants.BADGE_CADET) ->
                    GeajaoConstants.BADGE_CADET
                existingBadges.contains(GeajaoConstants.BADGE_EX_CADET) || existingPrimary == GeajaoConstants.BADGE_EX_CADET ->
                    GeajaoConstants.BADGE_EX_CADET
                else -> GeajaoConstants.BADGE_CADET
            }
            val preservedAuthority = (existingBadges + existingPrimary).filter {
                it == GeajaoConstants.BADGE_ADMIN || it == GeajaoConstants.BADGE_MODERATOR
            }.distinct()
            finalBadges = (preservedAuthority + requestedCadetStatus).distinct().take(6)
            safePrimaryBadge = when {
                existingPrimary == GeajaoConstants.BADGE_ADMIN || existingPrimary == GeajaoConstants.BADGE_MODERATOR ->
                    existingPrimary
                else -> requestedCadetStatus
            }
        }

        try {
            userRef.update(
                mapOf(
                    "verificationStatus" to if (approve) GeajaoConstants.STATUS_APPROVED else GeajaoConstants.STATUS_REJECTED,
                    "primaryBadge" to safePrimaryBadge,
                    "badges" to finalBadges,
                    "idCardNote" to reviewerNote.trim().take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }

        // If caller is Admin, sync /moderators/{targetUserId} and /admins/{targetUserId} role collections
        if (isCallerAdmin) {
            val modRef = db.collection("moderators").document(targetUserId)
            if (finalBadges.contains(GeajaoConstants.BADGE_MODERATOR)) {
                try {
                    modRef.set(
                        mapOf(
                            "uid" to targetUserId,
                            "grantedBy" to callerUid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, modRef.path)
                }
            } else {
                try {
                    modRef.delete().await()
                } catch (_: Exception) {
                }
            }

            val adminRef = db.collection("admins").document(targetUserId)
            if (finalBadges.contains(GeajaoConstants.BADGE_ADMIN)) {
                try {
                    adminRef.set(
                        mapOf(
                            "uid" to targetUserId,
                            "grantedBy" to callerUid,
                            "bootstrapCode" to GeajaoConstants.ADMIN_SECRET_PASSCODE,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, adminRef.path)
                }
            } else if (targetUserId != callerUid) {
                try {
                    adminRef.delete().await()
                } catch (_: Exception) {
                }
            }
        }
    }

    // --- FRIEND SYSTEM (ONLY FRIENDS CAN CHAT) ---

    fun observeUserFriendships(userId: String): Flow<List<Friendship>> {
        val path = "friendships"
        return db.collection("friendships")
            .whereArrayContains("participantUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Friendship::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun sendFriendRequest(
        targetUserId: String,
        requesterName: String,
        receiverName: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(uid != targetUserId) { "Cannot send friend request to yourself." }
        val sortedUids = listOf(uid, targetUserId).sorted()
        val friendshipId = GeajaoConstants.deterministicFriendshipId(uid, targetUserId)
        val docRef = db.collection("friendships").document(friendshipId)
        val payload = mapOf(
            "participantUids" to sortedUids,
            "requesterId" to uid,
            "requesterName" to requesterName.ifBlank { "Cadet" }.take(80),
            "receiverId" to targetUserId,
            "receiverName" to receiverName.ifBlank { "Cadet" }.take(80),
            "status" to "PENDING",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val existingSnap = try {
                docRef.get().await()
            } catch (_: Exception) {
                null
            }
            if (existingSnap != null && existingSnap.exists()) {
                val existingFriendship = existingSnap.toObject(Friendship::class.java)
                if (existingFriendship?.status == "ACCEPTED") {
                    return@runCatching friendshipId
                }
                if (existingFriendship?.receiverId == uid && existingFriendship.status == "PENDING") {
                    docRef.update(
                        mapOf(
                            "status" to "ACCEPTED",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    return@runCatching friendshipId
                }
                if (existingFriendship?.requesterId == uid && existingFriendship.status == "PENDING") {
                    return@runCatching friendshipId
                }
            }
            docRef.set(payload).await()
            dispatchPushNotificationEvent(
                recipientId = targetUserId,
                senderId = uid,
                senderName = requesterName.ifBlank { "Cadet" },
                senderBadge = GeajaoConstants.BADGE_CADET,
                type = "FRIEND_REQUEST",
                title = "New Cadet Friend Request",
                body = "${requesterName.ifBlank { "A Cadet" }} sent you a friend request on Geajao.",
                targetId = friendshipId
            )
            friendshipId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun acceptFriendRequest(friendshipId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("friendships").document(friendshipId)
        val snap = try {
            docRef.get().await()
        } catch (e: Exception) {
            null
        }
        val existingFriendship = snap?.toObject(Friendship::class.java)
        try {
            docRef.update(
                mapOf(
                    "status" to "ACCEPTED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (existingFriendship != null && existingFriendship.requesterId.isNotBlank() && existingFriendship.requesterId != uid) {
                dispatchPushNotificationEvent(
                    recipientId = existingFriendship.requesterId,
                    senderId = uid,
                    senderName = existingFriendship.receiverName.ifBlank { "Cadet" },
                    senderBadge = GeajaoConstants.BADGE_CADET,
                    type = "FRIEND_ACCEPTED",
                    title = "Friend Request Accepted",
                    body = "${existingFriendship.receiverName.ifBlank { "Your friend" }} accepted your friend request! You can now chat.",
                    targetId = friendshipId
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun removeOrDeclineFriendship(friendshipId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("friendships").document(friendshipId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- SOCIAL FEED & CREATOR ANALYTICS ---

    fun observeFeedPosts(): Flow<List<FeedPost>> {
        val path = "posts"
        return db.collection("posts")
            .whereEqualTo("corpsId", GeajaoConstants.CORPS_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FeedPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .map { post ->
                        post.copy(
                            authorBatch = GeajaoConstants.sanitizeBatchLabel(post.authorBatch)
                        )
                    }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createFeedPost(
        authorName: String,
        authorBadge: String,
        authorBatch: String,
        title: String = "",
        content: String,
        category: String = "General",
        mediaImageBase64: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("posts").document()
        val cleanContent = content.trim().take(2000)
        val cleanTitle = title.trim().ifBlank { cleanContent.take(60) }.take(120)
        val payload = mapOf(
            "authorId" to uid,
            "authorName" to authorName.trim().ifBlank { "Cadet" }.take(80),
            "authorBadge" to authorBadge.ifBlank { GeajaoConstants.BADGE_CADET }.take(30),
            "authorBatch" to authorBatch.trim().take(20),
            "corpsId" to GeajaoConstants.CORPS_ID,
            "title" to cleanTitle,
            "content" to cleanContent,
            "category" to category.ifBlank { "General" }.take(40),
            "mediaImageBase64" to mediaImageBase64.take(150000),
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "viewsCount" to 1L,
            "sharesCount" to 0L,
            "likedByUids" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleSaluteOnPost(post: FeedPost): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("posts").document(post.id)
        val alreadyLiked = post.likedByUids.contains(uid)
        val updatedLikedBy = if (alreadyLiked) {
            post.likedByUids - uid
        } else {
            (post.likedByUids + uid).distinct().take(10)
        }
        val newLikesCount = if (alreadyLiked) {
            (post.likesCount - 1L).coerceAtLeast(0L)
        } else {
            post.likesCount + 1L
        }

        try {
            docRef.update(
                mapOf(
                    "likesCount" to newLikesCount,
                    "viewsCount" to post.viewsCount + 1L,
                    "likedByUids" to updatedLikedBy,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun recordPostShareOrComment(post: FeedPost, isComment: Boolean): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("posts").document(post.id)
        val updates = if (isComment) {
            mapOf(
                "commentsCount" to post.commentsCount + 1L,
                "viewsCount" to post.viewsCount + 1L,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            mapOf(
                "sharesCount" to post.sharesCount + 1L,
                "viewsCount" to post.viewsCount + 1L,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- ENCRYPTED PRIVATE MESSAGING & DISAPPEARING MESSAGES ---

    fun observeUserChatRooms(userId: String): Flow<List<ChatRoom>> {
        val path = "chats"
        return db.collection("chats")
            .whereArrayContains("participantUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ChatRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeEncryptedMessages(chatId: String, userId: String): Flow<List<EncryptedMessage>> {
        val path = "chats/$chatId/messages"
        return db.collection("chats").document(chatId).collection("messages")
            .whereArrayContains("participantUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(EncryptedMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createOrGetEncryptedChatRoom(
        otherUserId: String,
        currentUserName: String,
        otherUserName: String,
        disappearingTtlSeconds: Long = 300L
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val sortedUids = listOf(uid, otherUserId).distinct().sorted()
        val deterministicChatId = "chat_${sortedUids.joinToString("_")}".take(120)
        val docRef = db.collection("chats").document(deterministicChatId)

        val payload = mapOf(
            "participantUids" to sortedUids,
            "participantNames" to "$currentUserName & $otherUserName".take(200),
            "chatTitle" to otherUserName.take(100),
            "lastMessagePreview" to "AES-256-GCM Channel Established",
            "disappearingTtlSeconds" to disappearingTtlSeconds.coerceIn(0L, 604800L),
            "isEncrypted" to true,
            "encryptionKeyHint" to TacticalCryptoEngine.generateKeyFingerprint(deterministicChatId, sortedUids),
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val snap = try {
                docRef.get().await()
            } catch (e: Exception) {
                null
            }
            if (snap == null || !snap.exists()) {
                docRef.set(payload).await()
            }
            deterministicChatId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateChatDisappearingTimer(chatId: String, ttlSeconds: Long): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("chats").document(chatId)
        try {
            docRef.update(
                mapOf(
                    "disappearingTtlSeconds" to ttlSeconds.coerceIn(0L, 604800L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun sendEncryptedMessage(
        chatId: String,
        senderName: String,
        senderBadge: String,
        participantUids: List<String>,
        plainText: String,
        mediaType: String = "NONE",
        mediaDataBase64: String = "",
        ttlSeconds: Long = 0L
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val encrypted = TacticalCryptoEngine.encryptMessage(plainText.ifBlank { "[Media]" }, chatId)
        val expiresAtEpochMs = if (ttlSeconds > 0L) {
            System.currentTimeMillis() + (ttlSeconds * 1000L)
        } else {
            0L
        }

        val msgRef = db.collection("chats").document(chatId).collection("messages").document()
        val payload = mapOf(
            "chatId" to chatId,
            "senderId" to uid,
            "senderName" to senderName.take(80),
            "senderBadge" to senderBadge.take(30),
            "participantUids" to participantUids.take(10),
            "cipherText" to encrypted.cipherTextBase64.take(4000),
            "ivBase64" to encrypted.ivBase64.take(120),
            "mediaType" to mediaType,
            "mediaDataBase64" to mediaDataBase64.take(150000),
            "ttlSeconds" to ttlSeconds.coerceIn(0L, 604800L),
            "expiresAtEpochMs" to expiresAtEpochMs,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            msgRef.set(payload).await()
            val previewLabel = when (mediaType) {
                "IMAGE" -> "🔒 [Encrypted Photo]"
                "GIF" -> "🔒 [Encrypted GIF]"
                "STICKER" -> "🔒 [Encrypted Sticker]"
                "VOICE_NOTE" -> "🔒 [Encrypted Voice Note]"
                "GRID_COORDS" -> "🔒 [Encrypted Coordinates]"
                else -> "🔒 AES-GCM: ${encrypted.cipherTextBase64.take(18)}..."
            }
            db.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessagePreview" to previewLabel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val pushPreview = when (mediaType) {
                "IMAGE" -> "Sent you an encrypted photo"
                "GIF" -> "Sent you an expressive GIF reaction"
                "STICKER" -> "Sent you a Cadet sticker"
                "VOICE_NOTE" -> "Sent you a voice message"
                else -> plainText.trim().ifBlank { "Sent you an encrypted private message" }.take(120)
            }
            participantUids.filter { it.isNotBlank() && it != uid }.forEach { recipientUid ->
                dispatchPushNotificationEvent(
                    recipientId = recipientUid,
                    senderId = uid,
                    senderName = senderName.ifBlank { "Cadet" },
                    senderBadge = senderBadge.ifBlank { GeajaoConstants.BADGE_CADET },
                    type = "PRIVATE_MESSAGE",
                    title = "${senderName.ifBlank { "Cadet" }} [$senderBadge]",
                    body = pushPreview,
                    targetId = chatId
                )
            }
            msgRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
    }

    suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> = runCatching {
        requireUserId()
        val msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
        try {
            msgRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, msgRef.path)
            throw e
        }
    }

    // --- VOICE & VIDEO CALL SIGNALING ---

    fun observeUserCalls(userId: String): Flow<List<CallSession>> {
        val path = "calls"
        return db.collection("calls")
            .whereArrayContains("participantUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun startVoiceOrVideoCall(
        callerName: String,
        callerBadge: String,
        calleeId: String,
        calleeName: String,
        callType: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val callRef = db.collection("calls").document()
        val participants = listOf(uid, calleeId).distinct()
        val payload = mapOf(
            "callerId" to uid,
            "callerName" to callerName.take(80),
            "callerBadge" to callerBadge.take(30),
            "calleeId" to calleeId,
            "calleeName" to calleeName.take(80),
            "participantUids" to participants,
            "callType" to if (callType == "VOICE") "VOICE" else "VIDEO",
            "status" to "RINGING",
            "encryptionFingerprint" to TacticalCryptoEngine.generateCallFingerprint(uid, calleeId),
            "isCallerMuted" to false,
            "isCalleeMuted" to false,
            "isVideoEnabled" to (callType == "VIDEO"),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            callRef.set(payload).await()
            callRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, callRef.path)
            throw e
        }
    }

    suspend fun updateCallSessionState(
        callId: String,
        status: String,
        isCallerMuted: Boolean,
        isCalleeMuted: Boolean,
        isVideoEnabled: Boolean
    ): Result<Unit> = runCatching {
        requireUserId()
        val callRef = db.collection("calls").document(callId)
        try {
            callRef.update(
                mapOf(
                    "status" to status,
                    "isCallerMuted" to isCallerMuted,
                    "isCalleeMuted" to isCalleeMuted,
                    "isVideoEnabled" to isVideoEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, callRef.path)
            throw e
        }
    }

    // --- FIREBASE CLOUD MESSAGING (FCM) & REAL-TIME PUSH NOTIFICATIONS ---

    suspend fun registerFcmDeviceToken(fcmToken: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanToken = fcmToken.trim().take(512)
        if (cleanToken.isEmpty()) return@runCatching
        val docRef = db.collection("fcm_tokens").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "fcmToken" to cleanToken,
            "platform" to "ANDROID",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeIncomingPushNotifications(userId: String): Flow<List<PushNotificationEvent>> {
        val path = "notifications"
        return db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PushNotificationEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { !it.isDelivered }
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun markPushNotificationDelivered(notificationId: String): Result<Unit> = runCatching {
        requireUserId()
        if (notificationId.isBlank()) return@runCatching
        val docRef = db.collection("notifications").document(notificationId)
        try {
            docRef.update(
                mapOf(
                    "isDelivered" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    private suspend fun dispatchPushNotificationEvent(
        recipientId: String,
        senderId: String,
        senderName: String,
        senderBadge: String,
        type: String,
        title: String,
        body: String,
        targetId: String
    ) {
        if (recipientId.isBlank() || senderId.isBlank() || recipientId == senderId) return
        val notifRef = db.collection("notifications").document()
        val payload = mapOf(
            "recipientId" to recipientId.take(128),
            "senderId" to senderId.take(128),
            "senderName" to senderName.ifBlank { "Cadet" }.take(80),
            "senderBadge" to senderBadge.take(40),
            "type" to type,
            "title" to title.ifBlank { "Geajao Notification" }.take(120),
            "body" to body.ifBlank { "New activity on Geajao" }.take(300),
            "targetId" to targetId.take(128),
            "isDelivered" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            notifRef.set(payload).await()
        } catch (e: Exception) {
            // Non-fatal if notification dispatch fails
        }
    }
}
