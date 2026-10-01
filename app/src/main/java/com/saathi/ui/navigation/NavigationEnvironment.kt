package com.saathi.ui.navigation

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal data class NavigationEnvironment(val animationsEnabled: Boolean, val blurSupported: Boolean)

/** Observe system changes so returning from Settings does not leave stale motion/blur policy. */
@Composable
internal fun rememberNavigationEnvironment(): NavigationEnvironment {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current
    fun read(): NavigationEnvironment {
        val lowRam = context.getSystemService(ActivityManager::class.java).isLowRamDevice
        val powerSave = context.getSystemService(PowerManager::class.java).isPowerSaveMode
        val highContrast = Settings.Secure.getInt(context.contentResolver, "high_text_contrast_enabled", 0) == 1
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        return NavigationEnvironment(scale > 0f, Build.VERSION.SDK_INT >= 31 && view.isHardwareAccelerated && !lowRam && !powerSave && !highContrast)
    }
    var environment by remember { mutableStateOf(read()) }
    DisposableEffect(context, lifecycle, view) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { environment = read() }
        }
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        context.contentResolver.registerContentObserver(Settings.Secure.getUriFor("high_text_contrast_enabled"), false, observer)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { environment = read() }
        }
        context.registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        val resume = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) environment = read() }
        lifecycle.lifecycle.addObserver(resume)
        environment = read()
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
            context.unregisterReceiver(receiver)
            lifecycle.lifecycle.removeObserver(resume)
        }
    }
    return environment
}
