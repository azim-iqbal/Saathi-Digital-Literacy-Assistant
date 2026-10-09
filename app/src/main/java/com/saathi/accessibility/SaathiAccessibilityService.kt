package com.saathi.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.saathi.orchestrator.SaathiSession
import java.util.concurrent.Executors

class SaathiAccessibilityService : AccessibilityService() {
    private val nodeExecutor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var pending = false
    private var copying = false
    private var dirty = false
    @Volatile private var destroyed = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    /** A changed request needs a fresh tree even when the other app has not emitted an event. */
    private fun refreshScreen() {
        SaathiSession.invalidateScreen()
        dirty = true
        scheduleCopy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!SaathiSession.isActive()) return
        if (getSystemService(KeyguardManager::class.java).isKeyguardLocked) { SaathiSession.stop(); return }
        if (event.eventType !in setOf(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_WINDOWS_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, AccessibilityEvent.TYPE_VIEW_SCROLLED,
                AccessibilityEvent.TYPE_VIEW_CLICKED, AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            )) return
        // Our non-focusable overlay can announce a delayed window-state event. It has not
        // changed the underlying screen; re-observing would interrupt speech/model work.
        // Match actual attached window IDs, never ignore all events from the Saathi package:
        // opening a Saathi activity must still invalidate external guidance.
        // TYPE_WINDOWS_CHANGED can have a null/system package even for our window.
        // Ownership of the currently attached window is the boundary, not the event package.
        val ownOverlay = com.saathi.overlay.HighlightOverlayService.ownsAccessibilityWindow(event.windowId) ||
                com.saathi.overlay.AssistantBubbleService.ownsAccessibilityWindow(event.windowId) ||
                com.saathi.overlay.CyberLinkOverlayService.ownsAccessibilityWindow(event.windowId)
        ObservationDiagnostics.event(event.eventType, event.windowId, event.contentChangeTypes, ownOverlay)
        if (ownOverlay) return
        // Old WebView/background windows can emit delayed content events. Compare
        // against the actual current root, never an earlier presentation ticket.
        // Window transitions and missing roots still take the invalidation path.
        val root = runCatching { rootInActiveWindow }.getOrNull()
        val rootWindow = try { root?.windowId } finally { @Suppress("DEPRECATION") root?.recycle() }
        if (!com.saathi.core.ObservationEventPolicy.relevant(event.eventType, event.windowId, rootWindow)) return
        refreshScreen()
    }

    private fun inputMethodSurface(root: AccessibilityNodeInfo): Boolean {
        val window = runCatching { root.window }.getOrNull()
        val type = try { window?.type } finally { @Suppress("DEPRECATION") window?.recycle() }
        val keyboards = runCatching {
            getSystemService(android.view.inputmethod.InputMethodManager::class.java).inputMethodList.map { it.packageName }.toSet()
        }.getOrDefault(emptySet())
        val configured = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.let(android.content.ComponentName::unflattenFromString)?.packageName
        return !com.saathi.core.WindowObservationPolicy.allows(type, root.packageName?.toString().orEmpty(), keyboards + listOfNotNull(configured))
    }

    private fun scheduleCopy() {
        if (destroyed || pending || copying || !SaathiSession.isActive()) return
        pending = true
        // Fixed delay from the first event: continuous events cannot starve snapshots.
        handler.postDelayed({
            pending = false
            if (destroyed || !SaathiSession.isActive()) return@postDelayed
            if (getSystemService(KeyguardManager::class.java).isKeyguardLocked) { SaathiSession.stop(); return@postDelayed }
            dirty = false
            // A WebView event may leave old descendants in the service cache even
            // though the renderer has changed. Fetch this observation from the
            // accessibility connection rather than reusing those cached children.
            if (android.os.Build.VERSION.SDK_INT >= 33) clearCache()
            val root = runCatching { rootInActiveWindow }.getOrNull() ?: run {
                SaathiSession.onScreenUnavailable()
                return@postDelayed
            }
            if (inputMethodSurface(root) || !SaathiSession.canObserve(root.packageName?.toString().orEmpty())) {
                @Suppress("DEPRECATION") root.recycle()
                SaathiSession.onScreenUnavailable()
                return@postDelayed
            }
            val ticket = SaathiSession.beginObservation(root.packageName?.toString().orEmpty(), root.windowId)
            val observedAtMs = System.currentTimeMillis()
            val started = android.os.SystemClock.elapsedRealtime()
            val snapshot = AccessibilityNodeInfo.obtain(root)
            @Suppress("DEPRECATION") root.recycle()
            if (ticket == null) { @Suppress("DEPRECATION") snapshot.recycle(); return@postDelayed }
            copying = true
            nodeExecutor.execute {
                try {
                    val nodes = NodeMasker.flatten(snapshot) {
                        !destroyed && android.os.SystemClock.elapsedRealtime() - started in 0..2_000
                    }
                    ObservationDiagnostics.snapshot(android.os.SystemClock.elapsedRealtime() - started)
                    val browserLocation = if (SaathiSession.wantsBrowserLocation() &&
                        com.saathi.core.ScreenInterruption.reason(nodes) == null) BrowserLocationReader.read(snapshot) else null
                    if (!destroyed) SaathiSession.onScreenChanged(nodes, ticket, browserLocation, observedAtMs)
                }
                catch (error: RuntimeException) { ObservationDiagnostics.observationFailure(error.message.orEmpty()); SaathiSession.onObservationFailed(ticket) }
                finally {
                    @Suppress("DEPRECATION") snapshot.recycle()
                    if (!destroyed) handler.post { copying = false; if (dirty) scheduleCopy() }
                }
            }
        }, 150)
    }

    override fun onDestroy() {
        destroyed = true
        if (instance === this) { instance = null; SaathiSession.stop() }
        handler.removeCallbacksAndMessages(null)
        // Let an already accepted worker run its finally/recycle even if it was queued.
        // The destroyed predicate aborts traversal before reading further node contents.
        nodeExecutor.shutdown()
        super.onDestroy()
    }
    override fun onInterrupt() { SaathiSession.stop() }

    companion object {
        private var instance: SaathiAccessibilityService? = null
        fun isConnected() = instance != null
        /** Main-thread only, like session changes and accessibility event delivery. */
        fun requestCurrentScreen() { instance?.refreshScreen() }
    }
}
