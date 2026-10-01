package com.saathi.orchestrator

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.Toast
import com.saathi.capture.ScreenshotCapture
import com.saathi.gateway.PracticeGateway
import com.saathi.core.GatewayCancellation
import com.saathi.core.GatewayResult
import com.saathi.core.SanitizedScreenSnapshot
import com.saathi.core.SanitizedTaskCategory
import com.saathi.core.ProposedAction
import com.saathi.core.DualProposalValidator
import com.saathi.core.GuideTarget
import com.saathi.core.GuideStep
import com.saathi.core.GuideAction
import com.saathi.core.StepHistory
import com.saathi.core.UiNode
import com.saathi.core.GuidanceSessionState
import com.saathi.guardrails.GuardrailAuditLog
import com.saathi.guardrails.GuardrailDecision
import com.saathi.guardrails.GuardrailEngine
import com.saathi.guardrails.GuardrailResult
import com.saathi.language.GuidanceCopy
import com.saathi.language.GuidanceLanguage
import com.saathi.overlay.HighlightOverlayService
import com.saathi.speech.TtsManager
import com.saathi.speech.VoiceConversationService
import com.saathi.core.ObservationGate
import com.saathi.core.PracticeSurfacePolicy
import com.saathi.storage.GuidanceStateStore

object SaathiSession {
    private val mutableStatus = kotlinx.coroutines.flow.MutableStateFlow(GuidanceSessionState.STOPPED)
    val status: kotlinx.coroutines.flow.StateFlow<GuidanceSessionState> = mutableStatus
    private val mutableInstruction = kotlinx.coroutines.flow.MutableStateFlow("")
    val instruction: kotlinx.coroutines.flow.StateFlow<String> = mutableInstruction
    private val mutableReply = kotlinx.coroutines.flow.MutableStateFlow("")
    val lastReply: kotlinx.coroutines.flow.StateFlow<String> = mutableReply
    fun currentRequest() = if (active) goal else ""
    private var live = false
    private var liveAi = false
    private val previousAiTargets = java.util.ArrayDeque<String>()
    fun acceptsLiveRequest(request: String) = if (liveAi || PracticeGateway.aiEnabled()) com.saathi.core.LiveAiPolicy.allowed(request) else LiveGuide.label(request) != null
    fun isLive() = live
    fun hasSpokenGuidance() = active && (spokenPromptsEnabled || VoiceConversationService.isRunning())
    fun setSpokenGuidance(enabled: Boolean) {
        spokenPromptsEnabled = enabled
        if (!enabled) {
            tts?.release(); tts = null
            context?.let(VoiceConversationService::stop)
        }
    }
    fun canObserve(packageName: String) = if (live) LiveGuide.allowedPackage(packageName, context?.packageName.orEmpty()) else packageName == context?.packageName
    fun startLive(app: Context, request: String, language: GuidanceLanguage, spoken: Boolean): Boolean {
        if (!acceptsLiveRequest(request)) return false
        start(app, request, language, spoken, com.saathi.ui.Preferences(app).speechRate, liveMode = true)
        return active
    }

    /** Retarget an existing live session; old screen/audio work must not answer the new request. */
    fun changeLiveRequest(request: String, expectedSession: String?, expectedPresentation: String? = null): Boolean {
        if (!active || !live || !observationGate.matchesSession(expectedSession) ||
            !acceptsLiveRequest(request) || !com.saathi.accessibility.SaathiAccessibilityService.isConnected()) return false
        if (expectedPresentation != null && (!observationGate.matchesPresentation(expectedPresentation) ||
                latestNodes.any { it.isSensitive || it.isPassword })) return false
        goal = request
        previousAiTargets.clear()
        mutableReply.value = ""
        mutableStatus.value = GuidanceSessionState.OBSERVING
        // Invalidate first and copy the real current screen; never reuse retained bounds.
        com.saathi.accessibility.SaathiAccessibilityService.requestCurrentScreen()
        return true
    }
    fun pause() { clearSession(GuidanceSessionState.PAUSED) }
    private val handler = Handler(Looper.getMainLooper())
    private val observationGate = ObservationGate()
    private var context: Context? = null
    private var tts: TtsManager? = null
    private var goal = ""
    private var language = GuidanceLanguage.ENGLISH
    private var speechRate = TtsManager.DEFAULT_SPEECH_RATE
    private var spokenPromptsEnabled = false
    private var lastSpokenTargetId: String? = null
    private var active = false
    private var latestNodes: List<UiNode> = emptyList()
    private var lastStep: GuideStep? = null
    private var history = mutableListOf<StepHistory>()
    private var pendingRunnable: Runnable? = null
    private var currentPackage: String? = null
    private var pendingGateway: GatewayCancellation? = null
    private var useGateway = false

