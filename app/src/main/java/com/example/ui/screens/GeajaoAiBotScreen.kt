package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GeajaoAiMessage
import com.example.data.model.GeajaoAiState
import com.example.ui.theme.BrassGold
import com.example.ui.theme.CrimsonHonor
import com.example.ui.theme.CyanTactical
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun GeajaoAiBotScreen(
    messages: List<GeajaoAiMessage>,
    aiState: GeajaoAiState,
    onSendPrompt: (String) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    showBackButton: Boolean = true
) {
    BackHandler(enabled = showBackButton) {
        onBack()
    }

    var inputPrompt by remember { mutableStateOf("") }
    val isBusy = aiState is GeajaoAiState.Loading || aiState is GeajaoAiState.Streaming

    val quickPrompts = listOf(
        "Cadet College Trivia" to "Tell me an inspiring fact about the 12 Cadet Colleges (CCR, BCC, PCC, RCC, MCC, FCC, JCC, SCC, CCC, FGCC, MGCC, JGCC).",
        "ISSB Prep Guide" to "Give me 5 actionable tips to excel in ISSB leadership and group planning tasks.",
        "Draft Feed Post" to "Write a short, motivating post for fellow Cadets and Ex-Cadets on Geajao.",
        "Fitness & Drill" to "Create a 20-minute Cadet morning PT workout for stamina and discipline.",
        "Expressive Chat Reply" to "Give me 3 witty and respectful Cadet replies I can send to a batchmate in chat."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("geajao_ai_bot_screen")
    ) {
        // Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (showBackButton) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("geajao_ai_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = CyanTactical.copy(alpha = 0.2f),
                            border = BorderStroke(1.5.dp, CyanTactical),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = "Geajao.AI",
                                    tint = CyanTactical,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Geajao.AI",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = CyanTactical.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, CyanTactical.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "AI BOT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = CyanTactical,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Official Cadet & Ex-Cadet Tactical AI Companion",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldVerified
                            )
                        }
                    }

                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("geajao_ai_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear AI Conversation",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Prompt Suggestions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPrompts.forEach { (label, fullPrompt) ->
                        AssistChip(
                            onClick = {
                                if (!isBusy) {
                                    onSendPrompt(fullPrompt)
                                }
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = BrassGold,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.testTag("geajao_ai_chip_${label.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        // Transparent Error Banner if AI Call Fails
        if (aiState is GeajaoAiState.Error) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("geajao_ai_error_banner"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CrimsonHonor.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, CrimsonHonor)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = CrimsonHonor
                    )
                    Text(
                        text = aiState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Message List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 330.dp),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (msg.isUser) 16.dp else 4.dp,
                            bottomEnd = if (msg.isUser) 4.dp else 16.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isUser) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (msg.isUser) BrassGold.copy(alpha = 0.5f) else CyanTactical.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (!msg.isUser) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = CyanTactical,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = if (msg.isUser) "You" else "Geajao.AI",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (msg.isUser) BrassGold else CyanTactical
                                )
                                if (msg.isStreaming) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = CyanTactical
                                    )
                                }
                            }

                            Text(
                                text = if (msg.text.isBlank() && msg.isStreaming) "Thinking..." else msg.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Composer Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    placeholder = { Text("Ask Geajao.AI anything...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("geajao_ai_prompt_input"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (inputPrompt.isNotBlank() && !isBusy) {
                            onSendPrompt(inputPrompt)
                            inputPrompt = ""
                        }
                    },
                    enabled = inputPrompt.isNotBlank() && !isBusy,
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("geajao_ai_send_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send to Geajao.AI")
                    }
                }
            }
        }
    }
}
