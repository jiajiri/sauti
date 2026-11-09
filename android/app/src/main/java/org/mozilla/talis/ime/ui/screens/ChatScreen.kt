package com.sauti.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sauti.app.ui.theme.*

data class MessageItem(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val tokensPerSec: String? = null
)

@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit = {},
    onVoiceClick: () -> Unit = {}
) {
    var inputMessage by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            MessageItem(
                id = "1",
                isUser = true,
                text = "Write a short, beautiful poem about nature."
            ),
            MessageItem(
                id = "2",
                isUser = false,
                text = "The morning light touches the leaves.\nA garden of green.\nThe earth is still.\nIt's a song.\n\nThe wind is a dancer in the grass.\nThe earth is warm.\nIt's a garden.\nI am a part of it.\nThe morning is a river.\nThe light is a song.\nThe time is a song.",
                tokensPerSec = "61.0 t/s"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundMatte)
    ) {
        // Top Navigation Header matching LocalAI
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "New Chat",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Right icons (Document view & Sliders)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Docs",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Messages Body
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(messages) { msg ->
                if (msg.isUser) {
                    UserMessageBubble(text = msg.text)
                } else {
                    AssistantMessageBlock(
                        text = msg.text,
                        tokensPerSec = msg.tokensPerSec
                    )
                }
            }
        }

        // Bottom Precision Action Bar & Input Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceInput)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BasicTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(AccentEmerald),
                    decorationBox = { innerTextField ->
                        if (inputMessage.isEmpty()) {
                            Text(
                                text = "Message Model...",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Precision Action Icons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left quick tool icons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionIcon(icon = Icons.Default.AttachFile, desc = "Attach")
                        ActionIcon(icon = Icons.Default.Mic, desc = "Voice", onClick = onVoiceClick)
                        ActionIcon(icon = Icons.Default.CameraAlt, desc = "Camera")
                        ActionIcon(icon = Icons.Default.Memory, desc = "Engine")
                        ActionIcon(icon = Icons.Default.Language, desc = "Web")
                    }

                    // Send arrow
                    IconButton(
                        onClick = {
                            if (inputMessage.isNotBlank()) {
                                messages.add(MessageItem(id = "${System.currentTimeMillis()}", isUser = true, text = inputMessage))
                                inputMessage = ""
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Send",
                            tint = if (inputMessage.isNotBlank()) AccentEmerald else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit = {}
) {
    Icon(
        imageVector = icon,
        contentDescription = desc,
        tint = TextMuted,
        modifier = Modifier
            .size(18.dp)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun UserMessageBubble(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp))
                .background(AccentEmerald)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // Tiny Copy action pill
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = TextDisabled,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "Copy",
                color = TextDisabled,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun AssistantMessageBlock(
    text: String,
    tokensPerSec: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Plain typography without heavy bubbles
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 13.sp,
            lineHeight = 22.sp
        )

        // Under-message clean action row [Copy] [Read] [61.0 t/s]
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MessagePill(icon = Icons.Default.ContentCopy, label = "Copy")
            MessagePill(icon = Icons.Default.VolumeUp, label = "Read")
            if (tokensPerSec != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = tokensPerSec,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun MessagePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextSecondary,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}