    fun start(
        appContext: Context,
        goalText: String,
        languageMode: GuidanceLanguage,
        speakPrompts: Boolean = false,
        requestedSpeechRate: Float = TtsManager.DEFAULT_SPEECH_RATE,
        liveMode: Boolean = false
    ): GuardrailResult {
        stop()
        context = appContext.applicationContext
        live = liveMode
        liveAi = liveMode && PracticeGateway.aiEnabled()
        useGateway = !liveMode && PracticeGateway.enabled()
        goal = goalText
        language = languageMode
        speechRate = requestedSpeechRate.coerceIn(TtsManager.MIN_SPEECH_RATE, TtsManager.MAX_SPEECH_RATE)
        mutableStatus.value = GuidanceSessionState.PREPARING

        val scope = (if (liveMode && acceptsLiveRequest(goalText))
            GuardrailResult(GuardrailDecision.ALLOW, "visible_option", "explicit_local_option")
            else GuardrailEngine.classify(goalText)).also(GuardrailAuditLog::record)
        if (scope.decision == GuardrailDecision.REFUSE) {
            clearSession(GuidanceSessionState.ERROR)
            return scope
        }

        active = true
        observationGate.start()
        mutableStatus.value = GuidanceSessionState.OBSERVING
        spokenPromptsEnabled = speakPrompts
        lastSpokenTargetId = null
        history.clear()
        lastStep = null
        currentPackage = null
        if (spokenPromptsEnabled) ensureTts()
        runCatching { GuidanceForegroundService.start(context!!) }
            .onFailure { clearSession(GuidanceSessionState.ERROR) }
        if (active) runCatching { context!!.startService(Intent(context, com.saathi.overlay.AssistantBubbleService::class.java)) }
            .onFailure { clearSession(GuidanceSessionState.ERROR) }

        // Conversation is enabled separately by a visible activity after microphone consent.
        return scope
    }

    fun stop() = clearSession(GuidanceSessionState.STOPPED)

    private fun clearSession(terminalState: GuidanceSessionState) {
        val app = context
        pendingGateway?.cancel(); pendingGateway = null
        useGateway = false
        active = false
        live = false
        liveAi = false
        previousAiTargets.clear()
        mutableInstruction.value = ""
        mutableReply.value = ""
        goal = ""
        observationGate.stop()
        latestNodes = emptyList()
        lastStep = null
        history.clear()
        currentPackage = null
        lastSpokenTargetId = null
        spokenPromptsEnabled = false
        ScreenshotCapture.stop()
        tts?.release()
        tts = null
        app?.let { GuidanceStateStore(it).clear() }
        handler.removeCallbacksAndMessages(null)
        pendingRunnable = null
        app?.stopService(Intent(app, HighlightOverlayService::class.java))
        app?.stopService(Intent(app, com.saathi.overlay.AssistantBubbleService::class.java))
        app?.let(VoiceConversationService::stop)
        app?.let(GuidanceForegroundService::stop)
        context = null
        mutableStatus.value = terminalState
    }

    fun isActive() = active
    fun sessionKey(): String? = observationGate.sessionKey()
    fun stopForSession(key: String?) { if (observationGate.matchesSession(key)) stop() }
    fun presentationKey(): String? = observationGate.currentKey()

    /** A failed old overlay must not terminate a newer observation or restarted session. */
    fun stopForPresentation(key: String?) {
        if (observationGate.matchesPresentation(key)) stop()
    }

    /** Called on the main thread at the event boundary, before copying a replacement tree. */
    fun invalidateScreen() {
        pendingGateway?.cancel(); pendingGateway = null
        observationGate.invalidate()
        pendingRunnable?.let(handler::removeCallbacks)
        latestNodes = emptyList()
        lastStep = null
        lastSpokenTargetId = null
        mutableInstruction.value = ""
        HighlightOverlayService.clearPresentation()
        VoiceConversationService.suspendForScreenChange()
        tts?.stop()
    }

