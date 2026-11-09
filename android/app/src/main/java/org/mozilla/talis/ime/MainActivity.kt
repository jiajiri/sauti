package com.sauti.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sauti.app.ui.screens.ChatScreen
import com.sauti.app.ui.screens.ColdChainScreen
import com.sauti.app.ui.screens.HitlScreen
import com.sauti.app.ui.screens.HubScreen
import com.sauti.app.ui.screens.VoiceModeScreen
import com.sauti.app.ui.theme.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SautiThemeLegacy {
                var selectedTab by remember { mutableIntStateOf(0) }
                var showVoiceMode by remember { mutableStateOf(false) }
                var isGlobalLoading by remember { mutableStateOf(false) }

                if (showVoiceMode) {
                    VoiceModeScreen(
                        onCloseClick = { showVoiceMode = false },
                        onSubmitTriageQuery = {
                            showVoiceMode = false
                            selectedTab = 1 // Switch to HITL / triage review
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = BackgroundMatte,
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { showVoiceMode = true },
                                containerColor = PrimaryBlue,
                                contentColor = TextPrimary
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Dictation Triage",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = SurfaceCard,
                                tonalElevation = 0.dp
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.AcUnit,
                                            contentDescription = "Cold Chain"
                                        )
                                    },
                                    label = { Text("Cold Chain") },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = PrimaryBlue.copy(alpha = 0.2f),
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = "HITL Reviews"
                                        )
                                    },
                                    label = { Text("Approvals") },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = PrimaryBlue.copy(alpha = 0.2f),
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Forum,
                                            contentDescription = "Consultation"
                                        )
                                    },
                                    label = { Text("Consult") },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = PrimaryBlue.copy(alpha = 0.2f),
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 3,
                                    onClick = { selectedTab = 3 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Memory,
                                            contentDescription = "Hardware Hub"
                                        )
                                    },
                                    label = { Text("Hub") },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = PrimaryBlue.copy(alpha = 0.2f),
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            color = BackgroundMatte
                        ) {
                            when (selectedTab) {
                                0 -> ColdChainScreen(
                                    isLoading = isGlobalLoading,
                                    onNavigateToHitl = { selectedTab = 1 },
                                    onLogExcursionAlert = { _, _, _ ->
                                        selectedTab = 1
                                    }
                                )
                                1 -> HitlScreen(
                                    isLoading = isGlobalLoading
                                )
                                2 -> ChatScreen(
                                    isLoading = isGlobalLoading,
                                    onVoiceClick = { showVoiceMode = true }
                                )
                                3 -> HubScreen(
                                    isLoading = isGlobalLoading,
                                    onToggleSkeleton = {
                                        isGlobalLoading = !isGlobalLoading
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
