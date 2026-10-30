package com.duolink.app.ui.mascot

import androidx.annotation.DrawableRes
import com.duolink.app.R

enum class OwlEyeState(
    val title: String,
    val description: String,
    @get:DrawableRes val drawableRes: Int
) {
    OPEN(
        title = "Active & Alert",
        description = "Partner active in the last 5 minutes",
        drawableRes = R.drawable.owl_open
    ),
    HALF(
        title = "Sleepy / Drowsy",
        description = "Partner inactive for 5 min – 1 hour",
        drawableRes = R.drawable.owl_half
    ),
    CLOSED(
        title = "Resting / Sleeping",
        description = "Partner inactive for more than 1 hour",
        drawableRes = R.drawable.owl_closed
    )
}
