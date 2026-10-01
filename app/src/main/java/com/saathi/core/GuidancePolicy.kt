package com.saathi.core

import android.content.Context
import android.os.Build
import com.saathi.language.GuidanceLanguage

/**
 * A conservative compatibility policy. It never attempts to bypass another app's privacy
 * controls. Package matching is used only to provide an early, helpful explanation; a blank
 * or secure accessibility tree is handled the same way.
 */
object GuidancePolicy {
    private val restrictedPaymentPackages = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "in.amazon.mShop.android.shopping" to "Amazon Pay"
    )

    fun restrictedAppName(packageName: String?): String? = packageName?.let(restrictedPaymentPackages::get)

    fun isBrowser(packageName: String?) = packageName?.let {
        it.contains("chrome") || it.contains("browser") || it.contains("firefox") ||
            it.contains("samsung.internet") || it.contains("webview")
    } == true

    fun unavailableMessage(language: GuidanceLanguage, appName: String) = when (language) {
        GuidanceLanguage.ENGLISH -> "Our service is not available inside $appName because that app restricts screen guidance. You can continue in its official website, or use the payment app yourself only at the final payment step."
        GuidanceLanguage.HINDI -> "$appName में हमारी सेवा उपलब्ध नहीं है क्योंकि वह ऐप स्क्रीन मार्गदर्शन को प्रतिबंधित करता है। आप उसकी आधिकारिक वेबसाइट पर जारी रख सकते हैं, या अंतिम भुगतान चरण पर ऐप का इस्तेमाल स्वयं करें।"
        GuidanceLanguage.HINGLISH -> "$appName ke andar hamari service available nahi hai kyunki app screen guidance ko restrict karta hai. Aap official website par continue kar sakte hain, ya final payment step par app khud use kijiye."
    }

    fun secureScreenMessage(language: GuidanceLanguage) = when (language) {
        GuidanceLanguage.ENGLISH -> "This screen does not share enough safe information for guidance. I will wait here; you can return to the official website or say cancel."
        GuidanceLanguage.HINDI -> "यह स्क्रीन सुरक्षित मार्गदर्शन के लिए पर्याप्त जानकारी साझा नहीं करती। मैं यहीं प्रतीक्षा करूँगा; आप आधिकारिक वेबसाइट पर लौट सकते हैं या रद्द बोल सकते हैं।"
        GuidanceLanguage.HINGLISH -> "Yeh screen safe guidance ke liye enough information share nahi karti. Main yahin wait karunga; aap official website par wapas ja sakte hain ya cancel bol sakte hain."
    }

    fun websiteFirstChoices() = listOf("Official provider website", "IRCTC website", "Airline website", "Other website")

    data class DeviceProfile(
        val manufacturer: String,
        val androidVersion: Int,
        val overlaySupported: Boolean,
        val accessibilitySupported: Boolean,
        val captureExplanation: String
    )

    fun deviceProfile(context: Context) = DeviceProfile(
        manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
        androidVersion = Build.VERSION.SDK_INT,
        overlaySupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
        accessibilitySupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
        captureExplanation = "Saathi uses Android's own consent-based capture, not the phone's Screen Recorder. Some apps can still block all capture and screen reading for privacy."
    )
}
