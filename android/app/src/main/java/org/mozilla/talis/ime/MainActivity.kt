package com.sauti.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.sauti.app.ui.screens.ChatScreen
import com.sauti.app.ui.screens.HubScreen
import com.sauti.app.ui.screens.VoiceModeScreen
import com.sauti.app.ui.theme.BackgroundDark
import com.sauti.app.ui.theme.PrimaryBlue
import com.sauti.app.ui.theme.SurfaceDark
import com.sauti.app.ui.theme.SautiThemeLegacy

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SautiThemeLegacy {
                var selectedTab by remember { mutableIntStateOf(0) }
                var showVoiceMode by remember { mutableStateOf(false) }

                if (showVoiceMode) {
                    VoiceModeScreen(
                        onCloseClick = { showVoiceMode = false }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = BackgroundDark,
                        bottomBar = {
                            NavigationBar(
                                containerColor = SurfaceDark
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Default.Memory, contentDescription = "Hub") },
                                    label = { Text("Hub") },
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryBlue)
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                                    label = { Text("Chat") },
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryBlue)
                                )
                                NavigationBarItem(
                                    selected = false,
                                    onClick = { showVoiceMode = true },
                                    icon = { Icon(Icons.Default.Mic, contentDescription = "Voice Mode") },
                                    label = { Text("Voice") },
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryBlue)
                                )
                            }
                        }
                    ) { innerPadding ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            color = BackgroundDark
                        ) {
                            when (selectedTab) {
                                0 -> HubScreen()
                                1 -> ChatScreen(onVoiceClick = { showVoiceMode = true })
                            }
                        }
                    }
                }
            }
        }
    }
}
