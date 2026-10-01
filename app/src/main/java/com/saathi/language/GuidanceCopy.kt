package com.saathi.language

object GuidanceCopy {
    fun guardrailRedirect(language: GuidanceLanguage) = when (language) {
        GuidanceLanguage.ENGLISH -> "I can only help you complete tasks on your screen, like paying a bill or filling a form. Is there a task like that I can help with?"
        GuidanceLanguage.HINDI -> "मैं केवल आपकी स्क्रीन पर काम पूरा करने में मदद कर सकता हूँ - जैसे बिल भरना या फॉर्म भरना। क्या मैं ऐसे किसी काम में मदद करूँ?"
        GuidanceLanguage.HINGLISH -> HinglishTemplates.guardrailRedirect
    }
    fun offlineNotice(language: GuidanceLanguage) = when (language) {
        GuidanceLanguage.ENGLISH -> "I’m using offline guidance right now. I can still guide you through the demo flow."
        GuidanceLanguage.HINDI -> "अभी मैं ऑफलाइन मार्गदर्शन का उपयोग कर रहा हूँ। मैं आपको डेमो फ्लो में मार्गदर्शन देता रहूँगा।"
        GuidanceLanguage.HINGLISH -> HinglishTemplates.offlineNotice
    }
    fun lowConfidence(language: GuidanceLanguage) = when (language) {
        GuidanceLanguage.ENGLISH -> "I didn’t catch that clearly. Please say it again or type your goal."
        GuidanceLanguage.HINDI -> "मुझे बात साफ़ समझ नहीं आई। कृपया दोबारा बोलें या अपना लक्ष्य लिखें।"
        GuidanceLanguage.HINGLISH -> HinglishTemplates.lowConfidence
    }
    fun ttsSetup(language: GuidanceLanguage) = when (language) {
        GuidanceLanguage.ENGLISH -> "The selected voice is unavailable. Install it in Text-to-Speech settings, or use text guidance."
        GuidanceLanguage.HINDI -> "चुनी गई आवाज़ उपलब्ध नहीं है। उसे Text-to-Speech सेटिंग्स में इंस्टॉल करें या लिखित मार्गदर्शन चुनें।"
        GuidanceLanguage.HINGLISH -> "Chuni hui voice available nahi hai. Text-to-Speech settings mein install karein, ya text guidance use karein."
    }

    fun voiceIntro(language: GuidanceLanguage) = when (language) {
        GuidanceLanguage.ENGLISH -> "Voice guidance is on. I will describe one safe step at a time. You can say repeat, help, or cancel."
        GuidanceLanguage.HINDI -> "आवाज़ से मार्गदर्शन चालू है। मैं एक बार में एक सुरक्षित कदम बताऊँगा। आप दोबारा, मदद, या रद्द बोल सकते हैं।"
        GuidanceLanguage.HINGLISH -> "Voice guidance on hai. Main ek time par ek safe step bataunga. Aap repeat, help, ya cancel bol sakte hain."
    }

    fun voiceControlHint(language: GuidanceLanguage) = when (language) {
        GuidanceLanguage.ENGLISH -> "When you understand, say understood. Say help to hear it again, or cancel to stop guidance."
        GuidanceLanguage.HINDI -> "समझ आने पर समझ गया बोलें। दोबारा सुनने के लिए मदद, या मार्गदर्शन रोकने के लिए रद्द बोलें।"
        GuidanceLanguage.HINGLISH -> "Samajh aaye to samajh gaya boliye. Phir se sunne ke liye help, ya guidance rokne ke liye cancel boliye."
    }

    fun privateField(language: GuidanceLanguage) = when (language) {
        GuidanceLanguage.ENGLISH -> "Enter this privately. The conversation microphone is off while this form is visible."
        GuidanceLanguage.HINDI -> "इसे निजी रूप से स्वयं भरें। यह फॉर्म दिखने के दौरान बातचीत का माइक बंद है।"
        GuidanceLanguage.HINGLISH -> "Ise privately khud bhariye. Yeh form dikhne tak conversation mic band hai."
    }

    fun guidancePaused(language: GuidanceLanguage) = when (language) {
        GuidanceLanguage.ENGLISH -> "Guidance is paused. Nothing was submitted."
        GuidanceLanguage.HINDI -> "मार्गदर्शन रोक दिया गया है। कुछ भी सबमिट नहीं हुआ।"
        GuidanceLanguage.HINGLISH -> "Guidance pause ho gayi hai. Kuch bhi submit nahi hua."
    }

    fun acknowledged(language: GuidanceLanguage) = when (language) {
        GuidanceLanguage.ENGLISH -> "Take your time. I'll check the screen after your next tap. You can ask me to repeat, or say help."
        GuidanceLanguage.HINDI -> "आराम से करें। आपके अगले टैप के बाद मैं स्क्रीन जाँचूँगा। आप दोबारा या मदद बोल सकते हैं।"
        GuidanceLanguage.HINGLISH -> "Aaram se kijiye. Aapke agle tap ke baad main screen check karunga. Aap repeat ya help bol sakte hain."
    }

    fun voiceFallback(language: GuidanceLanguage, live: Boolean = false): String {
        if (live) return when (language) {
            GuidanceLanguage.ENGLISH -> "To change the option, say find followed by its visible name, such as find Help. You can also say repeat, pause or stop. I cannot plan multi-step tasks yet."
            GuidanceLanguage.HINDI -> "विकल्प बदलने के लिए उसका नाम और खोजो बोलें, जैसे मदद खोजो। आप दोबारा, रोकें या रद्द भी बोल सकते हैं। मैं अभी कई कदमों वाले काम की योजना नहीं बना सकता।"
            GuidanceLanguage.HINGLISH -> "Option badalne ke liye uska naam aur dhundo boliye, jaise Help dhundo. Repeat, pause ya stop bhi bol sakte hain. Abhi main multi-step task plan nahi kar sakta."
        }
        return when (language) {
        GuidanceLanguage.ENGLISH -> "I'm a local screen guide for now. I can repeat the current step or pause. To change your request, open the Saathi button."
        GuidanceLanguage.HINDI -> "अभी मैं स्क्रीन पर स्थानीय मदद करता हूँ। मैं कदम दोहरा सकता हूँ या रुक सकता हूँ। अनुरोध बदलने के लिए साथी बटन खोलें।"
        GuidanceLanguage.HINGLISH -> "Abhi main local screen guide hoon. Step repeat ya pause kar sakta hoon. Request badalne ke liye Saathi button kholiye."
        }
    }
}
