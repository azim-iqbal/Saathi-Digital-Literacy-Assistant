package com.saathi.accessibility

/** Conservative detection, not complete redaction. Cloud mode additionally excludes editable values. */
object SensitiveContent {
    private val cues = Regex(
        "(?i)(?<![\\p{L}\\p{N}])(pin|otp|password|cvv|mpin|passcode|security[ _-]*code|recovery[ _-]*code)(?![\\p{L}\\p{N}])|" +
            "पासवर्ड|पिन|ओटीपी|ओ[.]टी[.]पी|गुप्त|सुरक्षा[ ]*कोड|सीवीवी"
    )
    private val digits = Regex("(?<![\\p{L}\\p{N}])\\p{N}{4,}(?![\\p{L}\\p{N}])")
    private val addressPort = Regex("^(https?://)?((?:[A-Za-z0-9-]+\\.)+[A-Za-z0-9-]+|localhost):([0-9]{1,5})(?=/|$)", RegexOption.IGNORE_CASE)
    fun isSensitive(password: Boolean, vararg values: String?): Boolean = password || values.filterNotNull().any {
        val splitIdentifier = it.replace(Regex("([a-z])([A-Z])"), "$1 $2")
        // A URL's valid port is not a secret. Query/path numbers and all semantic cues still mask.
        val address = addressPort.find(it)
        val numericContent = if (address != null && address.groupValues[3].toInt() in 1..65535)
            it.removeRange(address.groups[3]!!.range) else it
        cues.containsMatchIn(splitIdentifier) || digits.containsMatchIn(numericContent)
    }
}
