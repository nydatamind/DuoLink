package com.duolink.app.ui.mascot

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.duolink.app.ui.theme.DuoLinkRedPrimary

@Composable
fun OwlMascotView(
    state: OwlEyeState,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showBorder: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showBorder) {
                    Modifier
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = when (state) {
                                OwlEyeState.OPEN -> DuoLinkRedPrimary
                                OwlEyeState.HALF -> DuoLinkRedPrimary.copy(alpha = 0.5f)
                                OwlEyeState.CLOSED -> DuoLinkRedPrimary.copy(alpha = 0.2f)
                            },
                            shape = CircleShape
                        )
                } else Modifier.clip(CircleShape)
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = state, label = "OwlExpressionFade") { targetState ->
            Image(
                painter = painterResource(id = targetState.drawableRes),
                contentDescription = targetState.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        }
    }
}

