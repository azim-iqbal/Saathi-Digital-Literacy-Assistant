package com.saathi.device

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/** OEM-specific hints are best-effort only; every deep link has a readable fallback. */
object DeviceCapabilities {
    enum class BatteryRisk { NONE, MODERATE, SEVERE }
    data class Profile(val manufacturer: String, val batteryRisk: BatteryRisk, val setupIntent: Intent?, val manualInstructions: String)

    fun detect(context: Context): Profile {
        val maker = Build.MANUFACTURER.lowercase()
        val (risk, deepLink, fallback) = when (maker) {
            "samsung" -> Triple(BatteryRisk.SEVERE,
                Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
                "Settings → Apps → Saathi → Battery → Unrestricted. Also remove Saathi from Sleeping apps.")
            "xiaomi", "redmi" -> Triple(BatteryRisk.SEVERE,
                Intent("miui.intent.action.APP_PERM_EDITOR").putExtra("extra_pkgname", context.packageName),
                "Settings → Apps → Permissions → Autostart, then allow Saathi.")
            "oppo", "realme", "vivo" -> Triple(BatteryRisk.SEVERE, null,
                "Settings → Battery → Auto-launch or Background power → allow Saathi.")
            "google" -> Triple(BatteryRisk.NONE, null, "Stock Android normally keeps the active guidance notification running.")
            else -> Triple(BatteryRisk.MODERATE, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                "Settings → Apps → Saathi → Battery → Unrestricted.")
        }
        val usable = deepLink?.takeIf { it.resolveActivity(context.packageManager) != null }
        return Profile(maker.replaceFirstChar { it.uppercase() }, risk, usable, fallback)
    }

    fun isBatterySaverOn(context: Context) = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode
}
