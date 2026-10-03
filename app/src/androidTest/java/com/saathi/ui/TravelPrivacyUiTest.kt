package com.saathi.ui

import android.content.ComponentName
import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.MainActivity
import com.saathi.accessibility.NodeMasker
import com.saathi.orchestrator.LiveGuide
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TravelPrivacyUiTest {
    @Test fun realTreeAllowsTravelDisplaysButWithholdsOtp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val target = ComponentName(instrumentation.context.packageName, ExternalSurfaceActivity::class.java.name)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            for (privateScreen in listOf(false, true)) {
                scenario.onActivity { it.startActivity(Intent().setComponent(target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .putExtra("travel_fixture", true).putExtra("private_fixture", privateScreen)) }
                var nodes = emptyList<com.saathi.core.UiNode>()
                val deadline = SystemClock.uptimeMillis() + 10000
                do {
                    val root = automation.rootInActiveWindow
                    nodes = if (root != null) try { NodeMasker.flatten(root) } finally { root.recycle() } else emptyList()
                    if (nodes.any { it.text == "Shopping" } && (!privateScreen || nodes.any { it.isSensitive })) break
                    SystemClock.sleep(100)
                } while (SystemClock.uptimeMillis() < deadline)
                assertTrue(nodes.any { it.text == "Shopping" })
                if (!privateScreen) {
                    assertFalse(nodes.any { it.isSensitive })
                    for (label in listOf("To", "01/10/2026", "Cheapest from ₹5221", "Shopping"))
                        assertNotNull(label, LiveGuide.next(label, nodes, "en-IN").target)
                } else {
                    assertTrue(nodes.any { it.isSensitive && it.text == null })
                    assertFalse(nodes.any { it.text?.contains("582139") == true })
                    assertNull(LiveGuide.next("To", nodes, "en-IN").target)
                }
            }
            scenario.onActivity { it.startActivity(Intent().setComponent(target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("close_fixture", true)) }
        }
    }
}
