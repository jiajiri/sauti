package com.sauti.app.memory

import android.content.ComponentCallbacks2

/**
 * Android Memory Pressure Observer.
 * Intercepts Linux kernel out-of-memory pressure signals before the OOM Killer terminates the process.
 */
class MemoryPressureManager(
    private val onTrimAction: (isCritical: Boolean) -> Unit
) : ComponentCallbacks2 {

    override fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // Drop all dynamic caches and generative models immediately
                onTrimAction(true)
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                // Shrink non-essential view pools
                onTrimAction(false)
            }
            else -> {
                // Background trims
                if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
                    onTrimAction(false)
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        // No-op
    }

    override fun onLowMemory() {
        onTrimAction(true)
    }
}
