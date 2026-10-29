package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.data.crypto.TacticalCryptoEngine
import com.example.data.model.CadetProfile
import com.example.data.model.GeajaoConstants
import com.example.ui.components.Base64ImagePreview
import com.example.ui.components.CadetRoleBadgePill
import com.example.ui.components.GeajaoAppIconEmblem
import com.example.ui.components.MadeByMukhoshBrandCard
import com.example.ui.components.OfficialContactAndGuideCard
import com.example.ui.components.VerificationStatusChip
import com.example.ui.theme.BrassGold
import com.example.ui.theme.CadetNavyCard
import com.example.ui.theme.CadetNavyDeep
import com.example.ui.theme.CrimsonHonor
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val primaryRequest = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = try {
                credentialManager.getCredential(activityContext, primaryRequest)
            } catch (noCred: NoCredentialException) {
                val fallbackGoogleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(fallbackGoogleIdOption)
                    .build()
                credentialManager.getCredential(activityContext, fallbackRequest)
            }
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}")
            onAuthCancelled()
        } catch (e: CancellationException) {
            // Coroutine scope cancelled (e.g. screen transitioned after AuthStateListener fired); do not log as error
            throw e
        } catch (e: NoCredentialException) {
            Log.w("Auth", "No Google account available on device: ${e.message}")
            onAuthError("No Google account found on this device. Please sign into a Google account in your device Settings > Accounts and try again.")
        } catch (e: Exception) {
            Log.w("Auth", "Google Sign-In did not complete: ${e.message}")
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutCadet(
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Auth", "Could not clear credential state: ${e.message}")
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun GeajaoSignInScreen(
    onAuthSuccess: () -> Unit,
    onOpenUserGuide: () -> Unit = {},
    externalScope: CoroutineScope? = null
) {
    val context = LocalContext.current
    val localScope = rememberCoroutineScope()
    val scope = externalScope ?: localScope
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val heroBitmap by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = Unit
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            android.graphics.BitmapFactory.decodeResource(
                context.resources,
                R.drawable.img_cadet_hero_1793207828313
            )?.asImageBitmap()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CadetNavyCard)
            ) {
                if (heroBitmap != null) {
                    Image(
                        bitmap = heroBitmap!!,
                        contentDescription = "Geajao Cadet Academy Courtyard",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, CadetNavyDeep.copy(alpha = 0.92f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Surface(
                        color = BrassGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, BrassGold)
                    ) {
                        Text(
                            text = "RESTRICTED • CADETS & EX-CADETS ONLY",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrassGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "GEAJAO",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.auth_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = BrassGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cadet Social Network Authentication",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Every member is verified via official Cadet ID Card inspection by an Admin or Moderator before receiving their Cadet or Ex-cadet badge.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FeatureHighlightRow(
                        icon = Icons.Default.Badge,
                        title = "Mandatory ID Card Verification",
                        subtitle = "Upload your Cadet ID card for Admin & Moderator approval"
                    )
                    FeatureHighlightRow(
                        icon = Icons.Default.Lock,
                        title = "Friends-Only Encrypted Chat + GIFs & Stickers",
                        subtitle = "AES-256-GCM disappearing messages exclusively with Friends"
                    )
                    FeatureHighlightRow(
                        icon = Icons.Default.MilitaryTech,
                        title = "Role Badges & Voice/Video Calls",
                        subtitle = "Cadet, Ex-cadet, Moderator & Admin badges + live calls"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrassGold,
                            contentColor = CadetNavyDeep
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = CadetNavyDeep,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Sign In",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.sign_in_google),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            MadeByMukhoshBrandCard()

            Spacer(modifier = Modifier.height(14.dp))

            OfficialContactAndGuideCard(
                onOpenUserGuide = onOpenUserGuide
            )
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Cadet ID Card Verification & Verifying Page Gate.
 * Without publishing an ID card and receiving approval, the user remains on this Verifying Page.
 * Clicking the Geajao App Icon 6 times opens the secret Auth Passcode prompt (5246 -> Moderator, A54h -> Admin).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CadetIdVerificationGateScreen(
    existingProfile: CadetProfile?,
    selectedAppIconKey: String,
    customIconBase64: String,
    mukhoshLogoBase64: String = "",
    onUploadMukhoshLogo: (String) -> Unit = {},
    onSubmitProfile: (
        displayName: String,
        cadetNumber: String,
        batchYear: String,
        cadetCollege: String,
        bio: String,
        idCardBase64: String,
        idCardNote: String,
        cadetStatusBadge: String
    ) -> Unit,
    onOpenSecretPasscodePrompt: () -> Unit,
    onSignOut: () -> Unit,
    onOpenUserGuide: () -> Unit = {}
) {
    val context = LocalContext.current
    var iconTapCount by remember { mutableIntStateOf(0) }

    val hasPublishedIdCard = existingProfile != null && existingProfile.idCardImageBase64.isNotBlank()
    var isEditingSubmission by remember(hasPublishedIdCard) {
        mutableStateOf(!hasPublishedIdCard)
    }

    var displayName by remember(existingProfile) {
        mutableStateOf(existingProfile?.displayName ?: "")
    }
    var cadetNumber by remember(existingProfile) {
        mutableStateOf(existingProfile?.cadetNumber ?: "")
    }
    var batchYear by remember(existingProfile) {
        mutableStateOf(existingProfile?.batchYear ?: "")
    }
    var selectedCollege by remember(existingProfile) {
        mutableStateOf(existingProfile?.cadetCollege ?: GeajaoConstants.CADET_COLLEGES.first())
    }
    var selectedCadetStatus by remember(existingProfile) {
        mutableStateOf(existingProfile?.cadetStatusBadge ?: GeajaoConstants.BADGE_CADET)
    }
    var bio by remember(existingProfile) {
        mutableStateOf(existingProfile?.bio ?: "")
    }
    var idCardNote by remember(existingProfile) {
        mutableStateOf(existingProfile?.idCardNote ?: "")
    }
    var idCardBase64 by remember(existingProfile) {
        mutableStateOf(existingProfile?.idCardImageBase64 ?: "")
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = TacticalCryptoEngine.encodeUriToCompactBase64(context, uri)
            if (encoded != null) {
                idCardBase64 = encoded
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 580.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Status Header — Click App Icon 6 times to open Auth Passcode Dialog
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GeajaoAppIconEmblem(
                        iconKey = selectedAppIconKey,
                        customIconBase64 = customIconBase64,
                        size = 44.dp,
                        modifier = Modifier
                            .clickable {
                                iconTapCount += 1
                                if (iconTapCount >= GeajaoConstants.ICON_SECRET_TAP_THRESHOLD) {
                                    iconTapCount = 0
                                    onOpenSecretPasscodePrompt()
                                }
                            }
                            .testTag("gate_app_icon_6_tap_trigger")
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (hasPublishedIdCard && !isEditingSubmission) {
                                "GEAJAO • VERIFYING PAGE"
                            } else {
                                "GEAJAO • ID CARD PUBLISHING GATE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (hasPublishedIdCard && !isEditingSubmission) {
                                "Verifying Your Cadet ID Card"
                            } else {
                                "Publish Cadet ID Card"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                VerificationStatusChip(
                    status = existingProfile?.verificationStatus ?: GeajaoConstants.STATUS_PENDING
                )
            }

            // Dedicated Verifying Page View when ID Card is published and awaiting approval
            if (existingProfile != null && existingProfile.idCardImageBase64.isNotBlank() && !isEditingSubmission) {
                val isRejected = existingProfile.verificationStatus == GeajaoConstants.STATUS_REJECTED
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verifying_page_status_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.5.dp,
                        if (isRejected) CrimsonHonor else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = "Verifying ID Card",
                                tint = if (isRejected) CrimsonHonor else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Verification Status: ${existingProfile.verificationStatus}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRejected) CrimsonHonor else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isRejected) {
                                        "Your Cadet ID Card submission requires revision. Please review the note below and re-upload your ID Card."
                                    } else {
                                        "Your Cadet ID Card has been published and is awaiting Moderator or Admin approval."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            CadetRoleBadgePill(
                                badge = existingProfile.primaryBadge,
                                additionalBadges = existingProfile.badges
                            )
                        }

                        HorizontalDivider()

                        Text(
                            text = "Published Cadet Profile & ID Card",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Name: ${existingProfile.displayName}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Cadet Status: ${existingProfile.cadetStatusBadge}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Cadet Number: ${existingProfile.cadetNumber}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Batch / Intake: ${existingProfile.batchYear}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Cadet College: ${existingProfile.cadetCollege}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Base64ImagePreview(
                            dataUri = existingProfile.idCardImageBase64,
                            contentDescription = "Published Cadet ID Card",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )

                        if (existingProfile.idCardNote.isNotBlank()) {
                            Text(
                                text = "Note: ${existingProfile.idCardNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isRejected) CrimsonHonor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isRejected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Access to the Geajao main app is locked until your published Cadet ID Card is verified and approved by an Admin or Moderator.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isEditingSubmission = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_published_id_card_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit / Re-upload ID Card")
                            }
                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier.testTag("gate_sign_out_button")
                            ) {
                                Text("Sign Out")
                            }
                        }
                    }
                }
            } else {
                // Warning banner explaining that publishing an ID card is required before entering the app
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Mandatory ID Card Verification",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ID Card Verification Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "You must upload and publish your official Cadet ID Card and select your Cadet College below. Until your ID Card is published and verified, the app remains on the Verifying Page.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 1. Official Cadet ID Card Upload Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "1. Upload Cadet ID Card Image (Required)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Select a clear image of your official Cadet ID Card from your device gallery, or generate your official digital Cadet ID Card below for Admin or Moderator inspection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Base64ImagePreview(
                            dataUri = idCardBase64,
                            contentDescription = "Uploaded Cadet ID Card",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pick_id_card_photo_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Select ID Card Image", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (idCardBase64.isBlank()) "Select ID Card" else "Replace Image")
                            }

                            OutlinedButton(
                                onClick = {
                                    idCardBase64 = TacticalCryptoEngine.generateOfficialCadetIdCardBase64(
                                        displayName = displayName.ifBlank { "Cadet Officer" },
                                        cadetNumber = cadetNumber.ifBlank { "CDT-101" },
                                        batchYear = batchYear.ifBlank { "Batch 54" },
                                        cadetCollege = selectedCollege,
                                        trackLabel = selectedCadetStatus
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_digital_id_card_button")
                            ) {
                                Icon(Icons.Default.Badge, contentDescription = "Generate Digital ID Card", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Digital ID Card")
                            }
                        }
                    }
                }

                // 2. Cadet Registration Details & Cadet College Selection
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "2. Cadet Details, Status (Cadet / Ex-cadet) & College",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Full Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cadet_name_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = cadetNumber,
                                onValueChange = { cadetNumber = it },
                                label = { Text("Cadet Number") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cadet_number_input")
                            )
                            OutlinedTextField(
                                value = batchYear,
                                onValueChange = { batchYear = it },
                                label = { Text("Batch / Intake") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cadet_batch_input")
                            )
                        }

                        // Cadet / Ex-cadet Status Selection
                        Text(
                            text = "Select Cadet Status: $selectedCadetStatus",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = selectedCadetStatus == GeajaoConstants.BADGE_CADET,
                                onClick = { selectedCadetStatus = GeajaoConstants.BADGE_CADET },
                                label = { Text("a) Cadet") },
                                modifier = Modifier.testTag("gate_status_chip_cadet")
                            )
                            FilterChip(
                                selected = selectedCadetStatus == GeajaoConstants.BADGE_EX_CADET,
                                onClick = { selectedCadetStatus = GeajaoConstants.BADGE_EX_CADET },
                                label = { Text("b) Ex-cadet") },
                                modifier = Modifier.testTag("gate_status_chip_ex_cadet")
                            )
                        }

                        // Cadet Colleges (1..12)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Select Cadet College",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Select Cadet College: $selectedCollege",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cadet_college_selector"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GeajaoConstants.CADET_COLLEGES.forEachIndexed { index, college ->
                                FilterChip(
                                    selected = selectedCollege == college,
                                    onClick = { selectedCollege = college },
                                    label = {
                                        Text("${index + 1}) $college")
                                    },
                                    modifier = Modifier.testTag("college_chip_$college")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Bio (Optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cadet_bio_input")
                        )

                        OutlinedTextField(
                            value = idCardNote,
                            onValueChange = { idCardNote = it },
                            label = { Text("Note for Admin / Moderator (Optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cadet_note_input")
                        )

                        val canSubmit = displayName.isNotBlank() &&
                            cadetNumber.isNotBlank() &&
                            batchYear.isNotBlank() &&
                            selectedCollege.isNotBlank() &&
                            idCardBase64.isNotBlank()

                        Button(
                            onClick = {
                                onSubmitProfile(
                                    displayName,
                                    cadetNumber,
                                    batchYear,
                                    selectedCollege,
                                    bio,
                                    idCardBase64,
                                    idCardNote,
                                    selectedCadetStatus
                                )
                                isEditingSubmission = false
                            },
                            enabled = canSubmit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_id_verification_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Publish ID Card for Verification",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (hasPublishedIdCard) {
                            OutlinedButton(
                                onClick = { isEditingSubmission = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Back to Verifying Status Page")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gate_sign_out_button")
                        ) {
                            Text("Sign Out")
                        }
                    }
                }
            }

            MadeByMukhoshBrandCard(
                mukhoshLogoBase64 = mukhoshLogoBase64,
                isAdminEditable = false
            )

            OfficialContactAndGuideCard(
                onOpenUserGuide = onOpenUserGuide,
                mukhoshLogoBase64 = mukhoshLogoBase64
            )
        }
    }
}

