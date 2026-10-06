package com.saathi.orchestrator

import android.graphics.Rect
import com.saathi.core.GuideStep
import com.saathi.core.GuideTarget
import com.saathi.core.UiNode

/** Local-only form field guidance. Values are discarded; it never writes, clicks, or submits fields. */
internal object FormGuide {
    private val form = Regex("(?i)\\bforms?\\b|\\u092B\\u0949\\u0930\\u094D\\u092E|\\u092B\\u093C\\u0949\\u0930\\u094D\\u092E|\\u092B\\u093E\\u0930\\u094D\\u092E")
    private val helpAction = Regex("(?i)\\b(fill|filling|complete|completing|help|assist|bhar)\\b|\\u092D\\u0930")
    private val browserAddress = Regex(
        "(?i)(url|omnibox|address[_ ]?bar|search[_ ]?box[_ ]?text|search or type (?:a )?(?:web )?(?:address|url)|enter (?:a )?(?:web )?(?:address|url)|type (?:a )?(?:web )?(?:address|url))"
    )

    fun isRequest(request: String): Boolean = form.containsMatchIn(request) && helpAction.containsMatchIn(request)

    fun next(nodes: List<UiNode>, language: String): GuideStep {
        fun copy(en: String, hi: String, hinglish: String) = when (language) {
            "hi-IN" -> hi
            "hinglish" -> hinglish
            else -> en
        }
        fun noTarget(message: String) = GuideStep(message, language, null, "Wait for a safe visible form field.", false)

        if (nodes.any { it.isSensitive || it.isPassword }) return noTarget(copy(
            "This screen includes a private field. Fill it yourself; Saathi will not inspect or mark private fields.",
            "Is screen par niji field hai. Ise khud bharein; Saathi ise nahi padhega ya mark karega.",
            "Is screen par private field hai. Ise khud bharein; Saathi private fields ko nahi padhega ya mark karega."
        ))

        val fields = nodes.withIndex().filter { (_, node) ->
            node.isEnabled && node.isEditable && !node.isSensitive && !node.isPassword &&
                node.bounds.width() > 0 && node.bounds.height() > 0 &&
                !browserAddress.containsMatchIn(listOfNotNull(node.resourceId, node.hint, node.description, node.className).joinToString(" "))
        }
        val focused = fields.filter { it.value.isFocused }
        if (focused.size > 1) return noTarget(copy(
            "More than one form field appears focused, so I will not guess. Tap the field you want to fill.",
            "Ek se zyada form field focused hain, isliye main andaaza nahi lagaunga. Jis field ko bharna hai, use tap karein.",
            "Ek se zyada form fields focused hain, isliye main guess nahi karunga. Jis field ko bharna hai, use tap karein."
        ))

        val next = focused.singleOrNull() ?: fields.filterNot { it.value.hasValue }
            .minWithOrNull(compareBy<IndexedValue<UiNode>> { it.value.bounds.top }.thenBy { it.value.bounds.left })
        if (next == null) return noTarget(if (fields.isEmpty()) copy(
            "I cannot identify a safe text field here. Scroll until a form field is visible, then I will check again.",
            "Yahan safe text field nahi mila. Form field ko screen par laaiye; main phir jaanchunga.",
            "Yahan safe text field nahi mila. Form field ko screen par laaiye; main phir check karunga."
        ) else copy(
            "The visible fields look filled. Review them yourself, then scroll to the next section if needed.",
            "Dikh rahe fields bhare hue lagte hain. Khud jaanch lein, phir zaroorat ho to agle hissa tak scroll karein.",
            "Dikh rahe fields bhare hue lagte hain. Khud jaanch lein, phir zaroorat ho to agle section tak scroll karein."
        ))

        val node = next.value
        val instruction = copy(
            "Use the marker for this form field. Enter and review the value yourself; Saathi does not read, type, or submit it.",
            "Is form field ka nishaan dekhein. Jaankari khud bharein aur jaanchein; Saathi ise nahi padhta, likhta ya submit karta.",
            "Is form field ka marker dekhein. Jaankari khud bharein aur jaanchein; Saathi ise nahi padhta, likhta ya submit karta."
        )
        return GuideStep(
            instruction,
            language,
            GuideTarget(Rect(node.bounds), node.resourceId, "Form field", next.index),
            "Check the current screen after the user moves to another field.",
            false
        )
    }
}
