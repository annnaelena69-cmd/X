package com.lucas.xgameassistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

/**
 * Android-authorized gesture foundation.
 * This service does not play a game by itself; a game-specific decision engine
 * must determine when and where to act.
 */
class GameControlService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Game-specific screen understanding will be implemented separately.
    }

    override fun onInterrupt() = Unit

    fun tapScreen(x: Float, y: Float, durationMs: Long = 80L): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    companion object {
        @Volatile
        var activeService: GameControlService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
    }

    override fun onDestroy() {
        if (activeService === this) activeService = null
        super.onDestroy()
    }
}
