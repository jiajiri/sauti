package com.sauti.app.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hardware Interrupts & Dynamic Audio Routing Engine.
 * Monitors USB-C audio pins, 3.5mm wired headsets, and Bluetooth SCO/A2DP devices.
 * Automatically routes input/output channels and notifies acoustic processors of route changes.
 */
class AudioRoutingManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val tag = "AudioRoutingManager"

    sealed class AudioRouteType(val displayName: String, val isExternal: Boolean) {
        object BuiltInMic : AudioRouteType("Internal Microphone", false)
        object UsbCHeadset : AudioRouteType("USB-C Peripheral", true)
        object WiredHeadset : AudioRouteType("Wired Headset Jack", true)
        object BluetoothSco : AudioRouteType("Bluetooth SCO Device", true)
        object BluetoothA2dp : AudioRouteType("Bluetooth Audio", true)
        object Unknown : AudioRouteType("Default Audio Route", false)
    }

    private val _currentRoute = MutableStateFlow<AudioRouteType>(AudioRouteType.BuiltInMic)
    val currentRoute: StateFlow<AudioRouteType> = _currentRoute.asStateFlow()

    private val _preferredInputDevice = MutableStateFlow<AudioDeviceInfo?>(null)
    val preferredInputDevice: StateFlow<AudioDeviceInfo?> = _preferredInputDevice.asStateFlow()

    private val _externalDeviceAttached = MutableStateFlow(false)
    val externalDeviceAttached: StateFlow<Boolean> = _externalDeviceAttached.asStateFlow()

    private var deviceCallback: AudioDeviceCallback? = null

    init {
        try {
            detectCurrentActiveDevices()
            registerDeviceCallbacks()
        } catch (e: Exception) {
            Log.w(tag, "Audio routing setup fallback: ${e.message}")
        }
    }

    /**
     * Inspects currently attached hardware audio devices.
     */
    fun detectCurrentActiveDevices() {
        try {
            val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            evaluateRouting(inputDevices, outputDevices)
        } catch (e: Exception) {
            Log.w(tag, "Could not inspect active audio devices: ${e.message}")
        }
    }

    private fun registerDeviceCallbacks() {
        try {
            deviceCallback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    super.onAudioDevicesAdded(addedDevices)
                    Log.i(tag, "Hardware Interrupt: Audio Device(s) connected -> ${addedDevices?.size ?: 0}")
                    detectCurrentActiveDevices()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    super.onAudioDevicesRemoved(removedDevices)
                    Log.i(tag, "Hardware Interrupt: Audio Device(s) disconnected -> ${removedDevices?.size ?: 0}")
                    detectCurrentActiveDevices()
                }
            }
            audioManager.registerAudioDeviceCallback(deviceCallback, null)
        } catch (e: Exception) {
            Log.w(tag, "AudioDeviceCallback registration fallback: ${e.message}")
        }
    }

    private fun evaluateRouting(inputs: Array<AudioDeviceInfo>, outputs: Array<AudioDeviceInfo>) {
        var detectedRoute: AudioRouteType = AudioRouteType.BuiltInMic
        var targetDevice: AudioDeviceInfo? = null
        var hasExternal = false

        // 1. First prioritize dedicated external input devices
        for (device in inputs) {
            when (device.type) {
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_HEADSET -> {
                    detectedRoute = AudioRouteType.UsbCHeadset
                    targetDevice = device
                    hasExternal = true
                    break
                }
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> {
                    detectedRoute = AudioRouteType.WiredHeadset
                    targetDevice = device
                    hasExternal = true
                    break
                }
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                    detectedRoute = AudioRouteType.BluetoothSco
                    targetDevice = device
                    hasExternal = true
                    enableBluetoothSco()
                    break
                }
            }
        }

        // 2. If no dedicated mic was flagged in inputs alone, inspect output devices (e.g. wired headset / USB-C headset jack)
        if (!hasExternal) {
            for (device in outputs) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_USB_DEVICE -> {
                        detectedRoute = AudioRouteType.UsbCHeadset
                        targetDevice = inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_USB_HEADSET || it.type == AudioDeviceInfo.TYPE_USB_DEVICE }
                        hasExternal = true
                        break
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                        detectedRoute = AudioRouteType.WiredHeadset
                        targetDevice = inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET }
                        hasExternal = true
                        break
                    }
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                        detectedRoute = AudioRouteType.BluetoothSco
                        targetDevice = inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
                        hasExternal = true
                        enableBluetoothSco()
                        break
                    }
                }
            }
        }

        _externalDeviceAttached.value = hasExternal
        _preferredInputDevice.value = targetDevice
        _currentRoute.value = detectedRoute
        Log.d(tag, "Active Audio Route resolved: ${detectedRoute.displayName} (device=${targetDevice?.productName}, external=$hasExternal)")
    }

    /**
     * Activates Bluetooth SCO audio link if Bluetooth headset is present.
     */
    fun enableBluetoothSco() {
        try {
            if (!audioManager.isBluetoothScoOn) {
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
                Log.i(tag, "Bluetooth SCO audio link engaged")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start Bluetooth SCO: ${e.message}")
        }
    }

    /**
     * Deactivates Bluetooth SCO link.
     */
    fun disableBluetoothSco() {
        try {
            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                Log.i(tag, "Bluetooth SCO audio link disengaged")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop Bluetooth SCO: ${e.message}")
        }
    }

    /**
     * Cleans up listener callbacks.
     */
    fun teardown() {
        deviceCallback?.let {
            audioManager.unregisterAudioDeviceCallback(it)
            deviceCallback = null
        }
        disableBluetoothSco()
    }
}
