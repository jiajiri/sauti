package com.sauti.app.ui

import com.airbnb.android.showkase.annotation.ShowkaseRoot
import com.airbnb.android.showkase.annotation.ShowkaseRootModule

/**
 * Root specification module for Airbnb Showkase.
 *
 * Scans the codebase for `@Preview` and `@ShowkaseComposable` annotations and generates
 * `ShowkaseBrowserActivity` for interactive on-device UI component exploration.
 *
 * To launch on device/emulator:
 * ```bash
 * adb shell am start -n com.sauti.app/com.airbnb.android.showkase.ui.ShowkaseBrowserActivity
 * ```
 */
@ShowkaseRoot
class SautiShowkaseModule : ShowkaseRootModule