    fun beginObservation(packageName: String, windowId: Int): ObservationGate.Ticket? =
        observationGate.observe(packageName, windowId)

    fun onObservationFailed(ticket: ObservationGate.Ticket) {
        handler.post { if (observationGate.accepts(ticket)) onScreenUnavailable() }
    }

    /** A secure, empty, or transitioning window is never treated as a continuation of guidance. */
    fun onScreenUnavailable() {
            if (!active) return
            pendingGateway?.cancel(); pendingGateway = null
            observationGate.invalidate()
            pendingRunnable?.let(handler::removeCallbacks)
            pendingRunnable = null
            waitForPractice()
    }

    /** Called only from the explicit voice action in the visible session screen. */
    fun startConversation(app: Context) {
        if (!active) return
        tts?.release()
        tts = null
        VoiceConversationService.start(app, language, speechRate)
    }

    fun refreshVoice() {
        val step = lastStep ?: return
        if (active) VoiceConversationService.update(step.speechText, latestNodes.none { it.isSensitive || it.isPassword })
    }

    fun onScreenChanged(nodes: List<UiNode>, ticket: ObservationGate.Ticket) {
        val observedAtMs = System.currentTimeMillis()
        // Accessibility copying runs off-thread. All session state and presentation stay on main.
        handler.post {
            if (!observationGate.accepts(ticket)) return@post
            latestNodes = nodes
            currentPackage = ticket.packageName
            pendingRunnable?.let(handler::removeCallbacks)
            pendingRunnable = Runnable {
                pendingRunnable = null
                if (!observationGate.accepts(ticket)) return@Runnable
                val app = context ?: return@Runnable
                if (if (live) !LiveGuide.allowedPackage(ticket.packageName, app.packageName)
                    else !PracticeSurfacePolicy.isEligible(ticket.packageName, app.packageName, nodes)) {
                    waitForPractice()
                    return@Runnable
                }
                if (liveAi && nodes.none { it.isSensitive || it.isPassword }) {
                    requestLiveAi(nodes, ticket, observedAtMs)
                    return@Runnable
                }
                if (useGateway && nodes.none { it.isSensitive || it.isPassword }) {
                    requestGateway(nodes, ticket, observedAtMs)
                    return@Runnable
                }
                val step = if (live) LiveGuide.next(goal, nodes, language.apiTag) else DemoGuide.next(goal, nodes, language.apiTag, false)
                // Only a current, enabled local node can determine overlay coordinates.
                val resolvedNode = step.target?.let { proposed ->
                    if (proposed.nodeIndex != null) nodes.getOrNull(proposed.nodeIndex)
                    else nodes.singleOrNull { it.resourceId == proposed.resourceId }
                }
                val target = resolvedNode?.takeIf { it.isEnabled && !it.isSensitive }
                    ?.let { step.target?.copy(bounds = android.graphics.Rect(if (live && !it.isClickable) it.clickableAncestorBounds ?: it.bounds else it.bounds)) }
                if (observationGate.accepts(ticket)) present(
                    step.copy(target = target),
                    sensitiveTarget = resolvedNode?.isSensitive == true
                )
            }.also { handler.post(it) }
        }
    }

