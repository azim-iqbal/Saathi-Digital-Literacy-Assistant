package com.saathi.ui

import android.app.UiAutomation
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.MainActivity
import com.saathi.accessibility.NodeMasker
import com.saathi.language.GuidanceLanguage
import com.saathi.orchestrator.SaathiSession
import com.saathi.gateway.PracticeGateway
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReactiveFormIntegrationTest {
    private val inst=InstrumentationRegistry.getInstrumentation()
    private val automation get()=inst.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    private fun shell(cmd:String)=ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(cmd)).bufferedReader().use { it.readText().trim() }
    private fun capture(name:String) {
        waitFor("Form marker attached") { com.saathi.overlay.HighlightOverlayService.hasTarget() }
        // Session instruction publication precedes service attachment/drawing.
        // Capture rendered guidance rather than the pre-render state.
        SystemClock.sleep(120)
        val out=java.io.File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: inst.targetContext.filesDir.path).apply { mkdirs() }
        automation.takeScreenshot()?.let { bitmap ->
            java.io.File(out,"$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
            bitmap.recycle()
        }
    }
    private fun waitFor(message:String, predicate:()->Boolean) {
        val end=SystemClock.uptimeMillis()+12000
        while(!predicate() && SystemClock.uptimeMillis()<end) SystemClock.sleep(100)
        if(!predicate()) {
            val out=java.io.File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: inst.targetContext.filesDir.path).apply { mkdirs() }
            val root=automation.rootInActiveWindow
            java.io.File(out,"form-failure.txt").appendText("$message; state=${SaathiSession.status.value}; instruction=${SaathiSession.instruction.value}\n"+try { root?.let { NodeMasker.flatten(it) }?.joinToString("\n") { "hint=${it.hint} class=${it.className} enabled=${it.isEnabled} focus=${it.isFocused} valueKnown=${it.valueKnown} present=${it.hasValue} invalid=${it.contentInvalid} bounds=${it.bounds}" }.orEmpty() } finally { root?.recycle() })
            java.io.File(out,"form-windows.txt").writeText(shell("dumpsys window windows"))
        }
        assertTrue("$message: ${SaathiSession.instruction.value}",predicate())
    }
    private fun action(label:String, value:String?=null) {
        var acted=false
        waitFor("Fixture control $label") {
            val root=automation.rootInActiveWindow ?: return@waitFor false
            fun visit(n:AccessibilityNodeInfo) {
                if(!acted && (n.hintText?.toString()==label || n.text?.toString()==label || n.contentDescription?.toString()==label)) {
                    if(value!=null && n.isEditable) {
                        n.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                        acted=n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value) })
                    } else if(value==null && n.isClickable) acted=n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                for(i in 0 until n.childCount) n.getChild(i)?.let { child -> try { visit(child) } finally { child.recycle() } }
            }
            try { visit(root) } finally { root.recycle() }
            acted
        }
    }
    private fun verifyUnchangedFallbackKeepsPresentation() {
        val events=java.util.concurrent.atomic.AtomicInteger()
        com.saathi.accessibility.ObservationDiagnostics.observer={ _,_,_,own -> if(!own) events.incrementAndGet() }
        var previous:String?=null
        var lastEvents=-1
        var stableSamples=0
        try {
            repeat(24) {
                SystemClock.sleep(100)
                inst.runOnMainSync {
                    val now=events.get(); val key=SaathiSession.presentationKey()
                    if(now==lastEvents && previous!=null) {
                        assertEquals("Unchanged fallback must retain the presentation",previous,key)
                        stableSamples++
                    }
                    previous=key;lastEvents=now
                }
            }
            assertTrue("Observed stable presentation samples",stableSamples>=3)
        } finally { com.saathi.accessibility.ObservationDiagnostics.observer=null }
    }
    private fun exercise(web:Boolean) {
        assertTrue(android.os.Build.MODEL.startsWith("sdk_") || android.os.Build.FINGERPRINT.contains("generic"))
        val oldServices=shell("settings get secure enabled_accessibility_services");val oldEnabled=shell("settings get secure accessibility_enabled")
        val oldOverlay=Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)").find(shell("appops get com.saathi SYSTEM_ALERT_WINDOW"))?.groupValues?.get(1) ?: "default"
        val external=Intent().setComponent(ComponentName(inst.context.packageName,ExternalSurfaceActivity::class.java.name))
            .putExtra("reactive_form",true).putExtra("web_form",web).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        try {
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
            shell("settings put secure enabled_accessibility_services com.saathi/com.saathi.accessibility.SaathiAccessibilityService")
            shell("settings put secure accessibility_enabled 1")
            waitFor("Service") { com.saathi.accessibility.SaathiAccessibilityService.isConnected() }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { assertTrue(SaathiSession.startLive(it,"Help fill form",GuidanceLanguage.ENGLISH,false));it.startActivity(external) }
                val session=SaathiSession.sessionKey();val calls=PracticeGateway.requestsStarted.get()
                waitFor("First required field") { SaathiSession.instruction.value.contains("city field") }
                capture(if(web) "form-web-start" else "form-native-start")
                verifyUnchangedFallbackKeepsPresentation()
                action("City *","Fixturetown");action("Finish editing")
                waitFor("Next required, skipping optional") { SaathiSession.instruction.value.contains("state field") }
                capture(if(web) "form-web-next" else "form-native-next")
                action("City *","");action("Finish editing")
                waitFor("Clearing invalidates prior presence") { SaathiSession.instruction.value.contains("city field") }
                action("City *","Fixturetown");action("State *","Fictional");action("Finish editing")
                waitFor("No false validation or completion") { SaathiSession.instruction.value.contains("validity are not verified") }
                run {
                    action("Invalid city")
                    waitFor("Validation error") { SaathiSession.instruction.value.contains("reports an error") }
                    action("Correct city");action("Show dependent")
                    waitFor("Dynamic required field") { SaathiSession.instruction.value.contains("country field") }
                    action("Hide dependent")
                    waitFor("Removed field clears target") { SaathiSession.instruction.value.contains("validity are not verified") }
                }
                val root=requireNotNull(automation.rootInActiveWindow)
                val nodes=try { NodeMasker.flatten(root) } finally { root.recycle() }
                assertFalse(nodes.any { it.text?.contains("Fixturetown")==true || it.description?.contains("Fictional")==true })
                assertEquals(calls,PracticeGateway.requestsStarted.get());assertEquals(session,SaathiSession.sessionKey())
                assertTrue(SaathiSession.isActive())
                if(web) {
                    action("Private step")
                    waitFor("Private handoff retains form task") { SaathiSession.status.value == com.saathi.core.GuidanceSessionState.SENSITIVE_HANDOVER }
                    assertEquals(session,SaathiSession.sessionKey());assertTrue(SaathiSession.isActive())
                    capture("form-private-handoff")
                    val privateRoot=requireNotNull(automation.rootInActiveWindow)
                    val privateNodes=try { NodeMasker.flatten(privateRoot) } finally { privateRoot.recycle() }
                    assertTrue(privateNodes.any { it.structuralPrivateField })
                    assertFalse(privateNodes.any { it.text?.contains("fictional-secret-canary")==true || it.description?.contains("fictional-secret-canary")==true })
                    assertFalse(SaathiSession.canRecheckForm())
                    action("Hide dependent")
                    waitFor("Same form task resumes after private field disappears") { SaathiSession.instruction.value.contains("validity are not verified") }
                    assertEquals(session,SaathiSession.sessionKey());assertEquals(calls,PracticeGateway.requestsStarted.get())
                }
            }
        } finally {
            inst.runOnMainSync { SaathiSession.stop() }
            inst.targetContext.startActivity(Intent().setComponent(external.component).putExtra("close_fixture",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            for((key,value) in listOf("enabled_accessibility_services" to oldServices,"accessibility_enabled" to oldEnabled)) shell(if(value=="null"||value.isBlank()) "settings delete secure $key" else "settings put secure $key $value")
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW $oldOverlay");shell("input keyevent KEYCODE_HOME")
            inst.getUiAutomation(0)
        }
    }
    @Test fun nativeFieldsReactWithoutZoomRefreshOrValueAccess()=exercise(false)
    @Test fun webFieldsReactWithoutZoomRefreshOrValueAccess()=exercise(true)
}
