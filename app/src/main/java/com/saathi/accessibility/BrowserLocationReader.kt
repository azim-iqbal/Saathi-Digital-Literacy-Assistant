package com.saathi.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.saathi.core.ReviewedPlanNavigation

/** Read only supported browser chrome, never a URL quoted by web content or typed into a form.
 * Does not infer an omitted HTTPS scheme, scrape WebViews, or retain query/fragment values.
 * Full-address visibility is browser/version dependent; unsupported states fail closed.
 */
object BrowserLocationReader {
    private val bars = mapOf("com.android.chrome" to "com.android.chrome:id/url_bar",
        "com.brave.browser" to "com.brave.browser:id/url_bar")
    fun read(root: AccessibilityNodeInfo): String? {
        val owner=root.packageName?.toString() ?: return null
        val resource=bars[owner] ?: return null
        var count=0; var ambiguous=false; var found:String?=null
        fun visit(node: AccessibilityNodeInfo, depth:Int) {
            if (++count > 150 || depth > 20 || node.className?.toString()?.contains("WebView") == true) return
            if (node.packageName?.toString() != owner) return
            if (node.viewIdResourceName == resource && node.isVisibleToUser) {
                if (found != null) ambiguous=true
                if (node.isFocused || node.isPassword || !node.isEnabled) { ambiguous=true; return }
                val value=node.text
                if (value == null || value.length > 1024) { ambiguous=true; return }
                found=ReviewedPlanNavigation.canonical(value.toString())
                if (found == null) ambiguous=true
            }
            for (index in 0 until node.childCount) {
                if (count >= 150) { ambiguous=true; break }
                node.getChild(index)?.let { child ->
                    try { visit(child,depth+1) } finally { @Suppress("DEPRECATION") child.recycle() }
                }
            }
        }
        visit(root,0)
        return found.takeUnless { ambiguous }
    }
}
