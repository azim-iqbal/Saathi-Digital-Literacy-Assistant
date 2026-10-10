package com.saathi.orchestrator

import com.saathi.core.*

/** Recognized modal containers only. Never interprets an arbitrary X as permission to dismiss. */
internal object PopupGuide {
    enum class Kind { PROMOTIONAL_DISMISSIBLE, INFORMATIONAL, PERMISSION, CONSENT, SECURITY_WARNING, AUTHENTICATION, PAYMENT_CONFIRMATION, UNKNOWN }
    data class Classification(val kind: Kind, val closeIndex: Int? = null)
    private val promotion = Regex("(?i)\\b(special offer|limited time offer|newsletter|rate this app|subscription offer|promotional offer)\\b|विशेष ऑफर|खास ऑफर")
    private val consent = Regex("(?i)\\b(consent|privacy|cookies?|terms|agree)\\b|सहमति|शर्तें")
    private val permission = Regex("(?i)\\b(permission|allow access|microphone|camera|location access)\\b|अनुमति")
    private val security = Regex("(?i)\\b(security|certificate|unsafe|warning|malware|fraud)\\b|चेतावनी|सुरक्षा")
    private val payment = Regex("(?i)\\b(pay|purchase|payment|confirm|delete|remove|cancel order)\\b|भुगतान|पुष्टि")
    private val close = setOf("close", "dismiss", "skip", "no thanks", "not now", "maybe later", "बंद करें", "अभी नहीं", "छोड़ें")
    fun classify(nodes: List<UiNode>): Classification? {
        val dialogs = nodes.indices.filter { i ->
            // Android's standard dialog container ID is a platform contract, not an app adapter.
            nodes[i].className.orEmpty().contains("Dialog", true) || nodes[i].resourceId == "android:id/parentPanel"
        }
        if (dialogs.isEmpty()) return null
        if (dialogs.size != 1) return Classification(Kind.UNKNOWN)
        val members = CommerceGuide.descendants(nodes, dialogs.single())
        val labels = members.flatMap { CommerceGuide.labels(nodes[it]) }
        val kind = when {
            members.any { nodes[it].isSensitive || nodes[it].isPassword } -> Kind.AUTHENTICATION
            labels.any(security::containsMatchIn) -> Kind.SECURITY_WARNING
            labels.any(permission::containsMatchIn) -> Kind.PERMISSION
            labels.any(consent::containsMatchIn) -> Kind.CONSENT
            labels.any(payment::containsMatchIn) -> Kind.PAYMENT_CONFIRMATION
            labels.any(promotion::containsMatchIn) -> Kind.PROMOTIONAL_DISMISSIBLE
            else -> Kind.UNKNOWN
        }
        val controls = members.filter { CommerceGuide.action(nodes[it]) && CommerceGuide.isLabel(nodes[it], close) }
            .distinctBy { CommerceGuide.target(nodes, it, "").bounds.let { r -> listOf(r.left,r.top,r.right,r.bottom) } }
        return Classification(kind, controls.singleOrNull().takeIf { kind == Kind.PROMOTIONAL_DISMISSIBLE })
    }
    fun next(nodes: List<UiNode>, language: String): GuideStep? {
        val classified = classify(nodes) ?: return null
        val dismissible = classified.closeIndex != null
        val text = when (language) {
            "hi-IN" -> if (dismissible) "एक प्रचार पॉपअप रास्ते में है। चाहें तो चिन्हित विकल्प से बंद करें, फिर अपने काम पर लौटें।" else "इस संवाद को स्वयं पढ़कर निर्णय लें। साथी अनुमति, सहमति, भुगतान या सुरक्षा चेतावनी स्वीकार या बंद नहीं करेगा।"
            "hinglish" -> if (dismissible) "Ek promotional popup hai. Chahein toh marked option se band karein, phir apne task par lautein." else "Is dialog ko khud padhkar decision lein. Saathi permission, consent, payment ya security warning accept ya dismiss nahi karega."
            else -> if (dismissible) "A promotional popup is in the way. If you want to dismiss it, use the marked option; then return to your task." else "Review this dialog yourself. Saathi will not accept or dismiss permission, consent, payment or security choices."
        }
        return GuideStep(text, language, classified.closeIndex?.let { CommerceGuide.target(nodes, it, "Dismiss promotion") },
            "Reobserve after the user's dialog decision.", false)
    }
}
