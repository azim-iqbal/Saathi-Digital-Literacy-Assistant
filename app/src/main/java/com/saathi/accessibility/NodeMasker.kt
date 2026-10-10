package com.saathi.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.saathi.core.UiNode

/** Editable value getters are not accessed. Static-content filtering remains best effort. */
object NodeMasker {

    private data class Inherited(val clickable: Rect? = null, val parent: Int? = null)

    fun flatten(root: AccessibilityNodeInfo, isCurrent: () -> Boolean = { true }): List<UiNode> {
        val nodes = mutableListOf<UiNode>()
        CompleteTreeWalk.visit(root, Inherited(), { it.childCount }, { node, index -> node.getChild(index) },
            { node -> runCatching { node.recycle() }; Unit }, { node, ancestor -> read(node, nodes, ancestor) },
            isCurrent = isCurrent)
        // A private message/document need not contain digits. Discard the local copied content
        // as soon as the screen context is classified; keep only a non-content handoff flag.
        return if (com.saathi.core.PrivateContextPolicy.blocksCloud(nodes)) nodes.map {
            it.copy(text = null, description = null, hint = null, resourceId = null,
                hasValue = false, valueKnown = false, privateContext = true,
                contentInvalid = false, requiredField = false, inputType = 0, privacyKind = PrivacyKind.UNKNOWN_SENSITIVE)
        } else nodes
    }

    private fun read(node: AccessibilityNodeInfo, into: MutableList<UiNode>, inherited: Inherited): Inherited {
        val bounds = Rect().also(node::getBoundsInScreen)
        var parent = inherited.parent
        if (node.isVisibleToUser && !bounds.isEmpty) {
            val hint = if (android.os.Build.VERSION.SDK_INT >= 26) node.hintText?.toString()?.take(300) else null
            val id = node.viewIdResourceName
            val className = node.className?.toString()
            val editable = node.isEditable || className.orEmpty().contains("EditText")
            val content = NodeContentPolicy.read(node.isPassword, editable, hint, id, node.inputType,
                node.isShowingHintText, { node.text?.toString() }, { node.contentDescription?.toString() }, node.textSelectionEnd)
            val sensitive = content.sensitive
            parent = into.size
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
                clickableAncestorBounds = inherited.clickable?.let(::Rect),
                parentIndex = inherited.parent,
                privacyKind = PrivacyClassification.classify(node.isPassword, editable, node.inputType, hint, id, sensitive,
                    node.isClickable || inherited.clickable != null, content.text != null || content.description != null),
                isEditable = editable,
                isFocused = node.isFocused,
                contentInvalid = !sensitive && node.isContentInvalid,
                // Optional absence of new platform metadata remains UNKNOWN, not OPTIONAL.
                requiredField = !sensitive && android.os.Build.VERSION.SDK_INT >= 36 &&
                    runCatching { AccessibilityNodeInfo::class.java.getMethod("isFieldRequired").invoke(node) == true }.getOrDefault(false),
                inputType = if (sensitive) 0 else node.inputType
            )
        }
        return Inherited(if (node.isClickable && node.isEnabled && node.isVisibleToUser && !bounds.isEmpty) bounds else inherited.clickable, parent)
    }

    fun isSensitive(isPassword: Boolean, vararg values: String?): Boolean =
        SensitiveContent.isSensitive(isPassword, *values)
}
