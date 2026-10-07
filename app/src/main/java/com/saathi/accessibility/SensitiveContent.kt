package com.saathi.accessibility

/** Conservative detection, not complete redaction. Cloud mode additionally excludes editable values. */
object SensitiveContent {
    private val cues = Regex(
        "(?i)(?<![\\p{L}\\p{N}])(pin|otp|password|cvv|mpin|passcode|dob|date[ _-]*of[ _-]*birth|birth[ _-]*date|aadhaar|aadhar|passport|account[ _-]*number|card[ _-]*number|security[ _-]*code|recovery[ _-]*code)(?![\\p{L}\\p{N}])|" +
            "पासवर्ड|पिन|ओटीपी|ओ[.]टी[.]पी|गुप्त|सुरक्षा[ ]*कोड|सीवीवी"
    )
    private val digits = Regex("(?<![\\p{L}\\p{N}])\\p{N}{4,}(?![\\p{L}\\p{N}])")
    private val groupedDigits = Regex("(?<![\\p{L}\\p{N}])\\p{N}(?:[ \\t-]*\\p{N}){3,}(?![\\p{L}\\p{N}])")
    private val addressPort = Regex("^(https?://)?((?:[A-Za-z0-9-]+\\.)+[A-Za-z0-9-]+|localhost):([0-9]{1,5})(?=/|$)", RegexOption.IGNORE_CASE)
    // Only explicit public formatting is exempt. Secrets win across ALL node properties.
    private val publicDate = Regex("(?<![\\p{L}\\p{N}])(?:[0-3]?[0-9][/-][01]?[0-9][/-](?:19|20)[0-9]{2}|(?:19|20)[0-9]{2}[/-][01]?[0-9][/-][0-3]?[0-9])(?![\\p{L}\\p{N}])")
    private val publicPrice = Regex("(?i)(?:₹|\\$|€|£|\\bINR|\\bRs\\.?)\\s*(?:[0-9]{1,3}(?:,[0-9]{2,3}){1,2}|[0-9]{1,7})(?:\\.[0-9]{1,2})?(?![0-9,.])")
    fun isSensitive(password: Boolean, vararg values: String?): Boolean {
        if (password) return true
        val fields = values.filterNotNull().map {
            java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFKC)
                .filterNot { char -> Character.getType(char) == Character.FORMAT.toInt() }
                .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        }
        if (fields.any { cues.containsMatchIn(it) }) return true
        return fields.any {
            val address = addressPort.find(it)
            val withoutPort = if (address != null && address.groupValues[3].toInt() in 1..65535)
                it.removeRange(address.groups[3]!!.range) else it
            // Never use display exceptions inside URLs: query/path numbers can be credentials.
            val publicDisplay = if (it.contains("://") || it.contains("?")) withoutPort else
                withoutPort.replace(publicDate, " ").replace(publicPrice, " ")
            digits.containsMatchIn(publicDisplay) || groupedDigits.containsMatchIn(publicDisplay)
        }
    }
}
