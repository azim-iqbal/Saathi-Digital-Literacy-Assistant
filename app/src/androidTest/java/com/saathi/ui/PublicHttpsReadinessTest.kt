package com.saathi.ui

import android.app.UiAutomation
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.accessibility.BrowserLocationReader
import com.saathi.accessibility.NodeMasker
import com.saathi.core.BrowserSafetyPolicy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Public entry pages only. No legal acceptance, credentials, CAPTCHA bypass or form submission. */
@RunWith(AndroidJUnit4::class)
class PublicHttpsReadinessTest {
    @Test fun inspectPublicHttpsAndPortalWithoutAssumingHiddenScheme() {
        assertTrue(android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.startsWith("sdk_"))
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val automation=instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        fun shell(command:String)=ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }
        val flags="/data/local/tmp/chrome-command-line"
        val backup="/data/local/tmp/saathi-public-https-flags-backup"
        val debug=shell("settings get global debug_app")
        val hadFlags=shell("ls $flags").contains(flags)
        check(!shell("ls $backup").contains(backup))
        if(hadFlags) shell("cp $flags $backup")
        automation.serviceInfo=automation.serviceInfo.apply { this.flags=this.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
        val rows=JSONArray()
        try {
            shell("am set-debug-app --persistent com.android.chrome")
            val pipe=automation.executeShellCommandRw("tee $flags")
            ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { it.write("chrome --disable-fre --no-first-run --no-default-browser-check\n".toByteArray()) }
            ParcelFileDescriptor.AutoCloseInputStream(pipe[0]).use { it.readBytes() }
            shell("am force-stop com.android.chrome")
            for(url in listOf("https://example.org/","https://cybercrime.gov.in/")) {
                shell("am start -a android.intent.action.VIEW -d $url -p com.android.chrome")
                SystemClock.sleep(8000)
                val root=automation.rootInActiveWindow ?: error("No browser root")
                try {
                    val nodes=NodeMasker.flatten(root)
                    val location=BrowserLocationReader.read(root)
                    val warning=BrowserSafetyPolicy.present(nodes)
                    val bars=root.findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar")
                    val bar=bars.singleOrNull()
                    val form=bar?.text?.toString()?.let { if(it.startsWith("https://")) "full_https" else if(it.contains("example.org")||it.contains("cybercrime.gov.in")) "scheme_omitted" else "other" } ?: "missing"
                    val editable=bar?.isEditable; val focused=bar?.isFocused
                    bars.forEach { @Suppress("DEPRECATION") it.recycle() }
                    val content=nodes.filter { it.resourceId?.startsWith("com.android.chrome:")!=true }.mapNotNull { it.text }.joinToString(" ")
                    val loaded=if(url.contains("example.org")) content.contains("Example Domain") else listOf("National Cyber Crime Reporting Portal","Report Cyber Crime","Financial Fraud").any { content.contains(it,true) }
                    val networkError=nodes.any { it.text?.let { t -> t.contains("ERR_")||t.contains("can't be reached") }==true }
                    rows.put(JSONObject().put("requested_url",url).put("public_content_observed",loaded).put("security_warning",warning)
                        .put("network_error",networkError).put("verified_full_browser_address_available",location!=null)
                        .put("exact_requested_address_match",location==url).put("address_form",form).put("address_editable",editable).put("address_focused",focused)
                        .put("scope","Read-only public entry; no protected walkthrough or source-authority certification"))
                } finally { @Suppress("DEPRECATION") root.recycle() }
            }
        } finally {
            File(instrumentation.targetContext.filesDir,"public-https-readiness.json").writeText(rows.toString(2))
            if(hadFlags) { shell("cp $backup $flags");shell("rm $backup") } else shell("rm $flags")
            if(debug=="null"||debug.isBlank()) shell("am clear-debug-app") else shell("am set-debug-app --persistent $debug")
            shell("am force-stop com.android.chrome")
        }
    }
}
