package com.sauti.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sauti.app.ui.theme.*

data class HubModelData(
    val name: String,
    val author: String,
    val category: String,
    val size: String,
    val ramEst: String,
    val isDownloaded: Boolean
)

@Composable
fun HubScreen(
    onLoadModel: (String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("Recommended") }
    val filterTabs = listOf("Recommended", "Newest", "Popular", "Trending", "Bookmarks")

    val models = listOf(
        HubModelData(
            name = "Qwen3.5-0.8B-LocalAI",
            author = "weareapexcreators",
            category = "IMAGE TEXT TO TEXT",
            size = "524 MB",
            ramEst = "~850 MB",
            isDownloaded = true
        ),
        HubModelData(
            name = "Bonsai-27B-LocalAI",
            author = "weareapexcreators",
            category = "TEXT GENERATION",
            size = "14.2 GB",
            ramEst = "~16.0 GB",
            isDownloaded = false
        ),
        HubModelData(
            name = "SmolLM2-135M-Swahili",
            author = "mozilla-sauti",
            category = "GRAMMAR ASR CLEANUP",
            size = "43.2 MB",
            ramEst = "~90 MB",
            isDownloaded = true
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMatte)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Hub Header matching screenshot
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Octopus / Squid glyph simulation
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SurfaceInput),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "O", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "HUB",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Top action icons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloads",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Search Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceInput)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Search thousands of models...",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Filter",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        // Filter Tabs row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterTabs) { tab ->
                val isSelected = tab == selectedFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AccentAmber else Color.Transparent)
                        .clickable { selectedFilter = tab }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) Color.Black else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Ultra-minimal Hardware Telemetry Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceInput)
                .padding(horizontal = 16.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AccentEmeraldOrb)
                )
                Text(
                    text = "ARM64E",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = "24.0 GB RAM (17.8 GB usable for AI)",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Text(
            text = "YOUR MODELS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        // Models List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(models) { model ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF262013))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = model.category,
                                    color = AccentAmber,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column {
                            Text(
                                text = model.name,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "By: ${model.author}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${model.size} • ${model.ramEst}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )

                            // Play / Load button
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (model.isDownloaded) AccentAmber else SurfaceInput)
                                    .clickable { onLoadModel(model.name) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (model.isDownloaded) Icons.Default.PlayArrow else Icons.Default.Download,
                                    contentDescription = "Load",
                                    tint = if (model.isDownloaded) Color.Black else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
