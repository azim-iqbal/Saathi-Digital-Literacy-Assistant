package com.saathi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saathi.core.EvidencePlan
import com.saathi.core.GatewayCancellation
import com.saathi.core.GatewayResult
import com.saathi.gateway.PracticeGateway
import com.saathi.language.GuidanceLanguage
import com.saathi.orchestrator.SaathiSession
import com.saathi.ui.*
import com.saathi.ui.glass.*
import com.saathi.ui.navigation.rememberNavigationEnvironment
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

/** Separate consented research. No screen tree, private form, or chat history is attached. */
class ResearchActivity : ComponentActivity() {
    companion object {
        // One in-memory handoff; no task text in Intent extras, saved state or disk.
        private var nextGoal = ""
        private var nextPurpose: String? = null
        fun open(context: android.content.Context, goal: String, purpose: String? = null) {
            nextPurpose = purpose?.takeIf { it == "outage" }
            nextGoal = goal.take(160)
            context.startActivity(android.content.Intent(context, ResearchActivity::class.java))
        }
    }
    private var plan by mutableStateOf<EvidencePlan?>(null)
    private var planRevision by mutableIntStateOf(0)
    private var openingSource = false
    private var pending: GatewayCancellation? = null
    private var busy by mutableStateOf(false)
    private var report by mutableStateOf("")
    private var researchId by mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        // Opening this screen does not let new retrieved text affect an active guide.
        if (SaathiSession.isActive()) SaathiSession.pause()
        plan = SaathiSession.reviewedPlan()?.also { it.invalidateContext() }
        val initialGoal = nextGoal.also { nextGoal = "" }
        val initialPurpose = nextPurpose.also { nextPurpose = null }
        if (initialGoal.isNotBlank() || initialPurpose != null) {
            SaathiSession.discardReviewedPlan(plan); plan = null
        }
        setContent {
            val preferences = remember { Preferences(this) }
            fun local(en: String, hi: String, hinglish: String) = when(preferences.language) {
                GuidanceLanguage.HINDI -> hi
                GuidanceLanguage.HINGLISH -> hinglish
                else -> en
            }
            var goal by remember { mutableStateOf(plan?.originalGoal ?: initialGoal) }
            var jurisdiction by remember { mutableStateOf("") }
            var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) {
                lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                    while (isActive) { currentTime = System.currentTimeMillis(); delay(1000) }
                }
            }
            var explainTerms by remember { mutableStateOf(false) }
            var sourcePrompt by remember { mutableStateOf<com.saathi.core.EvidenceStep?>(null) }
            var purpose by remember { mutableStateOf(initialPurpose ?: com.saathi.core.ResearchIntent.claimType(initialGoal) ?: "requirements") }
            var consent by remember { mutableStateOf(false) }
            val dark = preferences.theme == "Dark" || (preferences.theme == "System" && isSystemInDarkTheme())
            val colors = saathiColorScheme(dark)
            val environment = rememberNavigationEnvironment()
            MaterialTheme(colorScheme = colors) {
                CompositionLocalProvider(LocalContentColor provides colors.onSurface,
                    LocalGlass provides GlassEnvironment(dark = dark, reducedMotion = preferences.reducedMotion || !environment.animationsEnabled)) {
                    Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding()
                        .wrapContentWidth(androidx.compose.ui.Alignment.CenterHorizontally).widthIn(max = 720.dp)
                        .imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        SaathiBrand()
                        Text(local("Research requirements", "ज़रूरी शर्तें खोजें", "Zaroori shartein khojein"), style = MaterialTheme.typography.titleLarge)
                        Text(local("Describe the task without personal details. Your server checks its reviewed sources; results may be incomplete.",
                            "निजी जानकारी दिए बिना काम बताएं। सर्वर जाँचे हुए स्रोत खोजता है; नतीजे अधूरे हो सकते हैं।",
                            "Niji jaankari ke bina kaam batayein. Server jaanche hue sources khojta hai; natije adhoore ho sakte hain."))
                        OutlinedTextField(goal, { goal = it.take(160); researchId = null; report = ""; discardPlan() }, enabled = !busy,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), label = { Text(local("Task", "काम", "Kaam")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(jurisdiction, { jurisdiction = it.take(80); researchId = null; report = ""; discardPlan() }, enabled = !busy,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), label = { Text(local("Country / state or region", "देश / राज्य या क्षेत्र", "Desh / rajya ya kshetra")) }, modifier = Modifier.fillMaxWidth())
                        for ((kind, label) in listOf(
                            "requirements" to local("Requirements", "ज़रूरी शर्तें", "Zaroori shartein"),
                            "eligibility" to local("Eligibility", "पात्रता", "Patrata"),
                            "deadline" to local("Deadline", "अंतिम तारीख", "Aakhri tareekh"),
                            "pricing" to local("Fees / pricing", "शुल्क / कीमत", "Shulk / keemat"),
                            "outage" to local("Service issue", "सेवा की समस्या", "Service ki samasya"))) {
                            FilterChip(selected = purpose == kind, enabled = !busy,
                                onClick = { purpose = kind; researchId = null; report = ""; discardPlan() }, label = { Text(label) })
                        }
                        Text(local("Only this task and region go to your backend. No screen content is sent. A proposed plan uses both configured AI providers only after a separate tap.",
                            "सिर्फ यह काम और क्षेत्र बैकएंड को भेजे जाएंगे, स्क्रीन की जानकारी नहीं। योजना बनाने के लिए अलग से सहमति देने पर दोनों AI सेवाएं इस्तेमाल होंगी।",
                            "Sirf yeh kaam aur kshetra backend ko jayenge, screen ki jaankari nahin. Plan ke liye alag se tap karne par dono AI services istemaal hongi."))
                        Row { Checkbox(consent, { consent = it }, enabled = !busy); Text(local("Allow this research request", "इस खोज की अनुमति दें", "Is khoj ki anumati dein")) }
                        GlassButton(local("Find sources", "स्रोत खोजें", "Sources khojein"), enabled = consent && !busy && goal.isNotBlank() && jurisdiction.isNotBlank(), onClick = {
                            busy = true; report = ""; researchId = null; discardPlan()
                            pending = PracticeGateway.research(goal, jurisdiction, preferences.language.apiTag, consent, purpose) { result ->
                                busy = false; pending = null
                                if (result is GatewayResult.Research) { report = result.report; if (result.hasEvidence) researchId = result.requestId }
                                else report = failure(result)
                            }
                        })
                        researchId?.let { id ->
                            Text(local("Review the source excerpts before requesting a proposed plan. Model interpretation can be wrong; this is not an eligibility decision.",
                                "योजना माँगने से पहले स्रोत पढ़ें। AI की व्याख्या गलत हो सकती है; यह पात्रता का निर्णय नहीं है।",
                                "Plan maangne se pehle sources padhein. AI ki samajh galat ho sakti hai; yeh patrata ka faisla nahin hai."))
                            GlassButton(if (purpose == "outage") local("Allow AI issue review", "AI समस्या समीक्षा की अनुमति दें", "AI samasya review ki anumati dein") else local("Allow AI plan proposal", "AI योजना प्रस्ताव की अनुमति दें", "AI plan proposal ki anumati dein"), enabled = !busy, onClick = {
                                busy = true; researchId = null
                                pending = PracticeGateway.planResearch(id, true, preferences.language.apiTag) { result ->
                                    busy = false; pending = null
                                    if (result is GatewayResult.Research) { report = result.report; plan = result.plan; planRevision++ }
                                    else report = failure(result)
                                }
                            })
                        }
                        if (busy) {
                            Text(local("Checking…", "जाँच जारी है…", "Jaanch jaari hai…"))
                            GlassButton(local("Cancel", "रद्द करें", "Radd karein"), primary = false, onClick = { cancelRequest() })
                        }
                        plan?.let { checklist ->
                            val revision = planRevision
                            val now = currentTime
                            Text(local("Original goal: ", "मूल काम: ", "Mool kaam: ") + checklist.originalGoal)
                            if (!checklist.reviewed) GlassButton(local("I reviewed applicability", "मैंने लागू शर्तें जाँची हैं", "Maine lagu shartein jaanchi hain"), primary = false, onClick = {
                                if (!checklist.review(System.currentTimeMillis())) report = local("Refresh the sources before continuing.", "आगे बढ़ने से पहले स्रोत फिर खोजें।", "Aage badhne se pehle sources phir khojein.")
                                planRevision = revision + 1
                            })
                            checklist.next(now)?.let { step ->
                                Text(local("Next prerequisite / step", "अगली शर्त / कदम", "Agli shart / kadam"), style = MaterialTheme.typography.titleMedium)
                                Text(step.title)
                                Text("“${step.quote}”\n${step.sourceTitle}\n${step.sourceUrl}")
                                GlassButton(local("Read source in browser", "ब्राउज़र में स्रोत पढ़ें", "Browser mein source padhein"), primary = false, onClick = { sourcePrompt = step })
                                GlassButton(local("I completed this step", "मैंने यह कदम पूरा किया", "Maine yeh kadam poora kiya"), onClick = {
                                    checklist.confirm(step.id, System.currentTimeMillis()); planRevision = revision + 1
                                })
                            }
                            for (criterion in checklist.criteria) {
                                Text("“${criterion.quote}”\n${criterion.sourceTitle}\n${criterion.sourceUrl}")
                                Text(local("Do you meet this condition? Answer only yes, no or unknown; keep personal values private.",
                                    "क्या यह शर्त पूरी है? सिर्फ हाँ, नहीं या पता नहीं बताएं; निजी जानकारी न दें।",
                                    "Kya yeh shart poori hai? Sirf haan, nahin ya pata nahin batayein; niji jaankari na dein."))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GlassButton(local("Yes", "हाँ", "Haan"), primary = false, onClick = { checklist.setFact(criterion.id, true); planRevision++ })
                                    GlassButton(local("No", "नहीं", "Nahin"), primary = false, onClick = { checklist.setFact(criterion.id, false); planRevision++ })
                                }
                                GlassButton(local("Unknown", "पता नहीं", "Pata nahin"), primary = false, onClick = { checklist.setFact(criterion.id, null); planRevision++ })
                            }
                            Text(when (checklist.eligibility(now)) {
                                "POSSIBLY_ELIGIBLE" -> local("Possibly eligible based on your answers; official verification and complete criteria are still needed.", "आपके उत्तरों के आधार पर संभव पात्रता; पूरी शर्तें और आधिकारिक जाँच अभी बाकी हैं।", "Aapke jawaabon se sambhav patrata; poori shartein aur official jaanch abhi baaki hai.")
                                "CONDITION_NOT_MET" -> local("Your answers indicate a condition is not met. Verify with the official service.", "आपके उत्तर के अनुसार एक शर्त पूरी नहीं है। आधिकारिक सेवा से जाँचें।", "Aapke jawaab ke mutabik ek shart poori nahin hai. Official service se jaanchein.")
                                else -> local("Eligibility not established; information or applicability review is missing.", "पात्रता तय नहीं है; जानकारी या शर्तों की जाँच बाकी है।", "Patrata tay nahin hai; jaankari ya sharton ki jaanch baaki hai.")
                            })
                            if (checklist.complete(now)) Text(local("Checklist confirmed by you. This does not verify an application, payment or official outcome.", "आपने सूची पूरी होने की पुष्टि की है। यह आवेदन, भुगतान या आधिकारिक परिणाम का प्रमाण नहीं है।", "Aapne checklist poori hone ki pushti ki hai. Yeh application, payment ya official natije ka pramaan nahin hai."))
                        }
                        sourcePrompt?.let { proposed ->
                            AlertDialog(onDismissRequest = { sourcePrompt = null },
                                title = { Text(local("Open this source?", "यह स्रोत खोलें?", "Yeh source kholein?")) },
                                text = { Text(proposed.sourceUrl + "\n\n" + local(
                                    "Your browser will contact this website. Saathi can help you keep your current prerequisite in view, but will not fill forms, select actions or mark it complete. If the full address is hidden, source matching is unavailable.",
                                    "ब्राउज़र इस वेबसाइट से जुड़ेगा। साथी मौजूदा शर्त याद रखने में मदद करेगा, लेकिन फॉर्म नहीं भरेगा, विकल्प नहीं चुनेगा और काम पूरा नहीं मानेगा। पूरा पता न दिखने पर स्रोत का मिलान संभव नहीं है।",
                                    "Browser is website se judega. Saathi maujooda shart yaad rakhne mein madad karega, lekin form nahin bharega, option nahin chunega aur kaam poora nahin maanega. Poora pata na dikhne par source match mumkin nahin hai.")) },
                                confirmButton = { GlassButton(local("Open source", "स्रोत खोलें", "Source kholein"), onClick = {
                                    sourcePrompt = null
                                    val current = plan
                                    if (current == null || !SaathiSession.startReviewedSource(this@ResearchActivity, current, proposed.id, preferences.language)) {
                                        report = local("Review current sources and enable Saathi's accessibility and floating guidance permissions first.",
                                            "पहले मौजूदा स्रोत जाँचें और साथी की एक्सेसिबिलिटी तथा फ़्लोटिंग मार्गदर्शन अनुमतियाँ चालू करें।",
                                            "Pehle maujooda sources jaanchein aur Saathi ki accessibility aur floating guidance permissions chalu karein.")
                                    } else {
                                        openingSource = true
                                        try { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(proposed.sourceUrl))
                                            .addCategory(android.content.Intent.CATEGORY_BROWSABLE)) }
                                        catch (_: RuntimeException) {
                                            openingSource = false; SaathiSession.pause(); current.invalidateContext(); planRevision++
                                            report = local("No browser is available. Keep the source here for manual review.", "ब्राउज़र उपलब्ध नहीं है। स्रोत यहीं पढ़कर जाँचें।", "Browser uplabdh nahin hai. Source yahin padhkar jaanchein.")
                                        }
                                    }
                                }) },
                                dismissButton = { GlassButton(local("Not now", "अभी नहीं", "Abhi nahin"), primary = false, onClick = { sourcePrompt = null }) })
                        }
                        if (report.isNotBlank()) GlassPanel(Modifier.fillMaxWidth()) { Text(report, Modifier.padding(20.dp)) }
                        GlassButton(local("Explain common terms", "आम शब्दों का मतलब", "Aam shabdon ka matlab"), primary = false, onClick = { explainTerms = !explainTerms })
                        if (explainTerms) for (term in com.saathi.core.PlainLanguage.terms) {
                            Text(com.saathi.core.PlainLanguage.explain(term, preferences.language.apiTag).orEmpty())
                        }
                        GlassButton(local("Back", "वापस", "Wapas"), primary = false, onClick = { discardPlan(); finish() })
                    }
                }
            }
        }
    }
    private fun failure(result: GatewayResult): String {
        fun local(en: String, hi: String, hinglish: String) = when (Preferences(this).language) {
            GuidanceLanguage.HINDI -> hi; GuidanceLanguage.HINGLISH -> hinglish; else -> en
        }
        return when ((result as? GatewayResult.Rejected)?.reason) {
            "research_not_configured" -> local("Research sources are not configured on this server. No model call was made.", "सर्वर पर स्रोत तय नहीं हैं। किसी AI सेवा को नहीं बुलाया गया।", "Server par sources tay nahin hain. Kisi AI service ko nahin bulaya gaya.")
            "not_configured" -> local("Connect your configured backend first. No plan is available.", "पहले अपना बैकएंड जोड़ें। अभी कोई योजना उपलब्ध नहीं है।", "Pehle apna backend jodein. Abhi koi plan uplabdh nahin hai.")
            "rate_limited", "busy" -> local("A request is running or was made recently. Wait before trying again.", "अनुरोध जारी है या हाल में किया गया है। दोबारा कोशिश से पहले प्रतीक्षा करें।", "Request chal rahi hai ya haal mein ki gayi hai. Dobara koshish se pehle intezaar karein.")
            "source_unverified", "evidence_missing", "disagreement" -> local("Evidence was insufficient or the proposed plans disagreed. No plan was accepted.", "प्रमाण अधूरे थे या योजनाएं अलग थीं। कोई योजना स्वीकार नहीं की गई।", "Pramaan adhoore the ya plans alag the. Koi plan sweekar nahin kiya gaya.")
            else -> local("Research could not be verified. This does not establish eligibility or completion.", "खोज सत्यापित नहीं हो पाई। इससे पात्रता या काम पूरा होना तय नहीं होता।", "Khoj verify nahin ho payi. Isse patrata ya kaam poora hona tay nahin hota.")
        }
    }
    private fun discardPlan() { SaathiSession.discardReviewedPlan(plan); plan = null }
    override fun onDestroy() {
        if (isFinishing) discardPlan()
        super.onDestroy()
    }
    private fun cancelRequest() { pending?.cancel(); pending = null; busy = false; researchId = null }
    override fun onStart() {
        super.onStart()
        // Returning from a browser requires review again. No screen observation confirms a step.
        if (plan != null && SaathiSession.reviewedPlan() === plan) {
            if (SaathiSession.isActive()) SaathiSession.pause()
            plan?.invalidateContext(); planRevision++
        }
    }
    override fun onStop() {
        cancelRequest()
        if (!openingSource) plan?.invalidateContext()
        openingSource = false; planRevision++; super.onStop()
    }
}
