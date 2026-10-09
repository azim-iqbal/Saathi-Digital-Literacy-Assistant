package com.saathi.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.saathi.core.UiNode

/** Editable value getters are not accessed. Static-content filtering remains best effort. */
object NodeMasker {

    fun flatten(root: AccessibilityNodeInfo, isCurrent: () -> Boolean = { true }): List<UiNode> {
        val nodes = mutableListOf<UiNode>()
        CompleteTreeWalk.visit(root, null as Rect?, { it.childCount }, { node, index -> node.getChild(index) },
            { node -> runCatching { node.recycle() }; Unit }, { node, ancestor -> read(node, nodes, ancestor) },
            isCurrent = isCurrent)
        // A private message/document need not contain digits. Discard the local copied content
        // as soon as the screen context is classified; keep only a non-content handoff flag.
        return if (com.saathi.core.PrivateContextPolicy.blocksCloud(nodes)) nodes.map {
            it.copy(text = null, description = null, hint = null, resourceId = null,
                hasValue = false, valueKnown = false, privateContext = true)
        } else nodes
    }

    private fun read(node: AccessibilityNodeInfo, into: MutableList<UiNode>, clickableAncestor: Rect?): Rect? {
        val bounds = Rect().also(node::getBoundsInScreen)
        if (node.isVisibleToUser && !bounds.isEmpty) {
            val hint = if (android.os.Build.VERSION.SDK_INT >= 26) node.hintText?.toString()?.take(300) else null
            val id = node.viewIdResourceName
            val className = node.className?.toString()
            val editable = node.isEditable || className.orEmpty().contains("EditText")
            val content = NodeContentPolicy.read(node.isPassword, editable, hint, id, node.inputType,
                node.isShowingHintText, { node.text?.toString() }, { node.contentDescription?.toString() })
            val sensitive = content.sensitive
            into += UiNode(
                bounds = bounds,
                text = content.text,
                description = content.description,
                hint = if (sensitive) null else hint,
                resourceId = if (sensitive) null else id,
                className = className,
                isPassword = node.isPassword,
                isEnabled = node.isEnabled,
                isClickable = node.isClickable,
                isSensitive = sensitive,
                hasValue = content.hasValue,
                valueKnown = content.valueKnown,
                structuralPrivateField = content.structuralPrivateField,
                clickableAncestorBounds = clickableAncestor?.let(::Rect),
                isEditable = editable,
                isFocused = node.isFocused
            )
        }
        return if (node.isClickable && node.isEnabled && node.isVisibleToUser && !bounds.isEmpty) bounds else clickableAncestor
    }

    fun isSensitive(isPassword: Boolean, vararg values: String?): Boolean =
        SensitiveContent.isSensitive(isPassword, *values)
}
