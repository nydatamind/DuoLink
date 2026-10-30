package com.duolink.app.ui.mascot

import androidx.annotation.DrawableRes
import com.duolink.app.R

enum class OwlEyeState(
    val title: String,
    val description: String,
    @get:DrawableRes val drawableRes: Int
) {
    OPEN(
        title = "Active",
        description = "Partner active recently",
        drawableRes = R.drawable.owl_open
    ),
    HALF(
        title = "Inactive",
        description = "Partner inactive for some time",
        drawableRes = R.drawable.owl_half
    ),
    CLOSED(
        title = "Offline",
        description = "Partner inactive for over an hour",
        drawableRes = R.drawable.owl_closed
    )
}
