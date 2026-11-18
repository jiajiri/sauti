package com.sauti.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ExploreCategory(
    val title: String,
    val items: List<ExplorePromptItem>
)

data class ExplorePromptItem(
    val icon: ImageVector,
    val title: String,
    val prompt: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreTasksBottomSheet(
    onDismiss: () -> Unit,
    onSelectPrompt: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val categories = listOf(
        ExploreCategory(
            title = "Swahili Mastery & Learning",
            items = listOf(
                ExplorePromptItem(
                    icon = Icons.Outlined.Translate,
                    title = "Learn Daily Idioms (Misemo)",
                    prompt = "Nifundishe misemo 5 ya Kiswahili inayotumiwa sana katika maisha ya kila siku pamoja na maana zake."
                ),
                ExplorePromptItem(
                    icon = Icons.Outlined.Create,
                    title = "Poem Writing (Mashairi ya Kiswahili)",
                    prompt = "Nitungie shairi fupi lenye beti nne kuhusu bidii ya kazi na uvumilivu lenye urari wa vina."
                )
            )
        ),
        ExploreCategory(
            title = "Coding & Technology",
            items = listOf(
                ExplorePromptItem(
                    icon = Icons.Outlined.Code,
                    title = "Write Fast Python Function",
                    prompt = "Andika function ya Python inayopanga na kuchuja orodha ya vitu kwa kasi ya juu bila kutumia libraries za ziada."
                ),
                ExplorePromptItem(
                    icon = Icons.Outlined.Terminal,
                    title = "Bash Automation Script",
                    prompt = "Nisaidie kuandika bash script ya kuhifadhi nakala (backup) ya mafaili kila siku kiotomatiki."
                )
            )
        ),
        ExploreCategory(
            title = "Productivity & Professional Writing",
            items = listOf(
                ExplorePromptItem(
                    icon = Icons.Outlined.Description,
                    title = "Job Application Cover Letter",
                    prompt = "Andika barua rasmi na yenye ushawishi ya kuomba nafasi ya kazi ya maendeleo ya programu."
                ),
                ExplorePromptItem(
                    icon = Icons.Outlined.Lightbulb,
                    title = "Digital Business Brainstorm",
                    prompt = "Nipe mawazo 3 ya miradi ya kidijitali nchini Tanzania inayoweza kuanzishwa kwa mtaji mdogo."
                )
            )
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline) },
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Explore,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text("Explore Tasks", color = colors.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Select a curated task to launch instantly in Sauti", color = colors.onSurfaceVariant, fontSize = 13.sp)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                categories.forEach { cat ->
                    item {
                        Text(
                            text = cat.title.uppercase(),
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surfaceVariant)
                        ) {
                            cat.items.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectPrompt(item.prompt) }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Text(item.prompt, color = colors.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.ChevronRight,
                                        contentDescription = null,
                                        tint = colors.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (idx < cat.items.size - 1) {
                                    HorizontalDivider(color = colors.outline, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
