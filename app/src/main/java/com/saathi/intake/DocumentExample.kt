package com.saathi.intake

/** A safe, non-personal illustration used when someone needs help identifying an upload. */
data class DocumentExample(
    val title: String,
    val caption: String,
    val fieldHint: String
)

object DocumentExamples {
    fun forField(fieldLabel: String): DocumentExample {
        val value = fieldLabel.lowercase()
        return when {
            listOf("photo", "image", "selfie", "passport").any(value::contains) -> DocumentExample(
                title = "Photo example",
                caption = "A clear, recent head-and-shoulders photo with your face visible.",
                fieldHint = "Choose a clear photo with good light; do not use someone else’s photo."
            )
            listOf("address", "residence", "utility", "rent").any(value::contains) -> DocumentExample(
                title = "Address proof example",
                caption = "A document that visibly shows your name and current address.",
                fieldHint = "Use an accepted document named by the form, such as a utility bill or official address record."
            )
            listOf("mark", "school", "college", "grade", "transcript").any(value::contains) -> DocumentExample(
                title = "Academic record example",
                caption = "A complete marksheet or transcript with your name and institution visible.",
                fieldHint = "Upload the complete page, not a cropped screenshot."
            )
            else -> DocumentExample(
                title = "Identity document example",
                caption = "An official document that shows your name and photo, when the form asks for identity proof.",
                fieldHint = "Follow the form’s accepted-document list and hide nothing unless the form explicitly allows it."
            )
        }
    }
}
