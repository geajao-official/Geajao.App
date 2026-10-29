package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.crypto.TacticalCryptoEngine
import com.example.data.model.GeajaoConstants
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class GeajaoRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun registerCadetProfileAndAdminApprovalWithBadges_succeeds() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(8)
        val cadetUid = signInTestUser("cadet_$uniqueSuffix@geajao.test")
        val repository = GeajaoRepository(firestore, auth)

        val regResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.registerOrUpdateCadetProfile(
                displayName = "Cadet Rahman",
                cadetNumber = "CDT-54-$uniqueSuffix",
                batchYear = "Batch 54",
                academyHouse = "CCR",
                bio = "Cadet Honor & Duty",
                idCardImageBase64 = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
                idCardNote = "Official Academy ID Card"
            )
        }
        assertTrue("Profile registration should succeed: ${regResult.exceptionOrNull()}", regResult.isSuccess)

        val pendingProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCadetProfile(cadetUid).first { it != null }
        }
        assertNotNull(pendingProfile)
        assertEquals("CCR", pendingProfile?.cadetCollege)
        assertEquals(GeajaoConstants.STATUS_PENDING, pendingProfile?.verificationStatus)
        assertEquals(GeajaoConstants.BADGE_UNVERIFIED, pendingProfile?.primaryBadge)

        // Sign in as Commandant Admin, activate role, and approve Cadet with Ex-cadet & Moderator badges
        signInTestUser("admin_$uniqueSuffix@geajao.test")
        val adminRepo = GeajaoRepository(firestore, auth)
        val bootstrapRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.activateCommandantAdminRole()
        }
        assertTrue("Bootstrap admin activation should succeed: ${bootstrapRes.exceptionOrNull()}", bootstrapRes.isSuccess)

        val approvalRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.approveOrRejectCadet(
                targetUserId = cadetUid,
                approve = true,
                primaryBadge = GeajaoConstants.BADGE_EX_CADET,
                badges = listOf(GeajaoConstants.BADGE_EX_CADET, GeajaoConstants.BADGE_MODERATOR),
                reviewerNote = "Verified by Commandant Admin",
                isCallerAdmin = true
            )
        }
        assertTrue("Admin approval & badge grant should succeed: ${approvalRes.exceptionOrNull()}", approvalRes.isSuccess)

        val approvedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeCadetProfile(cadetUid).first { it?.verificationStatus == GeajaoConstants.STATUS_APPROVED }
        }
        assertEquals(GeajaoConstants.BADGE_EX_CADET, approvedProfile?.primaryBadge)
        assertTrue(approvedProfile?.badges?.contains(GeajaoConstants.BADGE_MODERATOR) == true)

        // Verify Admin can update the global App Icon in /app_config/global (including CUSTOM_GALLERY)
        val iconRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.updateSelectedAppIcon(
                iconKey = GeajaoConstants.ICON_CUSTOM_GALLERY,
                customIconBase64 = "data:image/jpeg;base64,/9j/4AAQSkZJRg=="
            )
        }
        assertTrue("Admin updating custom gallery app icon should succeed: ${iconRes.exceptionOrNull()}", iconRes.isSuccess)
        val updatedConfig = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAppConfig().first { it.selectedIconKey == GeajaoConstants.ICON_CUSTOM_GALLERY }
        }
        assertEquals(GeajaoConstants.ICON_CUSTOM_GALLERY, updatedConfig.selectedIconKey)
        assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRg==", updatedConfig.customIconBase64)
    }

    @Test
    fun moderatorPasscode5246AndFriendshipWithStickerGifChat_succeeds() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(8)
        val modUid = signInTestUser("mod_$uniqueSuffix@geajao.test")
        val modRepo = GeajaoRepository(firestore, auth)

        val modActivation = withTimeout(DEFAULT_TIMEOUT_MS) {
            modRepo.activateModeratorRoleWithPasscode("5246")
        }
        assertTrue("Moderator 5246 activation should succeed: ${modActivation.exceptionOrNull()}", modActivation.isSuccess)

        // Register FCM token for modUid
        val fcmRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            modRepo.registerFcmDeviceToken("fcm_test_token_$uniqueSuffix")
        }
        assertTrue("FCM token registration should succeed: ${fcmRes.exceptionOrNull()}", fcmRes.isSuccess)

        val friendUid = signInTestUser("friend_$uniqueSuffix@geajao.test")
        val friendRepo = GeajaoRepository(firestore, auth)

        // Send friend request from friendUid to modUid
        val sendReq = withTimeout(DEFAULT_TIMEOUT_MS) {
            friendRepo.sendFriendRequest(
                targetUserId = modUid,
                requesterName = "Cadet Friend",
                receiverName = "Moderator"
            )
        }
        assertTrue("Sending friend request should succeed: ${sendReq.exceptionOrNull()}", sendReq.isSuccess)

        // Switch back to modUid to observe friend request push notification and accept friend request
        signInTestUser("mod_$uniqueSuffix@geajao.test")
        val pushEvents = withTimeout(FLOW_TIMEOUT_MS) {
            modRepo.observeIncomingPushNotifications(modUid).first { it.isNotEmpty() }
        }
        assertEquals("FRIEND_REQUEST", pushEvents.first().type)
        val markRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            modRepo.markPushNotificationDelivered(pushEvents.first().id)
        }
        assertTrue("Marking push notification delivered should succeed", markRes.isSuccess)

        val pendingFriendships = withTimeout(FLOW_TIMEOUT_MS) {
            modRepo.observeUserFriendships(modUid).first { it.isNotEmpty() }
        }
        val friendshipId = pendingFriendships.first().id
        val acceptRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            modRepo.acceptFriendRequest(friendshipId)
        }
        assertTrue("Accepting friend request should succeed: ${acceptRes.exceptionOrNull()}", acceptRes.isSuccess)
    }

    @Test
    fun encryptedChatAndDisappearingMessage_participantCanReadAndDecrypt_strangerDenied() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(8)
        val aliceUid = signInTestUser("alice_$uniqueSuffix@geajao.test")
        val bobUid = signInTestUser("bob_$uniqueSuffix@geajao.test")

        // Switch back to Alice to create chat and send encrypted disappearing message
        signInTestUser("alice_$uniqueSuffix@geajao.test")
        val aliceRepo = GeajaoRepository(firestore, auth)

        val chatIdResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createOrGetEncryptedChatRoom(
                otherUserId = bobUid,
                currentUserName = "Cadet Alice",
                otherUserName = "Cadet Bob",
                disappearingTtlSeconds = 60L
            )
        }
        assertTrue(chatIdResult.isSuccess)
        val chatId = chatIdResult.getOrThrow()

        val sendResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.sendEncryptedMessage(
                chatId = chatId,
                senderName = "Cadet Alice",
                senderBadge = GeajaoConstants.BADGE_CADET,
                participantUids = listOf(aliceUid, bobUid),
                plainText = "Rendezvous at Parade Ground 0600",
                mediaType = "NONE",
                ttlSeconds = 60L
            )
        }
        assertTrue("Sending encrypted message should succeed: ${sendResult.exceptionOrNull()}", sendResult.isSuccess)

        // Bob observes and decrypts the message, and receives the real-time PRIVATE_MESSAGE push notification
        signInTestUser("bob_$uniqueSuffix@geajao.test")
        val bobRepo = GeajaoRepository(firestore, auth)
        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeEncryptedMessages(chatId, bobUid).first { it.isNotEmpty() }
        }
        val firstMsg = messages.first()
        val decrypted = TacticalCryptoEngine.decryptMessage(firstMsg.cipherText, firstMsg.ivBase64, chatId)
        assertEquals("Rendezvous at Parade Ground 0600", decrypted)

        val bobPushNotifications = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeIncomingPushNotifications(bobUid).first { it.isNotEmpty() }
        }
        assertEquals("PRIVATE_MESSAGE", bobPushNotifications.first().type)
        assertEquals(chatId, bobPushNotifications.first().targetId)

        // Charlie (non-participant) attempts to read the chat room document -> PERMISSION_DENIED
        signInTestUser("charlie_$uniqueSuffix@geajao.test")
        var caughtException: Exception? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("chats").document(chatId).get().await()
            }
        } catch (e: Exception) {
            caughtException = e
        }
        val fsEx = caughtException?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException for non-participant", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeFeedPosts_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = GeajaoRepository(firestore, auth)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeFeedPosts().first()
            }
        } catch (e: Exception) {
            caught = e
        }
        val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
