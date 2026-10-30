package com.duolink.app.ui.mascot

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object DynamicIconManager {
    private const val TAG = "DynamicIconManager"

    private const val ALIAS_OPEN = "com.duolink.app.MainActivityAliasOpen"
    private const val ALIAS_HALF = "com.duolink.app.MainActivityAliasHalf"
    private const val ALIAS_CLOSED = "com.duolink.app.MainActivityAliasClosed"

    /**
     * Dynamically updates the Android home-screen launcher icon using activity-alias.
     * Disables other aliases and enables the target alias with DONT_KILL_APP.
     */
    fun setLauncherIcon(context: Context, state: OwlEyeState) {
        try {
            val pm = context.packageManager
            val targetAliasName = when (state) {
                OwlEyeState.OPEN -> ALIAS_OPEN
                OwlEyeState.HALF -> ALIAS_HALF
                OwlEyeState.CLOSED -> ALIAS_CLOSED
            }

            val aliases = listOf(ALIAS_OPEN, ALIAS_HALF, ALIAS_CLOSED)

            for (aliasName in aliases) {
                val component = ComponentName(context.packageName, aliasName)
                val isTarget = (aliasName == targetAliasName)
                val desiredState = if (isTarget) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }

                val currentState = pm.getComponentEnabledSetting(component)
                if (currentState != desiredState) {
                    pm.setComponentEnabledSetting(
                        component,
                        desiredState,
                        PackageManager.DONT_KILL_APP
                    )
                    Log.d(TAG, "Alias $aliasName switched to $desiredState")
                }
            }
            Log.d(TAG, "Dynamic launcher icon updated to $state successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update dynamic launcher icon: ${e.message}")
        }
    }
}

