package com.saathi.ui

import android.app.UiAutomation
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.accessibility.NodeMasker
import com.saathi.language.GuidanceLanguage
import com.saathi.orchestrator.SaathiSession
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*

/** Explicit opt-in observation only. No external taps, purchase, model call or raw screen dump. */
class PhysicalCommerceObservationTest {
    @Test fun observeConsentedPublicSurface() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val args=InstrumentationRegistry.getArguments()
        assertEquals("true",args.getString("physicalDeviceConfirmed"))
        val target=requireNotNull(args.getString("externalPackage"))
        require(target.matches(Regex("[a-zA-Z0-9_.]+")) && target!=inst.targetContext.packageName)
        val automation=inst.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        fun shell(command:String) = android.os.ParcelFileDescriptor.AutoCloseInputStream(
            automation.executeShellCommand(command)
        ).bufferedReader().use { it.readText().trim() }
        val output=java.io.File(inst.targetContext.filesDir,args.getString("evidenceName") ?: "physical-commerce").apply { mkdirs() }
        // `am instrument` on this Samsung splits some quoted multi-word extras.
        // Tests can provide requestEncoded with `~` as a space; production request parsing is unchanged.
        val request=(args.getString("requestEncoded")?.replace('~', ' ') ?: args.getString("request") ?: "Order milk")
        val failures=java.util.concurrent.ConcurrentHashMap<String,Int>()
        val events=java.util.concurrent.atomic.AtomicInteger()
        val times=java.util.concurrent.CopyOnWriteArrayList<Long>()
        com.saathi.accessibility.ObservationDiagnostics.failureObserver={ failures.merge(it,1,Integer::sum) }
        com.saathi.accessibility.ObservationDiagnostics.observer={ _,_,_,own -> if(!own) events.incrementAndGet() }
        com.saathi.accessibility.ObservationDiagnostics.snapshotObserver={ times.add(it) }
        try {
            DeviceTestAccess.reconnect(automation)
            val boundDeadline=SystemClock.uptimeMillis()+12000
            while(!com.saathi.accessibility.SaathiAccessibilityService.isConnected() && SystemClock.uptimeMillis()<boundDeadline) SystemClock.sleep(100)
            assertTrue("Accessibility service connected",com.saathi.accessibility.SaathiAccessibilityService.isConnected())
            inst.runOnMainSync { assertTrue(SaathiSession.startLive(inst.targetContext,request,GuidanceLanguage.ENGLISH,false)) }
            if (args.getString("reuseCurrentSurface") != "true") {
                val activity=requireNotNull(args.getString("externalActivity"))
                require(activity.matches(Regex("[a-zA-Z0-9_.$]+")))
                // On Android 16, an instrumentation context can retain USER_CURRENT_OR_SELF (-2),
                // which the device rejects for a cross-package Activity start.  Run the already
                // validated component explicitly as the foreground user instead.
                val launch=shell("am start --user current -n $target/$activity")
                assertTrue(
                    "External activity launch failed: $launch",
                    launch.contains("Starting:") || launch.contains("Status:") || launch.contains("Warning:")
                )
            }
            SystemClock.sleep(4000) // Allow the external app to settle; never certifies transient launch UI.
            val observationDeadline=SystemClock.uptimeMillis()+12000
            while(SaathiSession.instruction.value.isBlank() && SystemClock.uptimeMillis()<observationDeadline) SystemClock.sleep(100)

            // Samsung can briefly report no active accessibility root immediately after a
            // cross-app transition. Retry only for a fresh root; never reuse a prior tree.
            var root: android.view.accessibility.AccessibilityNodeInfo? = null
            val rootDeadline=SystemClock.uptimeMillis()+4_000
            while (root == null && SystemClock.uptimeMillis()<rootDeadline) {
                root=automation.rootInActiveWindow
                if (root == null) SystemClock.sleep(100)
            }
            requireNotNull(root)
            assertEquals(target,root.packageName.toString())
            val nodes=try { NodeMasker.flatten(root) } catch(error:IllegalStateException) { failures.merge(error.message.orEmpty(),1,Integer::sum);emptyList() } finally { root.recycle() }
            val public=Regex("(?i)^(?:search(?: .{0,60})?|add|add to cart|view cart|cart|[+-]|[1-9][0-9]?|quantity[: ]*[0-9]+|(?:₹|Rs[.]?)\\s*[0-9,.]+|[\\p{L} |()\\-]{0,80}milk[\\p{L} |()0-9.%-]{0,80})$")
            val rows=JSONArray()
            nodes.forEachIndexed { i,n ->
                val label=listOfNotNull(n.text,n.description,n.hint).firstOrNull { public.matches(it) }
                if (label != null || n.isSensitive || n.structuralPrivateField || n.isEditable) rows.put(
                    JSONObject().put("index",i).put("parent",n.parentIndex)
                        .put("bounds",n.bounds.toShortString()).put("class",n.className).put("editable",n.isEditable).put("clickable",n.isClickable)
                        .put("sensitive",n.isSensitive).put("privateField",n.structuralPrivateField).put("privateContext",n.privateContext)
                        .put("kind",n.privacyKind.name).put("publicLabel",label)
                )
            }
            val result=JSONObject().put("nodeCount",nodes.size).put("relevantNodes",rows).put("status",SaathiSession.status.value.name)
                .put("instruction",SaathiSession.instruction.value).put("overlay",com.saathi.overlay.HighlightOverlayService.hasTarget())
                .put("request",request).put("modelCalls",0).put("externalEvents",events.get()).put("snapshotMs",JSONArray(times)).put("failures",JSONObject(failures as Map<*,*>))
            java.io.File(output,"observation.json").writeText(result.toString(2))
            assertTrue("Service produced guidance",SaathiSession.instruction.value.isNotBlank())
        } finally {
            com.saathi.accessibility.ObservationDiagnostics.failureObserver=null
            com.saathi.accessibility.ObservationDiagnostics.observer=null
            com.saathi.accessibility.ObservationDiagnostics.snapshotObserver=null
            inst.runOnMainSync { SaathiSession.stop() };inst.getUiAutomation(0) }
    }
}
