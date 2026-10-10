package com.saathi.core

/** Structural facts only. Fresh snapshots replace the entire form; values are never stored. */
enum class FormRequiredness { REQUIRED, OPTIONAL, UNKNOWN, CONDITIONAL }
enum class FormPresence { EMPTY, PRESENT, UNKNOWN }
enum class FormState { NOT_STARTED, ACTIVE, VALUE_PRESENT, VALIDATION_PENDING, VALID, INVALID, OPTIONAL_SKIPPED, USER_CONFIRMATION_REQUIRED, UNKNOWN }
data class FormField(val id: String, val label: String?, val presence: FormPresence,
                     val focused: Boolean, val invalid: Boolean, val enabled: Boolean,
                     val required: Boolean = false)
data class FormAssessment(val fields: List<FormField>, val states: Map<String, FormState>, val next: FormField?,
                          val ambiguous: Boolean = false, val completed: Boolean = false)

object ReactiveForm {
    private val optional = Regex("(?i)\\boptional\\b|वैकल्पिक|\\bvaikalpik\\b")
    private val conditional = Regex("(?i)required if|required when|चुने जाने पर आवश्यक|chune jaane par zaroori")
    private val required = Regex("(?i)\\brequired\\b|आवश्यक|अनिवार्य|\\bzaroori\\b|\\banivarya\\b")
    private val notRequired = Regex("(?i)\\bnot\\s+required\\b|(?:आवश्यक|अनिवार्य)\\s+नहीं|\\b(?:zaroori|anivarya)\\s+(?:nahi|nahin)\\b")
    private val notOptional = Regex("(?i)\\bnot\\s+optional\\b|वैकल्पिक\\s+नहीं|\\bvaikalpik\\s+(?:nahi|nahin)\\b")
    fun requiredness(label: String?, metadataRequired: Boolean = false): FormRequiredness {
        val text=label.orEmpty()
        // Negation is explicit evidence, not a substring match for "required".
        // Conflicting labels/platform metadata stay unknown; do not silently skip them.
        if (notOptional.containsMatchIn(text)) return FormRequiredness.UNKNOWN
        val opt=optional.containsMatchIn(text) || notRequired.containsMatchIn(text)
        val req=metadataRequired || text.trimEnd().endsWith('*') || required.containsMatchIn(notRequired.replace(text, ""))
        return when {
            opt && req -> FormRequiredness.UNKNOWN
            conditional.containsMatchIn(text) -> FormRequiredness.CONDITIONAL
            metadataRequired -> FormRequiredness.REQUIRED
            opt -> FormRequiredness.OPTIONAL
            req -> FormRequiredness.REQUIRED
            else -> FormRequiredness.UNKNOWN
        }
    }
    fun assess(fields: List<FormField>): FormAssessment {
        val visible=fields.filter { it.enabled }
        val ambiguous=fields.map { it.id }.distinct().size != fields.size || visible.count { it.focused }>1
        val states=fields.associate { f -> f.id to when {
            !f.enabled -> FormState.UNKNOWN
            f.invalid -> FormState.INVALID
            f.focused -> FormState.ACTIVE
            f.presence==FormPresence.PRESENT -> FormState.VALUE_PRESENT
            requiredness(f.label,f.required)==FormRequiredness.OPTIONAL -> FormState.OPTIONAL_SKIPPED
            f.presence==FormPresence.UNKNOWN -> FormState.USER_CONFIRMATION_REQUIRED
            else -> FormState.NOT_STARTED
        } }
        fun missing(f: FormField)=f.presence!=FormPresence.PRESENT
        val next=if(ambiguous) null else visible.firstOrNull { it.invalid && requiredness(it.label,it.required)==FormRequiredness.REQUIRED }
            ?: visible.firstOrNull { it.invalid }
            ?: visible.singleOrNull { it.focused }
            ?: visible.firstOrNull { requiredness(it.label,it.required)==FormRequiredness.REQUIRED && missing(it) }
            ?: visible.firstOrNull { requiredness(it.label,it.required) in setOf(FormRequiredness.CONDITIONAL,FormRequiredness.UNKNOWN) && missing(it) }
        // Presence can advance a local cursor after focus leaves, but is NEVER validation,
        // successful authentication, form readiness or a submission/completion predicate.
        return FormAssessment(fields,states,next,ambiguous)
    }
}
