package com.duolink.app.ui.emoji

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolink.app.ui.theme.DuoLinkCharcoalBg
import com.duolink.app.ui.theme.DuoLinkRedPrimary
import com.duolink.app.ui.theme.DuoLinkSurface
import com.duolink.app.ui.theme.DuoLinkTextMuted
import com.duolink.app.ui.theme.DuoLinkTextWhite

@Composable
fun EmojiPickerView(
    onEmojiSelected: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val emojiManager = remember { EmojiManager(context) }

    var selectedCategory by remember { mutableStateOf(EmojiCategory.RECENT) }
    var searchQuery by remember { mutableStateOf("") }
    var recentsList by remember { mutableStateOf(emojiManager.getRecentEmojis()) }

    val displayedEmojis = remember(selectedCategory, searchQuery, recentsList) {
        if (searchQuery.isNotBlank()) {
            emojiManager.searchEmojis(searchQuery).map { it.emoji }
        } else if (selectedCategory == EmojiCategory.RECENT) {
            recentsList
        } else {
            EmojiCatalog.ALL_EMOJIS.filter { it.category == selectedCategory }.map { it.emoji }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .background(DuoLinkSurface)
            .padding(top = 8.dp)
    ) {
        // TOP SEARCH BAR & BACKSPACE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search emoji (e.g. heart, fire, owl)", fontSize = 13.sp, color = DuoLinkTextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DuoLinkTextMuted, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = DuoLinkTextMuted)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DuoLinkCharcoalBg,
                    unfocusedContainerColor = DuoLinkCharcoalBg,
                    focusedBorderColor = DuoLinkRedPrimary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = DuoLinkTextWhite,
                    unfocusedTextColor = DuoLinkTextWhite
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // BACKSPACE BUTTON
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DuoLinkCharcoalBg)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBackspace()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = DuoLinkRedPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // CATEGORY TAB ROW (TELEGRAM STYLE)
        if (searchQuery.isBlank()) {
            ScrollableTabRow(
                selectedTabIndex = selectedCategory.ordinal,
                containerColor = DuoLinkSurface,
                contentColor = DuoLinkRedPrimary,
                edgePadding = 8.dp,
                divider = {}
            ) {
                EmojiCategory.values().forEach { cat ->
                    val isSelected = (cat == selectedCategory)
                    Tab(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        text = {
                            Text(
                                text = cat.icon,
                                fontSize = if (isSelected) 18.sp else 16.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    )
                }
            }
        }

        // EMOJI GRID
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(displayedEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            emojiManager.addRecentEmoji(emoji)
                            recentsList = emojiManager.getRecentEmojis()
                            onEmojiSelected(emoji)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}

