package com.duolink.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolink.app.ui.theme.DuoLinkCharcoalBg
import com.duolink.app.ui.theme.DuoLinkRedPrimary
import com.duolink.app.ui.theme.DuoLinkSurface
import com.duolink.app.ui.theme.DuoLinkTextMuted
import com.duolink.app.ui.theme.DuoLinkTextWhite

@Composable
fun DuoLinkTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(DuoLinkCharcoalBg)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBackClick != null) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DuoLinkRedPrimary
                )
            }
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DuoLinkTextWhite
                    )
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DuoLinkTextMuted
                        )
                    )
                }
            }
        }

        actions()
    }
}

@Composable
fun StatusBadge(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isOnline) DuoLinkRedPrimary else Color.Gray
    val text = if (isOnline) "Online" else "Offline"

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AvatarView(
    name: String,
    isOnline: Boolean? = null,
    sizeDp: Int = 48,
    modifier: Modifier = Modifier
) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "K"

    Box(
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(DuoLinkSurface)
                .border(1.5.dp, DuoLinkRedPrimary.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = DuoLinkTextWhite,
                fontSize = (sizeDp * 0.42f).sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isOnline != null) {
            val dotColor = if (isOnline) DuoLinkRedPrimary else Color.Gray
            Box(
                modifier = Modifier
                    .size((sizeDp * 0.28f).dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(DuoLinkCharcoalBg)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}
