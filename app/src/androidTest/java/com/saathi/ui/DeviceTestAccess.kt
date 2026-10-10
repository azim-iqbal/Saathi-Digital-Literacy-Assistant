package com.saathi.ui

import android.app.UiAutomation
import android.os.ParcelFileDescriptor
import android.os.SystemClock

/** Instrumentation restarts the app; Samsung can retain an enabled-but-crashed service entry. */
internal object DeviceTestAccess {
    fun reconnect(automation: UiAutomation) {
        fun shell(command:String)=ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }
        val component="com.saathi/com.saathi.accessibility.SaathiAccessibilityService"
        val prior=shell("settings get secure enabled_accessibility_services").takeUnless { it=="null" }.orEmpty()
        val others=prior.split(':').filter { it.isNotBlank() && it!=component }.joinToString(":")
        if (others.isEmpty()) shell("settings delete secure enabled_accessibility_services")
        else shell("settings put secure enabled_accessibility_services $others")
        SystemClock.sleep(300)
        val enabled=(others.split(':').filter { it.isNotBlank() }+component).joinToString(":")
        shell("settings put secure enabled_accessibility_services $enabled")
        shell("settings put secure accessibility_enabled 1")
    }
}
