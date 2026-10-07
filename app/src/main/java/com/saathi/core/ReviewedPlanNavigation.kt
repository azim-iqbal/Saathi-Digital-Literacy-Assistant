package com.saathi.core

import java.net.URI

/** A local source-reading companion, not permission to execute a model's proposed step.
 * Only browser-chrome metadata may be passed as the location; page text is not an address.
 * Exact source-document matching does not prove TLS health, applicability or task completion.
 */
class ReviewedPlanNavigation(val plan: EvidencePlan, val stepId: String) {
    enum class State { UNAVAILABLE, SOURCE_MATCH, DETOUR, REVIEW_REQUIRED, EXPIRED }
    val step = requireNotNull(plan.steps.singleOrNull { it.id == stepId })
    val sourceUrl = requireNotNull(canonical(step.sourceUrl))
    var state = State.UNAVAILABLE; private set
    init { require(plan.reviewed && plan.next(Long.MIN_VALUE)?.id == stepId) }
    fun invalidate() { state = State.UNAVAILABLE }
    fun observe(browserLocation: String?, nowMs: Long): State {
        state = when {
            nowMs >= plan.expiresAtMs -> State.EXPIRED
            !plan.reviewed || plan.next(nowMs)?.id != stepId -> State.REVIEW_REQUIRED
            browserLocation == null -> State.UNAVAILABLE
            canonical(browserLocation) == sourceUrl -> State.SOURCE_MATCH
            else -> State.DETOUR
        }
        return state
    }
    fun message(locale: String): String {
        fun local(en: String, hi: String, hinglish: String) = when(locale) { "hi-IN" -> hi; "hinglish" -> hinglish; else -> en }
        val status = when(state) {
            State.SOURCE_MATCH -> local("The visible browser address matches the cited source. Read and verify the requirement yourself; this does not confirm completion or site safety.",
                "ब्राउज़र का दिख रहा पता स्रोत से मेल खाता है। शर्त खुद पढ़कर जाँचें; इससे काम पूरा होना या साइट की सुरक्षा साबित नहीं होती।",
                "Browser ka dikh raha pata source se milta hai. Shart khud padhkar jaanchein; isse kaam poora hona ya site ki safety saabit nahin hoti.")
            State.DETOUR -> local("This address is different from the cited source. Return to the source or to Saathi to review the plan. No step has been marked complete.",
                "यह पता दिए गए स्रोत से अलग है। स्रोत पर या योजना जाँचने के लिए साथी में लौटें। कोई कदम पूरा नहीं माना गया है।",
                "Yeh pata diye gaye source se alag hai. Source par ya plan jaanchne ke liye Saathi mein lautein. Koi kadam poora nahin maana gaya hai.")
            State.EXPIRED -> local("These sources have expired. Return to Saathi and refresh the research.", "इन स्रोतों का समय समाप्त हुआ। साथी में लौटकर फिर खोजें।", "In sources ka samay khatam hua. Saathi mein lautkar phir khojein.")
            State.REVIEW_REQUIRED -> local("Return to Saathi and review the current prerequisite before continuing.", "आगे बढ़ने से पहले साथी में लौटकर मौजूदा शर्त जाँचें।", "Aage badhne se pehle Saathi mein lautkar maujooda shart jaanchein.")
            State.UNAVAILABLE -> local("I cannot verify a full HTTPS address from the browser here. Return to Saathi to review the source. No navigation target is inferred.",
                "यहाँ ब्राउज़र का पूरा HTTPS पता जाँचना संभव नहीं है। स्रोत जाँचने के लिए साथी में लौटें। कोई विकल्प अनुमान से नहीं दिखाया जाएगा।",
                "Yahan browser ka poora HTTPS pata jaanchna mumkin nahin hai. Source jaanchne ke liye Saathi mein lautein. Koi option andaaze se nahin dikhaya jayega.")
        }
        return local("Original goal: ", "मूल काम: ", "Mool kaam: ") + plan.originalGoal + "\n" +
            local("Current prerequisite: ", "मौजूदा शर्त: ", "Maujooda shart: ") + step.title + "\n" + status
    }
    companion object {
        fun canonical(value: String): String? = try {
            require(value.length in 1..1024 && value.all { it.code in 33..126 } && '%' !in value && '\\' !in value)
            val url=URI(value)
            require(url.scheme == "https" && url.userInfo == null && url.rawQuery == null && url.rawFragment == null && url.port in setOf(-1,443))
            val host=requireNotNull(url.host).lowercase(java.util.Locale.ROOT)
            require(host.contains('.') && host.split('.').all { it.isNotEmpty() && !it.startsWith("xn--") } && !host.endsWith('.'))
            val path=url.rawPath.ifEmpty { "/" }
            require(path.split('/').none { it == "." || it == ".." } && !path.contains("//"))
            "https://$host$path"
        } catch (_: Exception) { null }
    }
}
