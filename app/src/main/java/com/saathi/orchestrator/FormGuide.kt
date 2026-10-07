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
    private val knownFieldNames = mapOf(
        "first name" to "first name", "given name" to "first name",
        "last name" to "last name", "family name" to "last name", "surname" to "last name",
        "middle name" to "middle name", "full name" to "full name", "name" to "name",
        "email" to "email", "e mail" to "email", "email address" to "email",
        "phone" to "phone number", "phone number" to "phone number",
        "mobile" to "mobile number", "mobile number" to "mobile number",
        "address" to "address", "street address" to "address", "city" to "city",
        "state" to "state", "country" to "country", "postal code" to "postal code",
        "zip code" to "postal code", "company" to "company", "website" to "website",
        "username" to "username", "subject" to "subject", "message" to "message",
        "comments" to "comments"
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
        val fieldName = fieldName(next.index, node, fields, nodes)
        val instruction = if (fieldName == null) copy(
            "Fill the highlighted field. Enter and review the value yourself; Saathi does not read, type, or submit it.",
            "Nishaan wale field mein jaankari khud bharein aur jaanchein; Saathi ise nahi padhta, likhta ya submit karta.",
            "Marker wale field mein jaankari khud bharein aur jaanchein; Saathi ise nahi padhta, likhta ya submit karta."
        ) else copy(
            "Fill the $fieldName field. Enter and review the value yourself; Saathi does not read, type, or submit it.",
            "$fieldName field mein jaankari khud bharein aur jaanchein; Saathi ise nahi padhta, likhta ya submit karta.",
            "$fieldName field mein jaankari khud bharein aur jaanchein; Saathi ise nahi padhta, likhta ya submit karta."
        )
        return GuideStep(
            instruction,
            language,
            GuideTarget(Rect(node.bounds), node.resourceId, "Form field", next.index),
            "Check the current screen after the user moves to another field.",
            false
        )
    }

    /** Speak only a small allowlist of field labels, never arbitrary page text or entered values. */
    private fun fieldName(
        nodeIndex: Int,
        field: UiNode,
        fields: List<IndexedValue<UiNode>>,
        nodes: List<UiNode>
    ): String? {
        label(field.hint)?.let { return it }
        val id = field.resourceId?.substringAfterLast('/')?.substringAfterLast(':')
        label(id)?.let { return it }

        val labels = nodes.mapIndexedNotNull { index, candidate ->
            if (candidate.isEditable || candidate.isSensitive || candidate.isPassword) return@mapIndexedNotNull null
            val name = label(candidate.text) ?: label(candidate.description) ?: return@mapIndexedNotNull null
            IndexedValue(index, candidate to name)
        }
        val nearby = labels.filter { (_, pair) ->
            val bounds = pair.first.bounds
            val verticalOverlap = bounds.bottom > field.bounds.top && bounds.top < field.bounds.bottom
            val directlyAbove = bounds.bottom <= field.bounds.top &&
                field.bounds.top - bounds.bottom <= (field.bounds.height() * 2).coerceAtLeast(80)
            val horizontalOverlap = bounds.right > field.bounds.left && bounds.left < field.bounds.right
            horizontalOverlap && (verticalOverlap || directlyAbove)
        }.minByOrNull { (_, pair) ->
            kotlin.math.abs(field.bounds.top - pair.first.bounds.bottom)
        } ?: return null

        val anchor = nearby.value.second
        if (anchor != "name") return anchor

        // Some pages expose one shared "Name" label above separate first/last-name inputs.
        val anchorBounds = nearby.value.first.bounds
        val nextLabelTop = labels.asSequence().map { it.value.first }
            .filter { it.bounds.top >= anchorBounds.bottom && it.bounds.top > anchorBounds.top }
            .minOfOrNull { it.bounds.top } ?: Int.MAX_VALUE
        val group = fields.filter { (_, candidate) ->
            candidate.bounds.top >= anchorBounds.bottom && candidate.bounds.top < nextLabelTop &&
                candidate.bounds.right > anchorBounds.left && candidate.bounds.left < anchorBounds.right
        }.sortedBy { it.value.bounds.top }
        val position = group.indexOfFirst { it.index == nodeIndex }
        if (group.size == 2 && position in 0..1) return if (position == 0) "first name" else "last name"
        return anchor
    }

    private fun label(raw: String?): String? {
        val value = raw?.replace(Regex("([a-z])([A-Z])"), "$1 $2")
            ?.replace(Regex("[_-]+"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.trimEnd(':', '*', '.')
            ?.lowercase(java.util.Locale.ROOT)
            ?: return null
        if ('@' in value) return "email"
        return knownFieldNames[value]
    }
}
