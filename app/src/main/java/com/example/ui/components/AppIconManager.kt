package com.example.ui.components

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.crypto.TacticalCryptoEngine
import com.example.data.model.GeajaoConstants
import com.example.ui.theme.BrassGold
import com.example.ui.theme.CadetNavyCard
import com.example.ui.theme.CadetNavyDeep
import com.example.ui.theme.CyanTactical
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

object AppIconManager {

    private val aliasMap = mapOf(
        GeajaoConstants.ICON_CRIMSON_STAR to "com.example.LauncherCrimsonStar",
        GeajaoConstants.ICON_EMERALD_EAGLE to "com.example.LauncherEmeraldEagle",
        GeajaoConstants.ICON_CYBER_STEALTH to "com.example.LauncherCyberStealth"
    )

    fun drawableResForIconKey(iconKey: String): Int = when (iconKey) {
        GeajaoConstants.ICON_CRIMSON_STAR -> R.drawable.ic_emblem_crimson
        GeajaoConstants.ICON_EMERALD_EAGLE -> R.drawable.ic_emblem_emerald
        GeajaoConstants.ICON_CYBER_STEALTH -> R.drawable.ic_emblem_stealth
        else -> R.drawable.img_launcher_icon_1793207827419
    }

    /**
     * Switches the active Android launcher `<activity-alias>` when an Admin selects a new App Icon,
     * while keeping `MainActivity` intact so the streaming emulator never loses its entry point.
     */
    fun applyDeviceLauncherIconAlias(context: Context, selectedIconKey: String) {
        try {
            val pm = context.packageManager
            val pkg = context.packageName
            aliasMap.forEach { (key, className) ->
                val state = if (key == selectedIconKey) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                pm.setComponentEnabledSetting(
                    ComponentName(pkg, className),
                    state,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (_: Exception) {
            // Ignore in headless unit tests
        }
    }
}

object MukhoshLogoPreferences {
    private const val PREFS_NAME = "geajao_mukhosh_logo_prefs"
    private const val KEY_MUKHOSH_LOGO_BASE64 = "mukhosh_logo_base64"

    private val _localMukhoshLogoFlow = MutableStateFlow("")
    val localMukhoshLogoFlow: StateFlow<String> = _localMukhoshLogoFlow

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_MUKHOSH_LOGO_BASE64, "").orEmpty()
            if (saved.isNotBlank()) {
                _localMukhoshLogoFlow.value = saved
            }
        } catch (_: Exception) {
        }
    }

    fun saveLocalMukhoshLogo(context: Context, base64DataUri: String) {
        val trimmed = base64DataUri.take(150000)
        _localMukhoshLogoFlow.value = trimmed
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_MUKHOSH_LOGO_BASE64, trimmed).apply()
        } catch (_: Exception) {
        }
    }
}

@Composable
fun GeajaoAppIconEmblem(
    iconKey: String,
    customIconBase64: String = "",
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val customBitmap = androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = iconKey,
        key2 = customIconBase64
    ) {
        value = if (iconKey == GeajaoConstants.ICON_CUSTOM_GALLERY && customIconBase64.isNotBlank()) {
            withContext(Dispatchers.Default) {
                TacticalCryptoEngine.decodeBase64DataUriToBitmap(customIconBase64)?.asImageBitmap()
            }
        } else {
            null
        }
    }.value

    if (customBitmap != null) {
        Image(
            bitmap = customBitmap,
            contentDescription = "Geajao Custom Gallery Emblem",
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        val resId = AppIconManager.drawableResForIconKey(iconKey)
        Image(
            painter = painterResource(id = resId),
            contentDescription = "Geajao Emblem ($iconKey)",
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Renders the MUKHOSH brand logo:
 * - If a custom MUKHOSH logo has been uploaded from the gallery (either synced in Firestore `mukhoshLogoBase64`
 *   or stored locally in `MukhoshLogoPreferences`), renders that uploaded image just like the main Geajao logo.
 * - Otherwise, renders a custom vector-drawn MUKHOSH tactical crest ("M" shield emblem).
 */
@Composable
fun MukhoshLogoEmblem(
    mukhoshLogoBase64: String = "",
    size: Dp = 34.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localLogo by MukhoshLogoPreferences.localMukhoshLogoFlow.collectAsStateWithLifecycle()

    LaunchedEffect(mukhoshLogoBase64) {
        if (mukhoshLogoBase64.isNotBlank()) {
            MukhoshLogoPreferences.saveLocalMukhoshLogo(context, mukhoshLogoBase64)
        }
    }

    val effectiveBase64 = if (mukhoshLogoBase64.isNotBlank()) mukhoshLogoBase64 else localLogo

    val customBitmap = androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = effectiveBase64
    ) {
        value = if (effectiveBase64.isNotBlank()) {
            withContext(Dispatchers.Default) {
                TacticalCryptoEngine.decodeBase64DataUriToBitmap(effectiveBase64)?.asImageBitmap()
            }
        } else {
            null
        }
    }.value

    if (customBitmap != null) {
        Image(
            bitmap = customBitmap,
            contentDescription = "MUKHOSH Custom Uploaded Logo",
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(CadetNavyDeep, CadetNavyCard)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height
                // Outer shield crest border
                val shieldPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.08f)
                    lineTo(w * 0.88f, h * 0.22f)
                    lineTo(w * 0.84f, h * 0.64f)
                    lineTo(w * 0.5f, h * 0.92f)
                    lineTo(w * 0.16f, h * 0.64f)
                    lineTo(w * 0.12f, h * 0.22f)
                    close()
                }
                drawPath(
                    path = shieldPath,
                    color = BrassGold.copy(alpha = 0.22f)
                )
                drawPath(
                    path = shieldPath,
                    color = BrassGold,
                    style = Stroke(width = w * 0.06f)
                )

                // Stylized "M" Mukhosh mask/monogram inside shield
                val mPath = Path().apply {
                    moveTo(w * 0.27f, h * 0.68f)
                    lineTo(w * 0.27f, h * 0.34f)
                    lineTo(w * 0.5f, h * 0.54f)
                    lineTo(w * 0.73f, h * 0.34f)
                    lineTo(w * 0.73f, h * 0.68f)
                }
                drawPath(
                    path = mPath,
                    color = BrassGold,
                    style = Stroke(width = w * 0.085f)
                )
                drawCircle(
                    color = CyanTactical,
                    radius = w * 0.05f,
                    center = Offset(w * 0.5f, h * 0.26f)
                )
            }
        }
    }
}

