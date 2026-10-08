package com.saathi.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.saathi.core.BrowserChromeInspector

/** Reads supported browser chrome only. Never logs, uploads or saves address-bar values. */
object BrowserLocationReader {
    fun read(root: AccessibilityNodeInfo): String? = BrowserChromeInspector.read(AndroidNode(root))
    private class AndroidNode(private val node: AccessibilityNodeInfo) : BrowserChromeInspector.Node {
        override val owner get() = node.packageName?.toString().orEmpty()
        override val className get() = node.className?.toString().orEmpty()
        override val resourceId get() = node.viewIdResourceName
        override val visible get() = node.isVisibleToUser
        override val enabled get() = node.isEnabled
        override val focused get() = node.isFocused
        override val password get() = node.isPassword
        override val editable get() = node.isEditable
        override val childCount get() = node.childCount
        override fun addressText(): String? = node.text?.takeIf { it.length <= 1024 }?.toString()
        override fun child(index: Int): BrowserChromeInspector.Node? = node.getChild(index)?.let(::AndroidNode)
        @Suppress("DEPRECATION") override fun release() { node.recycle() }
    }
}
