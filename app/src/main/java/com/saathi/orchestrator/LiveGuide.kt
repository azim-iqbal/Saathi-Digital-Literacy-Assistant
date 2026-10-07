package com.saathi.orchestrator

import com.saathi.accessibility.SensitiveContent
import com.saathi.core.GuideStep
import com.saathi.core.GuideTarget
import com.saathi.core.UiNode
import java.util.Locale

/** Local visible-option finder. No inferred workflows, remote reasoning, gestures or completion. */
object LiveGuide {
    private val prefixes = Regex("^(find|show|tap|click|open)\\s+", RegexOption.IGNORE_CASE)
    private val consequential = Regex("(?i)(?<![\\p{L}\\p{N}])(pay|purchase|buy|send|transfer|delete|remove|confirm|submit|install|allow|approve|accept|agree|reset|erase|password|pin|otp|permission)(?![\\p{L}\\p{N}])|भुगतान|भेज|मिटा|स्वीकार|अनुमति")
    fun label(request: String): String? {
        val label = request.trim().replace(prefixes, "").trim().trim('"', '“', '”')
        return label.takeIf { it.length in 2..80 && !it.contains('\n') &&
            !SensitiveContent.isSensitive(false, it) && !consequential.containsMatchIn(it) }
    }
    fun allowedPackage(name: String, own: String) = name.isNotBlank() && name != own &&
        name != "com.android.systemui" && name != "android" &&
        !name.contains("permissioncontroller") && !name.contains("packageinstaller") && !name.contains("inputmethod")
    private fun normalized(value: String) = value.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    private fun matches(desired: String, nodes: List<UiNode>): List<IndexedValue<UiNode>> =
        nodes.withIndex().filter { (_, node) -> node.isEnabled && !node.isEditable && (node.isClickable || node.clickableAncestorBounds != null) &&
            listOfNotNull(node.text, node.description).any { normalized(it) == normalized(desired) } }
            .distinctBy { indexed ->
                // Web accessibility can expose both a link and its identically labelled child.
                // Collapse only the same observed nonempty tap area, never separate controls.
                val node = indexed.value
                val area = if (node.isClickable) node.bounds else node.clickableAncestorBounds!!
                if (area.right > area.left && area.bottom > area.top)
                    listOf(area.left, area.top, area.right, area.bottom) else indexed.index
            }

    data class Plan(val local: GuideStep, val useCloud: Boolean)

    /** AI opt-in never overrides an exact local match or a privacy/ambiguity handover. */
    fun plan(request: String, nodes: List<UiNode>, language: String, cloudEnabled: Boolean): Plan {
        com.saathi.core.ScreenInterruption.reason(nodes)?.let {
            return Plan(GuideStep(com.saathi.core.ScreenInterruption.message(it, language), language, null,
                "Wait for a fresh screen after private user action.", false), useCloud = false)
        }
        if (com.saathi.core.BrowserConsentPolicy.present(nodes)) return Plan(
            GuideStep(com.saathi.core.BrowserConsentPolicy.message(language), language, null,
                "Wait for the user's privacy choice.", false), useCloud = false)
        com.saathi.core.PaymentSafety.state(nodes)?.let {
            return Plan(GuideStep(com.saathi.core.PaymentSafety.explanation(it, language), language, null,
                "Verify the transaction privately; no automatic retry or success inference.", false), useCloud = false)
        }
        if (com.saathi.core.ScreenErrorPolicy.present(nodes)) return Plan(
            GuideStep(com.saathi.core.ScreenErrorPolicy.message(language), language, null,
                "The user may choose consented research; no inferred retry or diagnosis.", false), useCloud = false)
        if (com.saathi.core.DestinationPolicy.requiresProvenance(request, nodes)) return Plan(
            GuideStep(com.saathi.core.DestinationPolicy.explanation(language), language, null,
                "Verified destination provenance is required.", false), useCloud = false)
        if (com.saathi.core.ResearchIntent.claimType(request) != null) return Plan(
            GuideStep(com.saathi.core.ResearchIntent.message(language), language, null,
                "Explicit consent and current evidence are required.", false), useCloud = false)
        // Form progression uses only local field metadata. Editable contents never go to a model.
        if (FormGuide.isRequest(request)) return Plan(FormGuide.next(nodes, language), useCloud = false)
        val local = next(request, nodes, language)
        val desired = label(request)
        val ambiguous = desired != null && matches(desired, nodes).size > 1
        val useCloud = cloudEnabled && local.target == null && !ambiguous &&
            nodes.none { it.isSensitive || it.isPassword } && com.saathi.core.LiveAiPolicy.allowed(request)
        return Plan(local, useCloud)
    }

    fun next(request: String, nodes: List<UiNode>, language: String): GuideStep {
        fun message(en: String, hi: String, hinglish: String) = when(language) { "hi-IN" -> hi; "hinglish" -> hinglish; else -> en }
        fun wait(text: String) = GuideStep(text, language, null, "Wait for a clear, current control.", false)
        if (nodes.any { it.isSensitive || it.isPassword }) return wait(message(
            "This screen contains private fields. Guidance and listening are paused here. Finish privately, then return to a non-private screen.",
            "इस स्क्रीन पर निजी जानकारी है। मार्गदर्शन और माइक रुके हैं। इसे स्वयं पूरा करके दूसरी स्क्रीन पर जाएँ।",
            "Is screen par private fields hain. Guidance aur mic ruke hain. Ise khud poora karke doosri screen par jaaiye."))
        val desired = label(request) ?: return wait(message(
            "Tell me the name of a visible navigation option, such as Settings or Help. I cannot confirm purchases, send, delete or handle secrets.",
            "स्क्रीन पर दिख रहे विकल्प का नाम बताएँ, जैसे Settings या Help। भुगतान, भेजना, मिटाना या गुप्त जानकारी स्वयं संभालें।",
            "Screen par dikh rahe option ka naam bataaiye, jaise Settings ya Help. Payment, send, delete aur secrets khud sambhaaliye."))
        val matches = matches(desired, nodes)
        if (matches.size != 1) return wait(message(
            if (matches.isEmpty()) "I cannot find that option on this screen. If you opened a different page, go back; or open Saathi to change your request." else "More than one option matches. I will not guess. Open Saathi to clarify your request.",
            if (matches.isEmpty()) "वह विकल्प इस स्क्रीन पर नहीं मिला। दूसरी जगह पहुँच गए हों तो वापस जाएँ, या साथी में अनुरोध बदलें।" else "एक से अधिक विकल्प मिले। साथी में अपना अनुरोध स्पष्ट करें।",
            if (matches.isEmpty()) "Woh option is screen par nahi mila. Doosri jagah pahunch gaye hon toh back jaaiye, ya Saathi mein request badlein." else "Ek se zyada options mile. Saathi mein request clear karein."))
        val match = matches.single()
        return GuideStep(message("Find “$desired” on this screen. Follow the marker if visible; if the app hides it, open Saathi for text help.",
            "इस स्क्रीन पर “$desired” देखें। निशान दिखे तो उसका उपयोग करें; छिपा हो तो साथी में लिखित मदद देखें।", "Is screen par “$desired” dekhiye. Marker dikhe toh use karein; chhupa ho toh Saathi mein text help dekhein."),
            language, GuideTarget(if (match.value.isClickable) match.value.bounds else match.value.clickableAncestorBounds!!, match.value.resourceId, desired, match.index),
            "Recheck the current screen after the user's tap.", false)
    }
}
