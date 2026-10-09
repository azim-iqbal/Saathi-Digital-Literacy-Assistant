package com.saathi.accessibility

/** Lazy access is intentional: masking after calling a value getter is too late. */
internal object NodeContentPolicy {
    data class Content(val text: String?, val description: String?, val sensitive: Boolean,
        val structuralPrivateField: Boolean, val hasValue: Boolean, val valueKnown: Boolean)

    fun read(password: Boolean, editable: Boolean, hint: String?, id: String?, inputType: Int,
             showingHint: Boolean, text: () -> String?, description: () -> String?): Content {
        // Android input-type constants, kept platform-independent for accessor regression tests.
        val variation = inputType and 0xfff
        val secretType = variation in setOf(0x81, 0x91, 0xe1, 0x12)
        val privateMetadata = SensitiveContent.isSensitive(password || secretType, hint, id)
        if (privateMetadata) return Content(null, null, true, editable || password || secretType, false, false)
        // An unlabelled editable field may also contain a secret. Never inspect its value or
        // content description to decide whether it is filled. Showing a hint is a structural
        // empty-field signal; absence of a hint is UNKNOWN, not proof of completion.
        if (editable) return Content(null, null, false, false, false, showingHint)
        val publicText = text()?.take(300)
        val publicDescription = description()?.take(300)
        val sensitive = SensitiveContent.isSensitive(false, publicText, publicDescription)
        return if (sensitive) Content(null, null, true, false, false, false)
        else Content(publicText, publicDescription, false, false, !publicText.isNullOrBlank(), true)
    }
}