    private fun requestGateway(nodes: List<UiNode>, ticket: ObservationGate.Ticket, observedAtMs: Long) {
        pendingGateway?.cancel()
        val task = when {
            goal.contains("water", true) || goal.contains("पानी") -> SanitizedTaskCategory.WATER_BILL
            goal.contains("dth", true) || goal.contains("tv", true) || goal.contains("recharge", true) -> SanitizedTaskCategory.DTH_RECHARGE
            else -> SanitizedTaskCategory.ELECTRICITY_BILL
        }
        val snapshot = SanitizedScreenSnapshot.from(ticket, language.apiTag, nodes, task, observedAtMs)
        mutableStatus.value = GuidanceSessionState.ANALYSING
        mutableInstruction.value = when (language) {
            GuidanceLanguage.ENGLISH -> "Checking this practice step with the local test server…"
            GuidanceLanguage.HINDI -> "स्थानीय टेस्ट सर्वर से अभ्यास का कदम जाँच रहे हैं…"
            GuidanceLanguage.HINGLISH -> "Local test server se practice step check ho raha hai…"
        }
        pendingGateway = PracticeGateway.request(snapshot) { result ->
            if (!observationGate.accepts(ticket)) return@request
            pendingGateway = null
            val proposal = (result as? GatewayResult.Accepted)?.proposal
            if (proposal == null || DualProposalValidator.validate(snapshot, proposal) != null) {
                val text = when (language) {
                    GuidanceLanguage.ENGLISH -> "The local test server could not verify this step. No target is shown. Check the connection, or disable Local backend test and restart practice."
                    GuidanceLanguage.HINDI -> "स्थानीय टेस्ट सर्वर इस कदम की जाँच नहीं कर सका। कोई निशान नहीं दिखाया गया। कनेक्शन जाँचें या Local backend test बंद करके अभ्यास फिर शुरू करें।"
                    GuidanceLanguage.HINGLISH -> "Local test server step verify nahi kar saka. Koi target nahi dikhaya. Connection check karein ya Local backend test band karke practice dobara shuru karein."
                }
                present(GuideStep(text, language.apiTag, null, "Explicit recovery required.", false), false)
                return@request
            }
            val node = proposal.targetResourceId?.let { id -> nodes.singleOrNull { it.resourceId == id && it.isEnabled && it.isClickable && !it.isSensitive && !it.isPassword } }
            if (proposal.action == ProposedAction.HIGHLIGHT && node == null) {
                present(GuideStep("The practice control changed. Wait for a clear screen.", language.apiTag, null, "A fresh screen is required.", false), false)
                return@request
            }
            // Keep reviewed local translations; the paired decision supplies only grounded action/ID.
            val local = DemoGuide.next(goal, nodes, language.apiTag, false)
            val agrees = when (proposal.action) {
                ProposedAction.HIGHLIGHT -> local.target?.resourceId == proposal.targetResourceId && !local.goalComplete
                ProposedAction.COMPLETE -> local.goalComplete
                ProposedAction.HANDOVER -> false
            }
            if (!agrees) {
                present(GuideStep("The local test server needs a clearer practice screen. No target is shown.", language.apiTag, null, "Wait for a supported step.", false), false)
            } else present(local.copy(target = node?.let { GuideTarget(android.graphics.Rect(it.bounds), it.resourceId, "Practice control") }), false)
        }
    }

    private fun requestLiveAi(nodes: List<UiNode>, ticket: ObservationGate.Ticket, observedAtMs: Long) {
        pendingGateway?.cancel()
        val snapshot = com.saathi.core.LiveAiPolicy.snapshot(ticket, nodes, goal, language.apiTag, observedAtMs, previousAiTargets.toList())
        fun clarify() {
            val message = when (language) {
                GuidanceLanguage.ENGLISH -> "I could not agree on a clear next step. No marker is shown. Check your backend connection or describe a more specific task."
                GuidanceLanguage.HINDI -> "अगले कदम पर सहमति नहीं मिली। कोई निशान नहीं दिखाया गया। कनेक्शन जाँचें या काम को और स्पष्ट बताएँ।"
                GuidanceLanguage.HINGLISH -> "Clear next step par agreement nahi mila. Koi marker nahi dikhaya. Connection check karein ya task aur clearly batayein."
            }
            present(GuideStep(message, language.apiTag, null, "User clarification required.", false), false)
        }
        if (snapshot == null) { clarify(); return }
        mutableStatus.value = GuidanceSessionState.ANALYSING
        mutableInstruction.value = when (language) {
            GuidanceLanguage.ENGLISH -> "Checking the next step…"
            GuidanceLanguage.HINDI -> "अगला कदम जाँच रहे हैं…"
            GuidanceLanguage.HINGLISH -> "Agla step check ho raha hai…"
        }
        pendingGateway = PracticeGateway.requestLive(snapshot) { result ->
            if (!observationGate.accepts(ticket)) return@requestLive
            pendingGateway = null
            val proposal = (result as? GatewayResult.Accepted)?.proposal
            if (proposal == null || !snapshot.valid(proposal) || proposal.action != ProposedAction.HIGHLIGHT) { clarify(); return@requestLive }
            val control = snapshot.controls.singleOrNull { it.id == proposal.targetResourceId } ?: run { clarify(); return@requestLive }
            val node = nodes.getOrNull(control.nodeIndex) ?: run { clarify(); return@requestLive }
            if (!node.isEnabled || node.isSensitive || node.isPassword || node.isEditable) { clarify(); return@requestLive }
            val bounds = if (node.isClickable) node.bounds else node.clickableAncestorBounds ?: run { clarify(); return@requestLive }
            val text = when (language) {
                GuidanceLanguage.ENGLISH -> "For your task, choose “${control.label}”. You control every tap. If this is not what you meant, change your request in Saathi."
                GuidanceLanguage.HINDI -> "अपने काम के लिए “${control.label}” चुनें। हर टैप आप करते हैं। अगर यह आपका मतलब नहीं था, साथी में अनुरोध बदलें।"
                GuidanceLanguage.HINGLISH -> "Apne task ke liye “${control.label}” chuniye. Har tap aap karte hain. Agar yeh aapka matlab nahi tha, Saathi mein request badaliye."
            }
            // Record suggestions, not claimed clicks/completed actions. Backend treats these as untrusted context.
            if (previousAiTargets.peekLast() != control.label) { previousAiTargets.addLast(control.label); while (previousAiTargets.size > 3) previousAiTargets.removeFirst() }
            present(GuideStep(text, language.apiTag, GuideTarget(android.graphics.Rect(bounds), node.resourceId, control.label, control.nodeIndex),
                "Observe the next screen before proposing another step.", false), false)
        }
    }

