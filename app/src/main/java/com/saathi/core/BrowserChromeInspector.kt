package com.saathi.core

/** Browser chrome is a separate local channel: web content cannot supply location evidence.
 * The adapter owns/recycles nodes; the inspector never reads text outside a known address bar.
 */
object BrowserChromeInspector {
    interface Node {
        val owner: String
        val className: String
        val resourceId: String?
        val visible: Boolean
        val enabled: Boolean
        val focused: Boolean
        val password: Boolean
        val editable: Boolean
        val childCount: Int
        fun addressText(): String?
        fun child(index: Int): Node?
        fun release()
    }
    private val bars = mapOf("com.android.chrome" to "com.android.chrome:id/url_bar",
        "com.brave.browser" to "com.brave.browser:id/url_bar")
    fun read(root: Node): String? {
        val owner = root.owner
        val resource = bars[owner] ?: return null
        var count = 0
        var incomplete = false
        var candidates = 0
        var found: String? = null
        fun visit(node: Node, depth: Int) {
            if (++count > 150 || depth > 20) { incomplete = true; return }
            // Web content, including spoofed IDs, is deliberately outside this channel.
            if (node.className.contains("WebView")) return
            if (node.owner != owner) { incomplete = true; return }
            if (node.resourceId == resource && node.visible) {
                candidates++
                if (node.focused || node.password || node.editable || !node.enabled) incomplete = true
                else found = node.addressText()?.let(ReviewedPlanNavigation::canonical)
            }
            for (index in 0 until node.childCount) {
                if (count >= 150) { incomplete = true; break }
                val child = node.child(index)
                if (child == null) incomplete = true
                else try { visit(child, depth + 1) } finally { child.release() }
            }
        }
        return try { visit(root, 0); found.takeIf { !incomplete && candidates == 1 } }
        catch (_: RuntimeException) { null }
    }
}
