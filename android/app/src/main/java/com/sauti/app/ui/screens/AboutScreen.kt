package com.sauti.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardDarkBg   = Color(0xFF171717)
private val DividerColor = Color(0xFF262626)
private val TextPri      = Color(0xFFFFFFFF)
private val TextSec      = Color(0xFF9E9E9E)
private val CircleBtnBg  = Color(0xFF1E1E1E)

/**
 * About Sub-Screen strictly modeled after Reference 5.
 * Features a single grouped rounded card with hairline dividers and version aligned right.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    BackHandler(enabled = true) { onBack() }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar: Circular back button and centered About title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CircleBtnBg)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPri
                )
            }

            Text(
                text = "About",
                color = TextPri,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grouped Card (Reference 5)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardDarkBg)
        ) {
            AboutItemRow(
                title = "Help center",
                isExternal = true,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com"))
                    context.startActivity(intent)
                }
            )

            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

            AboutItemRow(
                title = "Terms of use",
                isExternal = true,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com"))
                    context.startActivity(intent)
                }
            )

            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

            AboutItemRow(
                title = "Privacy policy",
                isExternal = true,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com"))
                    context.startActivity(intent)
                }
            )

            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

            AboutItemRow(
                title = "Licenses",
                isExternal = false,
                onClick = { /* Open license notices */ }
            )

            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

            // App Version row aligned right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sauti for Android",
                    color = TextPri,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )

                Text(
                    text = "1.0.0 (100% Sovereign)",
                    color = TextSec,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun AboutItemRow(
    title: String,
    isExternal: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = TextPri,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        )

        Icon(
            imageVector = if (isExternal) Icons.Outlined.NorthEast else Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = TextSec,
            modifier = Modifier.size(18.dp)
        )
    }
}