    /** Stop any visual or spoken output before the user moves outside the synthetic fixture. */
    private fun waitForPractice() {
        if (!active) return
        latestNodes = emptyList()
        lastStep = null
        lastSpokenTargetId = null
        mutableInstruction.value = if (live) "Open the app or browser you want help with. Saathi will check its visible controls." else "Open the practice screen to continue."
        tts?.stop()
        context?.stopService(Intent(context, HighlightOverlayService::class.java))
        VoiceConversationService.suspendForScreenChange()
        mutableStatus.value = GuidanceSessionState.WAITING_FOR_PRACTICE
    }

    private fun present(step: GuideStep, sensitiveTarget: Boolean) {
        if (!active) return

        if (step.action == GuideAction.REFUSE) {
            clearSession(GuidanceSessionState.ERROR)
            return
        }

        lastStep?.let { history += StepHistory(it.speechText, it.expectedOutcome) }
        lastStep = step
        mutableInstruction.value = step.speechText
        mutableReply.value = step.speechText

        val app = context ?: return
        if (step.goalComplete) {
            clearSession(GuidanceSessionState.COMPLETED)
            return
        }
        if (VoiceConversationService.isRunning()) refreshVoice() else speakWhenUseful(step)

        mutableStatus.value = if (sensitiveTarget) {
            GuidanceSessionState.SENSITIVE_HANDOVER
        } else {
            GuidanceSessionState.GUIDING
        }
        runCatching {
            app.startService(
                HighlightOverlayService.intent(
                    app,
                    step.target?.bounds,
                    latestNodes.filter { it.isSensitive }.map { it.bounds },
                    complete = false,
                    status = step.speechText,
                    presentationKey = presentationKey()
                )
            )
        }.onFailure { clearSession(GuidanceSessionState.ERROR) }
    }

    /**
     * Visual guidance is the default. Speech is opt-in and only fires for a genuinely new
     * destination or a gentle correction, avoiding repetitive narration while a user types.
     */
    private fun speakWhenUseful(step: GuideStep) {
        if (!spokenPromptsEnabled) return

        val targetId = step.target?.resourceId ?: step.speechText
        val isCorrection = step.correctionNote != null
        if (isCorrection || targetId != lastSpokenTargetId) {
            val prompt = step.correctionNote ?: step.speechText
            val isSensitiveTarget = targetId.orEmpty().lowercase().let { it.contains("pin") || it.contains("password") || it.contains("otp") || it.contains("cvv") }
            val app = context ?: return
            ensureTts()
            if (isSensitiveTarget) {
                tts?.speak(GuidanceCopy.privateField(language), language)
            } else {
                tts?.speak(prompt, language)
            }
            lastSpokenTargetId = targetId
        }
    }

    private fun ensureTts() {
        if (tts != null) return

        val app = context ?: return
        tts = TtsManager(app, speechRate) {
            handler.post {
                Toast.makeText(app, GuidanceCopy.ttsSetup(language), Toast.LENGTH_LONG).show()
                runCatching {
                    app.startActivity(
                        Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }
    }

}
