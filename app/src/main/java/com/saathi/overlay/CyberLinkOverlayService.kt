package com.saathi.overlay

import android.app.Service
import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView
import androidx.compose.ui.graphics.toArgb
import com.saathi.cyber.CyberReportGuide
import com.saathi.cyber.ComplaintField
import com.saathi.cyber.ComplaintClipboard
import com.saathi.cyber.ComplaintHelperHandoff
import com.saathi.ui.Preferences
import com.saathi.ui.saathiColorScheme

/** Optional, short-lived link or reviewed-draft helper. Never reads screens or the clipboard, and never pastes. */
class CyberLinkOverlayService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var panel: View? = null
    private var expires = 0L
    private var draftFields: List<ComplaintField> = emptyList()
    private var selectedField: ComplaintField? = null
    private val manager by lazy { getSystemService(WindowManager::class.java) }
    override fun onCreate() { super.onCreate(); instance = this }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private val check = object : Runnable {
        override fun run() {
            if (!Settings.canDrawOverlays(this@CyberLinkOverlayService) ||
                getSystemService(KeyguardManager::class.java).isKeyguardLocked ||
                !getSystemService(PowerManager::class.java).isInteractive || SystemClock.elapsedRealtime() >= expires) stopSelf()
            else handler.postDelayed(this, 500)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || !Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        selectedField = null
        draftFields = if (intent.action == DRAFT_ACTION) ComplaintHelperHandoff.take() else emptyList()
        if (intent.action == DRAFT_ACTION && draftFields.isEmpty()) { stopSelf(); return START_NOT_STICKY }
        expires = SystemClock.elapsedRealtime() + 120_000
        show(false)
        handler.removeCallbacks(check); handler.post(check)
        return START_NOT_STICKY
    }
    private fun show(consent: Boolean, result: String? = null) {
        removePanel()
        val preferences = Preferences(this)
        val dark = preferences.theme == "Dark" || (preferences.theme == "System" && resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        val colors = saathiColorScheme(dark)
        fun background(fill: Int, radius: Int) = GradientDrawable().apply {
            setColor(fill); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), colors.outlineVariant.toArgb())
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = background(colors.surface.toArgb(), 28)
            elevation = dp(8).toFloat()
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        fun text(value: String, size: Float = 16f) { column.addView(TextView(this).apply {
            text = value; textSize = size; setTextColor(colors.onSurface.toArgb()); setPadding(0, 0, 0, dp(12))
        }) }
        fun button(value: String, primary: Boolean = false, click: () -> Unit) {
            column.addView(Button(this).apply {
                text = value; textSize = 15f; isAllCaps = false
                minWidth = 0; minimumWidth = 0; minHeight = dp(48); minimumHeight = dp(48)
                setPadding(dp(20), dp(10), dp(20), dp(10))
                setTextColor((if (primary) colors.onPrimary else colors.onSurface).toArgb())
                background = background((if (primary) colors.primary else colors.primaryContainer).toArgb(), 28)
                setOnClickListener { click() }
            }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) })
        }
        text("Saathi · Cyber reporting", 18f)
        when {
            result != null -> {
                text(result)
                if (draftFields.isNotEmpty()) button("Choose another field") { selectedField = null; show(false) }
            }
            consent && selectedField != null -> {
                val field = requireNotNull(selectedField)
                text("Allow copy: ${field.title}?")
                text(field.instruction)
                text(field.text)
                text("Only this full text replaces your clipboard. Saathi does not read it. Return to your existing browser tab, check cybercrime.gov.in, focus the matching field, long-press and Paste. Review before continuing. Nothing is submitted.")
                button("Allow copy", true) {
                    if (!canCopy()) { stopSelf(); return@button }
                    val copied = ComplaintClipboard.copy(this, field)
                    selectedField = null
                    show(false, if (copied) "${field.title} copied. Close this helper if it covers the form. Focus the matching field, long-press and Paste, then check the full text. Use recent apps to return to Saathi if needed."
                        else "Copy unavailable. Return to the Saathi worksheet through recent apps and enter the details manually.")
                }
            }
            !consent && draftFields.isNotEmpty() -> {
                text("Choose the field you want to copy. Check its preview before allowing a clipboard change.")
                draftFields.forEach { field -> button("Copy ${field.title.lowercase()}", true) { selectedField = field; show(true) } }
            }
            consent -> {
                text("Allow copying this link?")
                text("${CyberReportGuide.PORTAL}\n\nThis replaces your clipboard. Saathi does not read it. Paste into the browser address bar yourself, check the address, then press Go.")
                button("Allow copy", true) {
                    if (!canCopy()) { stopSelf(); return@button }
                    val copied = runCatching { getSystemService(ClipboardManager::class.java).setPrimaryClip(
                        ClipData.newPlainText("Official cybercrime portal", CyberReportGuide.PORTAL)) }.isSuccess
                    show(false, if (copied) "Link copied. Paste into the browser address bar and press Go." else "Copy unavailable. Open the official portal from Saathi instead.")
                }
            }
            else -> button("Copy reporting link", true) { show(true) }
        }
        button(if (consent) "Not now" else "Close helper") { stopSelf() }
        val width = minOf(resources.displayMetrics.widthPixels - dp(32), dp(340)).coerceAtLeast(dp(160))
        val scroll = ScrollView(this).apply { addView(column); background = background(colors.surface.toArgb(), 28); clipToOutline = true }
        column.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        val height = minOf(column.measuredHeight, resources.displayMetrics.heightPixels * 2 / 3)
        val params = WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = dp(28); title = "Saathi reporting link" }
        panel = scroll
        runCatching { manager.addView(scroll, params) }.onFailure { panel = null; stopSelf() }
    }
    private fun canCopy() = Settings.canDrawOverlays(this) &&
        !getSystemService(KeyguardManager::class.java).isKeyguardLocked &&
        getSystemService(PowerManager::class.java).isInteractive && SystemClock.elapsedRealtime() < expires
    private fun removePanel() { panel?.let { if (it.isAttachedToWindow) runCatching { manager.removeView(it) } }; panel = null }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); draftFields = emptyList(); selectedField = null; ComplaintHelperHandoff.clear(); removePanel(); if (instance === this) instance = null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object {
        const val DRAFT_ACTION = "com.saathi.REVIEWED_COMPLAINT_HELPER"
        private var instance: CyberLinkOverlayService? = null
        fun ownsAccessibilityWindow(id: Int): Boolean {
            val view = instance?.panel ?: return false
            if (id < 0 || !view.isAttachedToWindow) return false
            val node = view.createAccessibilityNodeInfo()
            return try { node.windowId == id } finally { @Suppress("DEPRECATION") node.recycle() }
        }
    }
}
