package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

object GeajaoNotificationHelper {
    private const val TAG = "GeajaoNotifications"

    const val CHANNEL_PRIVATE_MESSAGES = "geajao_private_messages"
    const val CHANNEL_FRIEND_REQUESTS = "geajao_friend_requests"

    const val EXTRA_NAVIGATE_TAB = "extra_geajao_navigate_tab"
    const val EXTRA_TARGET_CHAT_ID = "extra_geajao_target_chat_id"
    const val TAB_TARGET_CHAT = "CHAT"
    const val TAB_TARGET_FRIENDS = "FRIENDS"

    private const val PREFS_FCM = "geajao_fcm_prefs"
    private const val KEY_CACHED_FCM_TOKEN = "cached_fcm_token"

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val messageChannel = NotificationChannel(
                CHANNEL_PRIVATE_MESSAGES,
                "Encrypted Private Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time push notifications for new incoming encrypted private messages from Friends."
                enableVibration(true)
            }

            val friendChannel = NotificationChannel(
                CHANNEL_FRIEND_REQUESTS,
                "Friend Requests & Connections",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time push notifications for new incoming Cadet & Ex-Cadet friend requests."
                enableVibration(true)
            }

            manager.createNotificationChannel(messageChannel)
            manager.createNotificationChannel(friendChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun saveCachedFcmToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CACHED_FCM_TOKEN, token)
            .apply()
    }

    fun getCachedFcmToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE)
            .getString(KEY_CACHED_FCM_TOKEN, null)
    }

    suspend fun fetchCurrentFcmToken(context: Context, userId: String): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (!token.isNullOrBlank()) {
                saveCachedFcmToken(context, token)
                // Subscribe to personal user topic so FCM topic pushes also reach this device in background
                val sanitizedUid = userId.replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
                FirebaseMessaging.getInstance().subscribeToTopic("geajao_user_$sanitizedUid")
            }
            token
        } catch (e: Exception) {
            Log.w(TAG, "Unable to fetch FCM registration token", e)
            getCachedFcmToken(context)
        }
    }

    fun showPrivateMessageNotification(
        context: Context,
        senderName: String,
        senderBadge: String,
        messagePreview: String,
        chatId: String,
        notificationIdSeed: String = chatId
    ) {
        ensureNotificationChannels(context)
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted; skipping system notification.")
            return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TAB, TAB_TARGET_CHAT)
            putExtra(EXTRA_TARGET_CHAT_ID, chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIdSeed.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val badgeLabel = if (senderBadge.isNotBlank()) " [$senderBadge]" else ""
        val title = "$senderName$badgeLabel"
        val body = messagePreview.ifBlank { "Sent you an encrypted private message" }

        val notification = NotificationCompat.Builder(context, CHANNEL_PRIVATE_MESSAGES)
            .setSmallIcon(R.drawable.ic_emblem_crimson)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                notificationIdSeed.hashCode().takeIf { it != 0 } ?: Random.nextInt(1000, 99999),
                notification
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException posting private message notification", e)
        }
    }

    fun showFriendRequestNotification(
        context: Context,
        requesterName: String,
        friendshipId: String,
        isAccepted: Boolean = false
    ) {
        ensureNotificationChannels(context)
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted; skipping friend request notification.")
            return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TAB, TAB_TARGET_FRIENDS)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            friendshipId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isAccepted) {
            "Friend Request Accepted"
        } else {
            "New Cadet Friend Request"
        }
        val body = if (isAccepted) {
            "$requesterName accepted your friend request! You can now start an encrypted chat."
        } else {
            "$requesterName sent you a friend request on Geajao. Tap to review and connect."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_FRIEND_REQUESTS)
            .setSmallIcon(R.drawable.ic_emblem_emerald)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                friendshipId.hashCode().takeIf { it != 0 } ?: Random.nextInt(100000, 199999),
                notification
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException posting friend request notification", e)
        }
    }
}
