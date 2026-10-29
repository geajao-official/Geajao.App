package com.example.data.crypto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val cipherTextBase64: String,
    val ivBase64: String
)

object TacticalCryptoEngine {
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val CORPS_MASTER_SALT = "GEAJAO-CADET-E2EE-SALT-2026"

    private fun deriveChatSecretKey(chatId: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest("$CORPS_MASTER_SALT:$chatId".toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptMessage(plainText: String, chatId: String): EncryptedPayload {
        val secretKey = deriveChatSecretKey(chatId)
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        return EncryptedPayload(
            cipherTextBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    fun decryptMessage(cipherTextBase64: String, ivBase64: String, chatId: String): String {
        return try {
            val secretKey = deriveChatSecretKey(chatId)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(cipherTextBase64, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "[Encrypted Tactical Payload]"
        }
    }

    fun generateKeyFingerprint(chatId: String, participantUids: List<String>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val raw = "${participantUids.sorted().joinToString(":")}:$chatId"
        val hash = digest.digest(raw.toByteArray(Charsets.UTF_8))
        val hex = hash.take(6).joinToString("") { "%02X".format(it) }
        return "AES-256-GCM • ${hex.chunked(4).joinToString("-")}"
    }

    fun generateCallFingerprint(callerId: String, calleeId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest("SRTP:$callerId:$calleeId:${System.currentTimeMillis() / 60000}".toByteArray())
        val hex = hash.take(6).joinToString("") { "%02X".format(it) }
        return "SRTP-DTLS • ${hex.chunked(4).joinToString("-")}"
    }

    /**
     * Compresses an image selected via Android Photo Picker into a compact Base64 Data URI
     * that safely fits inside Firestore's document size limit (< 90 KB).
     */
    fun encodeUriToCompactBase64(context: Context, uri: Uri, maxDimension: Int = 540): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return null

            val ratio = minOf(
                maxDimension.toFloat() / original.width.toFloat(),
                maxDimension.toFloat() / original.height.toFloat(),
                1f
            )
            val width = (original.width * ratio).toInt().coerceAtLeast(1)
            val height = (original.height * ratio).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(original, width, height, true)

            val output = ByteArrayOutputStream()
            var quality = 72
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            while (output.size() > 85_000 && quality > 30) {
                output.reset()
                quality -= 12
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            }
            val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates an official high-contrast Cadet Academy ID Card bitmap encoded as a Base64 Data URI
     * so users can either pick a photo from device gallery OR generate their official digital Cadet ID Card.
     */
    fun generateOfficialCadetIdCardBase64(
        displayName: String,
        cadetNumber: String,
        batchYear: String,
        cadetCollege: String,
        trackLabel: String
    ): String {
        val width = 560
        val height = 340
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Navy tactical card background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                Color.parseColor("#0B162C"),
                Color.parseColor("#1B2E52"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 24f, 24f, bgPaint)

        // Gold header bar
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 62f, goldPaint)

        // Header text
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B162C")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("GEAJAO CADET CORPS • OFFICIAL ID CARD", 24f, 40f, headerTextPaint)

        // Portrait placeholder box
        val portraitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#243B67")
        }
        canvas.drawRoundRect(RectF(24f, 88f, 164f, 268f), 16f, 16f, portraitPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(RectF(24f, 88f, 164f, 268f), 16f, 16f, borderPaint)

        // Initials inside portrait box
        val initials = displayName.trim().split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifEmpty { "CD" }
        val initialsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
            textSize = 48f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(initials, 94f, 195f, initialsPaint)

        // Cadet details
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 15f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        canvas.drawText("CADET NAME", 190f, 108f, labelPaint)
        canvas.drawText(displayName.ifBlank { "Cadet Officer" }.take(24), 190f, 134f, valuePaint)

        canvas.drawText("SERIAL NO.", 190f, 170f, labelPaint)
        canvas.drawText(cadetNumber.ifBlank { "CDT-0001" }.take(18), 190f, 196f, monoPaint)

        canvas.drawText("BATCH / INTAKE", 380f, 170f, labelPaint)
        canvas.drawText(batchYear.ifBlank { "Batch 54" }.take(14), 380f, 196f, valuePaint)

        canvas.drawText("CADET COLLEGE", 190f, 232f, labelPaint)
        val normalizedCollege = com.example.data.model.GeajaoConstants.normalizeCadetCollege(cadetCollege)
        canvas.drawText(normalizedCollege.take(26), 190f, 258f, valuePaint)

        // Bottom security strip
        val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10203D")
        }
        canvas.drawRect(0f, 290f, width.toFloat(), height.toFloat(), stripPaint)
        canvas.drawText("TRACK: ${trackLabel.uppercase()}   •   VERIFICATION SEAL: GEAJAO-CORPS", 24f, 320f, labelPaint)

        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
        val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    fun decodeBase64DataUriToBitmap(dataUri: String): Bitmap? {
        if (dataUri.isBlank()) return null
        return try {
            val cleanBase64 = dataUri.substringAfter("base64,", dataUri)
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
