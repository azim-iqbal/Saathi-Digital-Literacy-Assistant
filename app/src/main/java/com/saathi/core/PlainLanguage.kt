package com.saathi.core

/** Local glossary. Values surrounding a term are never returned, stored or transmitted. */
object PlainLanguage {
    private val entries = mapOf(
        "eligibility" to listOf("Eligibility means meeting a service’s conditions. Meeting some conditions does not guarantee approval.", "पात्रता का मतलब सेवा की शर्तें पूरी करना है। कुछ शर्तें पूरी होने से मंज़ूरी पक्की नहीं होती।", "Patrata ka matlab service ki shartein poori karna hai. Kuch shartein poori hone se manzoori pakki nahin hoti."),
        "verification" to listOf("Verification means checking information against reliable evidence or the official service.", "सत्यापन का मतलब भरोसेमंद प्रमाण या आधिकारिक सेवा से जानकारी जाँचना है।", "Verification ka matlab bharosemand saboot ya official service se jaankari jaanchna hai."),
        "kyc" to listOf("KYC is an organisation’s identity check. Complete it only with the verified service; Saathi does not need your identity documents.", "KYC संस्था की पहचान जाँच है। इसे सत्यापित सेवा पर ही पूरा करें; साथी को आपके पहचान दस्तावेज़ नहीं चाहिए।", "KYC sanstha ki pehchaan jaanch hai. Ise verified service par hi poora karein; Saathi ko aapke identity documents nahin chahiye."),
        "otp" to listOf("An OTP is a one-time security code. Enter it yourself only in the intended verified service. Do not tell Saathi or another person the code.", "OTP एक बार इस्तेमाल होने वाला सुरक्षा कोड है। इसे सही सत्यापित सेवा में स्वयं भरें। साथी या किसी व्यक्ति को कोड न बताएं।", "OTP ek baar istemaal hone wala security code hai. Ise sahi verified service mein khud bharein. Saathi ya kisi vyakti ko code na batayein."),
        "cvv" to listOf("CVV is a card security code. Keep it private; Saathi does not need it.", "CVV कार्ड का सुरक्षा कोड है। इसे निजी रखें; साथी को इसकी ज़रूरत नहीं है।", "CVV card ka security code hai. Ise private rakhein; Saathi ko iski zaroorat nahin hai."),
        "application number" to listOf("An application number identifies a submitted application. Keep it for checking status with the official service; a number alone does not mean approval.", "आवेदन संख्या जमा किए गए आवेदन की पहचान है। आधिकारिक सेवा पर स्थिति जाँचने के लिए इसे रखें; संख्या मिलने का मतलब मंज़ूरी नहीं है।", "Application number jama kiye gaye application ki pehchaan hai. Official service par status jaanchne ke liye ise rakhein; number milna manzoori nahin hai."),
        "pending" to listOf("Pending means the result is not final. For payments, verify the existing transaction before attempting another payment.", "लंबित का मतलब नतीजा अभी तय नहीं है। भुगतान दोबारा करने से पहले पुराने लेनदेन की स्थिति जाँचें।", "Pending ka matlab natija abhi tay nahin hai. Payment dobara karne se pehle purane transaction ki sthiti jaanchein.")
    )
    fun explain(term: String, locale: String): String? = entries[term.trim().lowercase(java.util.Locale.ROOT)]?.get(
        when(locale) { "hi-IN" -> 1; "hinglish" -> 2; else -> 0 })
    val terms: List<String> get() = entries.keys.toList()
}
