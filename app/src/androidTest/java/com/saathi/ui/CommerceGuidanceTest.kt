package com.saathi.ui

import android.app.UiAutomation
import android.content.ComponentName
import android.content.Intent
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.MainActivity
import com.saathi.accessibility.NodeMasker
import com.saathi.core.*
import com.saathi.gateway.PracticeGateway
import com.saathi.language.GuidanceLanguage
import com.saathi.orchestrator.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommerceGuidanceTest {
    private val inst=InstrumentationRegistry.getInstrumentation()
    private val automation get()=inst.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    private fun shell(command:String)=ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }
    private fun waitFor(message:String, predicate:()->Boolean) {
        val deadline=SystemClock.uptimeMillis()+12000
        while(!predicate() && SystemClock.uptimeMillis()<deadline) SystemClock.sleep(100)
        assertTrue("$message: ${SaathiSession.instruction.value}",predicate())
    }
    private fun nodes():List<UiNode> {
        val root=automation.rootInActiveWindow ?: return emptyList()
        return try { NodeMasker.flatten(root) } catch(_:IllegalStateException) { emptyList() } finally { root.recycle() }
    }
    private fun tap(bounds:Rect) {
        // The highlight is a separate overlay window. On the Samsung, a window transition can
        // briefly make it the active accessibility root even though the grounded external
        // control remains unchanged. Re-read only until that exact, already-grounded rectangle
        // is clickable; never substitute a nearby or newly chosen control.
        val deadline=SystemClock.uptimeMillis()+4_000
        var tapped=false
        while(!tapped && SystemClock.uptimeMillis()<deadline) {
            val root=automation.rootInActiveWindow
            if(root != null) {
                fun visit(node:AccessibilityNodeInfo) {
                    val r=Rect().also(node::getBoundsInScreen)
                    if(!tapped && r==bounds && node.isClickable) tapped=node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    for(i in 0 until node.childCount) node.getChild(i)?.let { try { visit(it) } finally { it.recycle() } }
                }
                try { visit(root) } finally { root.recycle() }
            }
            if(!tapped) SystemClock.sleep(100)
        }
        assertTrue("Grounded control is still clickable",tapped)
    }
    private fun capture(name:String) {
        SystemClock.sleep(180)
        val dir=java.io.File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: inst.targetContext.filesDir.path).apply { mkdirs() }
        automation.takeScreenshot()?.let { bitmap ->
            java.io.File(dir,"$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
        }
    }
    @Test fun realServiceFollowsProductHierarchyToReviewAndPrivateReturnWithoutBackend() = exercise(false)
    @Test fun startingOnAlreadyOpenQuietCommerceScreenObservesWithoutNewAppEvent() = exercise(true)
    private fun exercise(alreadyOpen: Boolean) {
        val oldServices=shell("settings get secure enabled_accessibility_services"); val oldEnabled=shell("settings get secure accessibility_enabled")
        val oldOverlay=Regex("SYSTEM_ALERT_WINDOW: (allow|ignore|deny|default)").find(shell("appops get com.saathi SYSTEM_ALERT_WINDOW"))?.groupValues?.get(1) ?: "default"
        val fixture=ComponentName(inst.context.packageName,ExternalSurfaceActivity::class.java.name)
        try {
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW allow")
            shell("settings put secure enabled_accessibility_services com.saathi/com.saathi.accessibility.SaathiAccessibilityService")
            shell("settings put secure accessibility_enabled 1")
            DeviceTestAccess.reconnect(automation)
            waitFor("Service connected") { com.saathi.accessibility.SaathiAccessibilityService.isConnected() }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val open=Intent().setComponent(fixture).putExtra("commerce_fixture",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                if(alreadyOpen) {
                    scenario.onActivity { it.startActivity(open) }
                    waitFor("Quiet fixture ready") { nodes().any { it.text == "Search" } }
                    SystemClock.sleep(800)
                    inst.runOnMainSync { assertTrue(SaathiSession.startLive(inst.targetContext,"Order milk",GuidanceLanguage.ENGLISH,false)) }
                } else scenario.onActivity {
                    assertTrue(SaathiSession.startLive(it,"Order milk",GuidanceLanguage.ENGLISH,false))
                    it.startActivity(open)
                }
                val session=SaathiSession.sessionKey(); val calls=PracticeGateway.requestsStarted.get()
                waitFor("Grounded search") { SaathiSession.instruction.value.contains("Use Search") }
                var screen=nodes()
                tap(requireNotNull(LiveGuide.plan("Order milk",screen,"en-IN",false).local.target).bounds)
                waitFor("Relevant Add") { SaathiSession.instruction.value.contains("marked Add") }
                screen=nodes(); assertFalse("No prices are private",screen.any { it.isSensitive })
                val target=requireNotNull(LiveGuide.plan("Order milk",screen,"en-IN",false).local.target)
                val parent=screen[target.nodeIndex!!].parentIndex
                assertNotNull(parent)
                assertTrue(screen.any { it.text=="Fixture milk" && it.parentIndex==parent })
                assertFalse(screen.any { it.text=="Fixture oat drink" && it.parentIndex==parent })
                waitFor("Overlay") { com.saathi.overlay.HighlightOverlayService.hasTarget() }
                capture("commerce-correct-add")
                tap(target.bounds)
                waitFor("Cart after observed quantity") { SaathiSession.instruction.value.contains("added/quantity") }
                screen=nodes();capture("commerce-observed-quantity")
                tap(requireNotNull(LiveGuide.plan("Order milk",screen,"en-IN",false).local.target).bounds)
                waitFor("Review, not purchase") { SaathiSession.instruction.value.contains("Saathi will not place") }
                assertFalse(com.saathi.overlay.HighlightOverlayService.hasTarget());capture("commerce-review")
                screen=nodes();tap(screen.single { it.text=="Fixture private step" }.bounds)
                waitFor("Private handoff") { SaathiSession.status.value==GuidanceSessionState.SENSITIVE_HANDOVER }
                screen=nodes();assertTrue(screen.any { it.structuralPrivateField });assertTrue(screen.any { it.text=="Order total ₹47" && !it.isSensitive })
                assertFalse(screen.any { it.text=="321" });capture("commerce-private-field-only")
                tap(screen.single { it.text=="Fixture return" }.bounds)
                waitFor("Same task returns to review") { SaathiSession.instruction.value.contains("Saathi will not place") }
                assertEquals(session,SaathiSession.sessionKey());assertEquals(calls,PracticeGateway.requestsStarted.get())
            }
        } finally {
            inst.runOnMainSync { SaathiSession.stop() }
            inst.targetContext.startActivity(Intent().setComponent(fixture).putExtra("close_fixture",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            for((key,value) in listOf("enabled_accessibility_services" to oldServices,"accessibility_enabled" to oldEnabled)) shell(if(value=="null"||value.isBlank()) "settings delete secure $key" else "settings put secure $key $value")
            shell("appops set com.saathi SYSTEM_ALERT_WINDOW $oldOverlay");shell("input keyevent KEYCODE_HOME");inst.getUiAutomation(0)
        }
    }
    private fun node(text:String?,parent:Int?,clickable:Boolean=false,editable:Boolean=false,kind:String="android.widget.TextView") =
        UiNode(Rect(10,(parent?:0)*100,300,(parent?:0)*100+80),text,null,null,null,kind,false,true,clickable,isEditable=editable,parentIndex=parent)
    @Test fun repeatedControlsRequireTheMatchingCardAndAmbiguousProductsStayUserChoices() {
        val tree=listOf(node(null,null),node(null,0),node(null,1),node("Fixture milk",2),node("₹47",2),node("ADD",2,true),
            node(null,1),node("Fixture bread",6),node("₹30",6),node("ADD",6,true))
        assertEquals(5,LiveGuide.plan("Order milk",tree,"en-IN",false).local.target?.nodeIndex)
        for((goal,title) in listOf("Order veg burger" to "Fixture veg burger", "Order soap" to "Fixture soap", "दूध मंगाओ" to "Fixture दूध", "milk order karo" to "Fixture milk")) {
            assertEquals(5,LiveGuide.plan(goal,tree.mapIndexed { i,n -> if(i==3)n.copy(text=title) else n },"en-IN",false).local.target?.nodeIndex)
        }
        val duplicate=tree.mapIndexed { i,n -> if(i==7)n.copy(text="Other milk") else n }
        assertNull(LiveGuide.plan("Order milk",duplicate,"en-IN",false).local.target)
        val missing=tree.mapIndexed { i,n -> if(i==5)n.copy(text="Unavailable",isClickable=false) else n }
        assertNull("Do not use another card's Add",LiveGuide.plan("Order milk",missing,"en-IN",false).local.target)
        assertNull("No positional fallback",LiveGuide.plan("Order milk",tree.map { it.copy(parentIndex=null) },"en-IN",false).local.target)
    }
    @Test fun promotionNeedsAnObservedDialogAndSafeNamedDismissal() {
        val tree=listOf(node(null,null),node(null,0,kind="android.app.Dialog"),node("Special offer",1),node("Not now",1,true))
        assertEquals(3,LiveGuide.plan("Order milk",tree,"en-IN",false).local.target?.nodeIndex)
        for(label in listOf("Special offer privacy consent","Security warning","Payment confirmation","Allow camera permission","OTP")) {
            val result=LiveGuide.plan("Order milk",tree.mapIndexed { i,n -> if(i==2)n.copy(text=label) else n },"en-IN",false)
            assertNull(label,result.local.target);assertFalse(result.useCloud)
        }
        assertNull(LiveGuide.plan("Order milk",tree.mapIndexed { i,n -> if(i==3)n.copy(text="X") else n },"en-IN",false).local.target)
        assertNull(LiveGuide.plan("Order milk",tree.mapIndexed { i,n -> if(i==1)n.copy(className="android.widget.LinearLayout") else n },"en-IN",false).local.target)
    }
}
