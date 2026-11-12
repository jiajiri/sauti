package com.sauti.app.power

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Power Management & Hardware Window Flags Controller.
 * Monitors battery charging state, USB power interrupts, and dynamically controls
 * window flags (FLAG_KEEP_SCREEN_ON, soft input adjustments).
 */
class PowerHardwareManager(private val activity: Activity) {

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _chargingType = MutableStateFlow("On Battery")
    val chargingType: StateFlow<String> = _chargingType.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isLowPower = MutableStateFlow(false)
    val isLowPower: StateFlow<Boolean> = _isLowPower.asStateFlow()

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    _isCharging.value = true
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    _isCharging.value = false
                    _chargingType.value = "On Battery"
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

                    if (level != -1 && scale > 0) {
                        _batteryLevel.value = (level * 100) / scale
                    }
                    val isChargingNow = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                    _isCharging.value = isChargingNow

                    _chargingType.value = when {
                        !isChargingNow -> "On Battery"
                        chargePlug == BatteryManager.BATTERY_PLUGGED_AC -> "Charging (AC)"
                        chargePlug == BatteryManager.BATTERY_PLUGGED_USB -> "Charging (USB)"
                        chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Charging (Wireless)"
                        else -> "Charging"
                    }
                }
                Intent.ACTION_BATTERY_LOW -> {
                    _isLowPower.value = true
                }
                Intent.ACTION_BATTERY_OKAY -> {
                    _isLowPower.value = false
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }
        activity.registerReceiver(powerReceiver, filter)
    }

    /**
     * Toggles screen keep-awake flag during active continuous voice capture.
     */
    fun setVoiceScreenAwake(keepAwake: Boolean) {
        activity.runOnUiThread {
            if (keepAwake) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    /**
     * Unregisters broadcast receivers.
     */
    fun teardown() {
        try {
            activity.unregisterReceiver(powerReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }
}
