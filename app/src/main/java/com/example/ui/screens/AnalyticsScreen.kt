package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CadetProfile
import com.example.data.model.FeedPost
import com.example.data.model.GeajaoConstants
import com.example.data.model.UiState
import com.example.ui.components.CadetRoleBadgePill
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale

@Composable
fun AnalyticsScreen(
    currentUserId: String,
    allCadetsState: UiState<List<CadetProfile>>,
    feedPostsState: UiState<List<FeedPost>>
) {
    val posts = (feedPostsState as? UiState.Success)?.data ?: emptyList()
    val directory = (allCadetsState as? UiState.Success)?.data ?: emptyList()

    val myPosts = posts.filter { it.authorId == currentUserId }
    val totalViews = posts.sumOf { it.viewsCount }
    val totalLikes = posts.sumOf { it.likesCount }
    val totalComments = posts.sumOf { it.commentsCount }
    val totalShares = posts.sumOf { it.sharesCount }
    val myViews = myPosts.sumOf { it.viewsCount }
    val myLikes = myPosts.sumOf { it.likesCount }

    val engagementRate = if (totalViews > 0L) {
        ((totalLikes + totalComments + totalShares).toDouble() / totalViews.toDouble()) * 100.0
    } else {
        0.0
    }

    val badgeDistribution = GeajaoConstants.ALL_ASSIGNABLE_BADGES_ADMIN.map { badge ->
        badge to directory.count { it.primaryBadge == badge || it.badges.contains(badge) }
    }

    val now = System.currentTimeMillis()
    val dayMs = 86_400_000L
    val sevenDayPoints = (6 downTo 0).map { daysAgo ->
        val bucketStart = now - (daysAgo + 1) * dayMs
        val bucketEnd = now - daysAgo * dayMs
        val dayPosts = posts.filter {
            val createdMs = it.createdAt?.toDate()?.time ?: 0L
            createdMs in bucketStart..bucketEnd
        }
        val score = dayPosts.sumOf {
            it.viewsCount + (it.likesCount * 3L) + (it.commentsCount * 4L) + (it.sharesCount * 5L)
        }
        score.toFloat()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("analytics_dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = "Analytics",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Creator & Corps Intelligence",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Live engagement telemetry, audience growth across Cadet & Ex-Cadet wings, and creator post reach.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiMetricCard(
                    title = "CORPS REACH",
                    value = "$totalViews",
                    subtitle = "My Posts: $myViews views",
                    icon = Icons.Default.AutoGraph,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "ENGAGEMENT RATE",
                    value = String.format(Locale.US, "%.1f%%", engagementRate),
                    subtitle = "${totalLikes + totalComments + totalShares} actions ($myLikes mine)",
                    icon = Icons.Default.ThumbUp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val approvedCount = directory.count { it.isApproved }
                KpiMetricCard(
                    title = "VERIFIED CADETS",
                    value = "$approvedCount",
                    subtitle = "${directory.size} registered profiles",
                    icon = Icons.Default.Groups,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "DISPATCH VOLUME",
                    value = "${posts.size}",
                    subtitle = "${myPosts.size} authored by you",
                    icon = Icons.Default.Insights,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("engagement_growth_chart_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "7-DAY ENGAGEMENT & AUDIENCE VELOCITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = JetBrainsMonoFontFamily
                    )

                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    ) {
                        val maxVal = (sevenDayPoints.maxOrNull() ?: 0f).coerceAtLeast(1f)
                        val stepX = if (sevenDayPoints.size > 1) size.width / (sevenDayPoints.size - 1) else size.width
                        val linePath = Path()
                        val fillPath = Path()

                        sevenDayPoints.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = size.height - ((value / maxVal) * (size.height * 0.78f)) - 12f
                            if (index == 0) {
                                linePath.moveTo(x, y)
                                fillPath.moveTo(x, size.height)
                                fillPath.lineTo(x, y)
                            } else {
                                linePath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                            if (index == sevenDayPoints.lastIndex) {
                                fillPath.lineTo(x, size.height)
                                fillPath.close()
                            }
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.35f), primaryColor.copy(alpha = 0.0f))
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(width = 6f)
                        )

                        sevenDayPoints.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = size.height - ((value / maxVal) * (size.height * 0.78f)) - 12f
                            drawCircle(
                                color = secondaryColor,
                                radius = 9f,
                                center = Offset(x, y)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val labels = listOf("D-6", "D-5", "D-4", "D-3", "D-2", "Yest", "Today")
                        labels.forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "AUDIENCE COMPOSITION BY BADGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = JetBrainsMonoFontFamily
                    )

                    val totalMembers = directory.size.coerceAtLeast(1)
                    badgeDistribution.forEach { (badgeLabel, count) ->
                        val ratio = count.toFloat() / totalMembers.toFloat()
                        val barColor = MaterialTheme.colorScheme.primary
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CadetRoleBadgePill(badge = badgeLabel)
                                Text(
                                    text = "$count members (${(ratio * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                            }
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            ) {
                                drawRoundRect(
                                    color = trackColor,
                                    size = size,
                                    cornerRadius = CornerRadius(12f, 12f)
                                )
                                drawRoundRect(
                                    color = barColor,
                                    size = Size(size.width * ratio.coerceIn(0f, 1f), size.height),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (posts.isNotEmpty()) {
            item {
                Text(
                    text = "TOP PERFORMING DISPATCHES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
            items(
                posts.sortedByDescending { it.likesCount * 3 + it.sharesCount * 4 + it.viewsCount }.take(5),
                key = { it.id }
            ) { post ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = post.authorName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = post.title.ifBlank { post.content },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${post.likesCount} Salutes",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${post.viewsCount} Views • ${post.sharesCount} Relays",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
