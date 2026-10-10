package com.saathi.accessibility

/** Categories describe the evidence, not permission to transmit values or perform an action. */
enum class PrivacyKind {
    PUBLIC_DISPLAY, PUBLIC_ACTION, USER_INPUT_NORMAL, USER_INPUT_PERSONAL,
    SENSITIVE_AUTH, SENSITIVE_PAYMENT, UNKNOWN_SENSITIVE, NON_INTERACTIVE_DECORATION
}

object PrivacyClassification {
    private val payment = Regex("(?i)(?<![\\p{L}\\p{N}])(cvv|cvc|mpin|upi[ _-]*pin|atm[ _-]*pin|card[ _-]*(number|expiry|expiration)|payment[ _-]*(otp|authentication)|bank[ _-]*password)(?![\\p{L}\\p{N}])|सीवीवी")
    private val personal = Regex("(?i)(?<![\\p{L}\\p{N}])(name|address|phone|mobile|email|dob|birth[ _-]*date|date[ _-]*of[ _-]*birth|aadhaar|aadhar|passport|account[ _-]*number)(?![\\p{L}\\p{N}])|नाम|पता|मोबाइल")
    fun classify(password: Boolean, editable: Boolean, inputType: Int, hint: String?, id: String?,
                 redacted: Boolean, clickable: Boolean, hasPublicLabel: Boolean): PrivacyKind {
        val metadata = listOfNotNull(hint,id).joinToString(" ").let {
            java.text.Normalizer.normalize(it,java.text.Normalizer.Form.NFKC)
                .filterNot { c -> Character.getType(c)==Character.FORMAT.toInt() }
                .replace(Regex("([a-z])([A-Z])"),"$1 $2")
        }
        val secretType = (inputType and 0xfff) in setOf(0x81,0x91,0xe1,0x12)
        // Password semantics always beat weaker display/editability/context evidence.
        if (password || secretType || SensitiveContent.hasPrivateMetadata(metadata)) {
            if (payment.containsMatchIn(metadata)) return PrivacyKind.SENSITIVE_PAYMENT
            if (!password && !secretType && personal.containsMatchIn(metadata)) return PrivacyKind.USER_INPUT_PERSONAL
            return PrivacyKind.SENSITIVE_AUTH
        }
        if (redacted) return PrivacyKind.UNKNOWN_SENSITIVE
        if (editable) return if (personal.containsMatchIn(metadata)) PrivacyKind.USER_INPUT_PERSONAL else PrivacyKind.USER_INPUT_NORMAL
        if (clickable) return PrivacyKind.PUBLIC_ACTION
        return if (hasPublicLabel) PrivacyKind.PUBLIC_DISPLAY else PrivacyKind.NON_INTERACTIVE_DECORATION
    }
}
