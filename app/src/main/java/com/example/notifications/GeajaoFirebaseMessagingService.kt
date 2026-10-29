package com.example.notifications

import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging Service that receives real-time push notifications
 * for incoming private messages and friend requests even when the app is in the background.
 */
class GeajaoFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM registration token refreshed")
        GeajaoNotificationHelper.saveCachedFcmToken(applicationContext, token)

        val currentUid = try {
            Firebase.auth.currentUser?.uid
        } catch (e: Exception) {
            null
        }
        if (!currentUid.isNullOrBlank()) {
            syncTokenToFirestore(currentUid, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM RemoteMessage received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notificationType = data["type"] ?: "PRIVATE_MESSAGE"
        val senderName = data["senderName"]
            ?: remoteMessage.notification?.title
            ?: "Cadet Transmission"
        val senderBadge = data["senderBadge"].orEmpty()
        val body = data["body"]
            ?: remoteMessage.notification?.body
            ?: "New activity on Geajao"

        when (notificationType) {
            "FRIEND_REQUEST" -> {
                val friendshipId = data["targetId"] ?: data["friendshipId"] ?: System.currentTimeMillis().toString()
                val isAccepted = data["isAccepted"] == "true"
                GeajaoNotificationHelper.showFriendRequestNotification(
                    context = applicationContext,
                    requesterName = senderName,
                    friendshipId = friendshipId,
                    isAccepted = isAccepted
                )
            }
            else -> {
                val chatId = data["targetId"] ?: data["chatId"] ?: ""
                GeajaoNotificationHelper.showPrivateMessageNotification(
                    context = applicationContext,
                    senderName = senderName,
                    senderBadge = senderBadge,
                    messagePreview = body,
                    chatId = chatId,
                    notificationIdSeed = remoteMessage.messageId ?: chatId.ifBlank { System.currentTimeMillis().toString() }
                )
            }
        }
    }

    private fun syncTokenToFirestore(userId: String, token: String) {
        try {
            val dbId = applicationContext.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(Firebase.app, dbId)
            val payload = mapOf(
                "userId" to userId,
                "fcmToken" to token.take(512),
                "platform" to "ANDROID",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("fcm_tokens").document(userId).set(payload)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync refreshed FCM token to Firestore", e)
        }
    }

    private companion object {
        const val TAG = "GeajaoFcmService"
    }
}
