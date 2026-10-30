package com.duolink.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import com.duolink.app.ui.theme.DuoLinkTextMuted
import java.util.Locale

@Composable
fun AudioMessagePlayer(
    audioUri: String,
    durationMs: Long,
    waveformString: String?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    playbackSpeed: Float,
    onPlayPauseClick: () -> Unit,
    onSpeedToggleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val waveform = waveformString?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
        ?: listOf(30, 50, 80, 45, 90, 60, 40, 75, 50, 30)

    val progressFraction = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(DuoLinkRedPrimary)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalBars = waveform.size.coerceAtLeast(1)
                waveform.forEachIndexed { index, amp ->
                    val barFraction = index.toFloat() / totalBars.toFloat()
                    val isPast = barFraction <= progressFraction
                    val barColor = if (isPast) DuoLinkRedPrimary else Color.White.copy(alpha = 0.3f)
                    val barHeight = ((amp / 100f) * 26f).coerceIn(4f, 26f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(barColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayMs = if (isPlaying) currentPositionMs else durationMs
                Text(
                    text = formatDuration(displayMs),
                    fontSize = 11.sp,
                    color = DuoLinkTextMuted
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DuoLinkCharcoalBg)
                        .clickable { onSpeedToggleClick() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DuoLinkRedPrimary
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