/**
 * Compact `Made By MUKHOSH` tag pill with the MUKHOSH logo.
 * Read-only on general pages; only clickable if an Admin upload handler is explicitly supplied.
 */
@Composable
fun MadeByMukhoshBadge(
    mukhoshLogoBase64: String = "",
    onUploadMukhoshLogo: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && onUploadMukhoshLogo != null) {
            TacticalCryptoEngine.encodeUriToCompactBase64(context, uri)?.let { encoded ->
                MukhoshLogoPreferences.saveLocalMukhoshLogo(context, encoded)
                onUploadMukhoshLogo.invoke(encoded)
            }
        }
    }

    val clickableModifier = if (onUploadMukhoshLogo != null) {
        modifier.clickable {
            logoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    } else {
        modifier
    }

    Surface(
        modifier = clickableModifier.testTag("made_by_mukhosh_tag"),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MukhoshLogoEmblem(
                mukhoshLogoBase64 = mukhoshLogoBase64,
                size = 22.dp
            )
            Text(
                text = GeajaoConstants.MADE_BY_MUKHOSH_TAG,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * `Made By MUKHOSH` brand card with logo preview.
 * When `isAdminEditable = true` (in the Admin Page), displays gallery upload, change, and reset controls
 * just like the main Geajao App Icon manager.
 */
@Composable
fun MadeByMukhoshBrandCard(
    mukhoshLogoBase64: String = "",
    onUploadMukhoshLogo: ((String) -> Unit)? = null,
    isAdminEditable: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localLogo by MukhoshLogoPreferences.localMukhoshLogoFlow.collectAsStateWithLifecycle()
    val effectiveLogo = if (mukhoshLogoBase64.isNotBlank()) mukhoshLogoBase64 else localLogo

    val mukhoshGalleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            TacticalCryptoEngine.encodeUriToCompactBase64(context, uri)?.let { encoded ->
                MukhoshLogoPreferences.saveLocalMukhoshLogo(context, encoded)
                onUploadMukhoshLogo?.invoke(encoded)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("made_by_mukhosh_brand_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BrassGold.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isAdminEditable) {
                Text(
                    text = "ADMIN MUKHOSH LOGO MANAGER (GALLERY UPLOAD)",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    MukhoshLogoEmblem(
                        mukhoshLogoBase64 = effectiveLogo,
                        size = 52.dp,
                        modifier = Modifier.testTag("mukhosh_logo_emblem_preview")
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = GeajaoConstants.MADE_BY_MUKHOSH_TAG,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Creator Tag",
                                tint = BrassGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = if (isAdminEditable) {
                                "Admin Control • Upload or change the MUKHOSH logo from your gallery"
                            } else {
                                "Official Creator Signature • MUKHOSH"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (effectiveLogo.isNotBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text("CUSTOM LOGO") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Custom MUKHOSH Logo Active",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            if (isAdminEditable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            mukhoshGalleryPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_mukhosh_logo_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Change MUKHOSH Logo",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (effectiveLogo.isBlank()) {
                                "Upload MUKHOSH Logo"
                            } else {
                                "Change MUKHOSH Logo"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (effectiveLogo.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                MukhoshLogoPreferences.saveLocalMukhoshLogo(context, "")
                                onUploadMukhoshLogo?.invoke("")
                            },
                            modifier = Modifier.testTag("reset_mukhosh_logo_button")
                        ) {
                            Text("Reset")
                        }
                    }
                }
            }
        }
    }
}


