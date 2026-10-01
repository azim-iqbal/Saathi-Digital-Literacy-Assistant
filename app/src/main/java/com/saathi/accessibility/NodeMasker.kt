package com.saathi.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.saathi.core.UiNode

/** Best-effort local masking. Cloud guidance remains disabled; detection is not exhaustive. */
object NodeMasker {
    private const val MAX_NODES = 600

    fun flatten(root: AccessibilityNodeInfo): List<UiNode> {
        val nodes = mutableListOf<UiNode>()
        visit(root, nodes, 0, intArrayOf(0))
        return nodes
    }

    private fun visit(node: AccessibilityNodeInfo, into: MutableList<UiNode>, depth: Int, visited: IntArray, clickableAncestor: Rect? = null) {
        if (visited[0]++ >= MAX_NODES || depth > 50) return
        val bounds = Rect().also(node::getBoundsInScreen)
        if (node.isVisibleToUser && !bounds.isEmpty) {
            val rawText = node.text?.toString()?.take(300)
            val rawDescription = node.contentDescription?.toString()?.take(300)
            val hint = if (android.os.Build.VERSION.SDK_INT >= 26) node.hintText?.toString()?.take(300) else null
            val id = node.viewIdResourceName
            val sensitive = isSensitive(node.isPassword, hint, id, rawDescription, rawText)
            into += UiNode(
                bounds = bounds,
                text = if (sensitive) null else rawText,
                description = if (sensitive) null else rawDescription,
                hint = if (sensitive) null else hint,
                resourceId = id,
                className = node.className?.toString(),
                isPassword = node.isPassword,
                isEnabled = node.isEnabled,
                isClickable = node.isClickable,
                isSensitive = sensitive,
                hasValue = !rawText.isNullOrBlank(),
                clickableAncestorBounds = clickableAncestor?.let(::Rect),
                isEditable = node.isEditable
            )
        }
        for (index in 0 until node.childCount) {
            if (visited[0] >= MAX_NODES) return
            node.getChild(index)?.let { child ->
                val ancestor = if (node.isClickable && node.isEnabled && node.isVisibleToUser && !bounds.isEmpty) bounds else clickableAncestor
                try { visit(child, into, depth + 1, visited, ancestor) } finally { runCatching { child.recycle() } }
            }
        }
    }

    fun isSensitive(isPassword: Boolean, vararg values: String?): Boolean =
        SensitiveContent.isSensitive(isPassword, *values)
}
