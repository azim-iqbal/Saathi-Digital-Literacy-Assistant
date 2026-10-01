package com.saathi.core

import android.graphics.Rect

data class UiNode(
    val bounds: Rect,
    val text: String?,
    val description: String?,
    val hint: String?,
    val resourceId: String?,
    val className: String?,
    val isPassword: Boolean,
    val isEnabled: Boolean,
    val isClickable: Boolean,
    val isSensitive: Boolean = false,
    val hasValue: Boolean = !text.isNullOrBlank(),
    val clickableAncestorBounds: Rect? = null,
    val isEditable: Boolean = false
) {
    fun fingerprintPart() = listOf(resourceId, text, description, className, isEnabled, bounds.toShortString()).joinToString("|")
}

data class GuideTarget(val bounds: Rect, val resourceId: String? = null, val description: String, val nodeIndex: Int? = null)

data class GuideStep(
    val speechText: String,
    val language: String,
    val target: GuideTarget?,
    val expectedOutcome: String,
    val goalComplete: Boolean,
    val correctionNote: String? = null,
    val action: GuideAction = GuideAction.GUIDE
)

enum class GuideAction { GUIDE, REFUSE }

data class StepHistory(val instruction: String, val expectedOutcome: String)

/**
 * A user-facing session state. This is intentionally separate from speech state so the UI can
 * describe what Saathi is doing without claiming that it is listening or processing remotely.
 */
enum class GuidanceSessionState {
    STOPPED,
    PREPARING,
    OBSERVING,
    ANALYSING,
    GUIDING,
    WAITING_FOR_PRACTICE,
    SENSITIVE_HANDOVER,
    PAUSED,
    COMPLETED,
    ERROR
}
