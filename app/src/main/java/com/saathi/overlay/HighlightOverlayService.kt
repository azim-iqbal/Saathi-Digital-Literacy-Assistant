package com.saathi.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.IBinder
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.animation.ValueAnimator

class HighlightOverlayService : Service() {
    private var overlay: GuidanceOverlay? = null
    private var windowManager: WindowManager? = null
    override fun onCreate() { super.onCreate(); instance = this }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val currentKey = com.saathi.orchestrator.SaathiSession.presentationKey()
        if (currentKey == null || intent?.getStringExtra("presentation_key") != currentKey) {
            // An obsolete intent must not remove a newer marker already attached to this service.
            if (currentKey == null) { clearPresentation(); stopSelf(startId) }
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            com.saathi.orchestrator.SaathiSession.stopForPresentation(currentKey)
            stopSelf()
            return START_NOT_STICKY
        }
        val target = intent?.rectExtra(EXTRA_TARGET)
        val sensitive = intent?.rectListExtra(EXTRA_SENSITIVE).orEmpty()
        val complete = intent?.getBooleanExtra(EXTRA_COMPLETE, false) ?: false
        if (overlay == null) {
            overlay = GuidanceOverlay(this)
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                title = "Saathi guidance"
                // Android 12+ blocks touches through an opaque application-overlay window.
                alpha = if (Build.VERSION.SDK_INT >= 31) minOf(.7f,
                    getSystemService(android.hardware.input.InputManager::class.java).maximumObscuringOpacityForTouch) else .7f
                if (Build.VERSION.SDK_INT >= 30) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                else if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            try {
                windowManager?.addView(overlay, params)
            } catch (_: SecurityException) {
                com.saathi.orchestrator.SaathiSession.stopForPresentation(currentKey)
                stopSelf()
                return START_NOT_STICKY
            } catch (_: WindowManager.BadTokenException) {
                com.saathi.orchestrator.SaathiSession.stopForPresentation(currentKey)
                stopSelf()
                return START_NOT_STICKY
            }
            overlay?.post { overlay?.calibrate() }
        }
        overlay?.setState(target, sensitive, complete, intent?.getStringExtra(EXTRA_STATUS))
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        overlay?.let { view ->
            view.dispose()
            if (view.isAttachedToWindow) runCatching { windowManager?.removeView(view) }
        }
        overlay = null
        if (instance === this) instance = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private var instance: HighlightOverlayService? = null
        fun ownsAccessibilityWindow(id: Int): Boolean {
            val view = instance?.overlay ?: return false
            if (id < 0 || !view.isAttachedToWindow) return false
            val node = view.createAccessibilityNodeInfo()
            return try { node.windowId == id } finally { @Suppress("DEPRECATION") node.recycle() }
        }
        /** Clear pixels synchronously without creating window-add/remove accessibility feedback. */
        fun clearPresentation() { instance?.overlay?.setState(null, emptyList(), false, null) }
        private const val EXTRA_TARGET = "target"; private const val EXTRA_SENSITIVE = "sensitive"; private const val EXTRA_COMPLETE = "complete"; private const val EXTRA_STATUS = "status"
        fun intent(context: Context, target: Rect?, sensitive: List<Rect>, complete: Boolean, status: String? = null, presentationKey: String? = null) = Intent(context, HighlightOverlayService::class.java).apply {
            putExtra("presentation_key", presentationKey)
            putExtra(EXTRA_TARGET, target); putParcelableArrayListExtra(EXTRA_SENSITIVE, ArrayList(sensitive)); putExtra(EXTRA_COMPLETE, complete)
            status?.let { putExtra(EXTRA_STATUS, it) }
        }
    }
}

private fun Intent.rectExtra(key: String): Rect? = if (Build.VERSION.SDK_INT >= 33) {
    getParcelableExtra(key, Rect::class.java)
} else {
    @Suppress("DEPRECATION") getParcelableExtra(key)
}

private fun Intent.rectListExtra(key: String): ArrayList<Rect>? = if (Build.VERSION.SDK_INT >= 33) {
    getParcelableArrayListExtra(key, Rect::class.java)
} else {
    @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
}

private class GuidanceOverlay(context: Context) : android.view.View(context) {
    private val density = resources.displayMetrics.density
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(8, 121, 0); style = Paint.Style.STROKE; strokeWidth = 2.5f * density }
    private val contrast = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 4.5f * density }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(28, 20, 108, 90); style = Paint.Style.FILL }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 24f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
    private val labelBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(169, 33, 48) }
    private var target: Rect? = null; private var previousTarget: Rect? = null; private var sensitive = emptyList<Rect>(); private var complete = false; private var status: String? = null; private var pulse = 1f
    private var calibrationX = 0; private var calibrationY = 0
    private val animator = ValueAnimator.ofFloat(1f, 1.08f, 1f).apply {
        duration = 900
        addUpdateListener { pulse = it.animatedValue as Float; invalidate() }
    }
    fun setState(newTarget: Rect?, newSensitive: List<Rect>, isComplete: Boolean, newStatus: String?) {
        val changed = newTarget != target
        if (newTarget != target) previousTarget = target
        target = newTarget; sensitive = newSensitive; complete = isComplete; status = newStatus; invalidate()
        if (changed) {
            animator.cancel(); pulse = 1f
            if (newTarget != null && isAttachedToWindow && ValueAnimator.areAnimatorsEnabled() &&
                !com.saathi.ui.Preferences(context).reducedMotion) animator.start()
        }
    }
    fun calibrate() {
        val location = IntArray(2)
        getLocationOnScreen(location)
        calibrationX = -location[0]
        calibrationY = -location[1]
        setOnApplyWindowInsetsListener { _, insets -> insets }
        invalidate()
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { super.onSizeChanged(w, h, oldw, oldh); post { calibrate() } }
    override fun onAttachedToWindow() { super.onAttachedToWindow() }
    fun dispose() { animator.cancel() }
    override fun onDetachedFromWindow() { dispose(); super.onDetachedFromWindow() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        target?.let { rect ->
            drawTargetAnnotation(canvas, rect)
        }
        status?.let { text ->
            val padding = 16f * density
            val textPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; textSize = 16 * resources.displayMetrics.scaledDensity
            }
            val layout = android.text.StaticLayout.Builder.obtain(text, 0, text.length, textPaint,
                (width - padding * 4).toInt().coerceAtLeast(1)).setMaxLines(5)
                .setEllipsize(android.text.TextUtils.TruncateAt.END).build()
            val top = if ((target?.centerY() ?: 0) > height / 2) 72 * density
                else (height - layout.height - padding * 2 - 100 * density).coerceAtLeast(padding)
            val panel = RectF(padding, top, width - padding, top + layout.height + padding * 2)
            canvas.drawRoundRect(panel, 24 * density, 24 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(13, 70, 42) })
            canvas.save(); canvas.translate(panel.left + padding, panel.top + padding); layout.draw(canvas); canvas.restore()
        }
        sensitive.forEach { rect ->
            val badge = Rect(rect.left, (rect.top - 38).coerceAtLeast(0), (rect.left + 310).coerceAtMost(width), rect.top)
            canvas.drawRect(badge, labelBg); canvas.drawText("PRIVATE FIELD", badge.left + 10f, badge.bottom - 10f, label)
        }
        if (complete) { canvas.drawText("DONE", width / 2f - 45f, 96f, ring.apply { style = Paint.Style.FILL; textSize = 36f }); ring.style = Paint.Style.STROKE }
    }

    private fun drawTargetAnnotation(canvas: Canvas, target: Rect) {
        val inset = 8f * pulse
        val outline = RectF(target.left + calibrationX - inset, target.top + calibrationY - inset, target.right + calibrationX + inset, target.bottom + calibrationY + inset)
        canvas.drawRoundRect(outline, 24f, 24f, fill)
        canvas.drawRoundRect(outline, 24f, 24f, contrast)
        canvas.drawRoundRect(outline, 24f, 24f, ring)

        label.textSize = 12f * resources.displayMetrics.scaledDensity
        val tagWidth = label.measureText("NEXT STEP") + 24 * density
        val tagHeight = 28 * density
        val tagTop = (outline.top - tagHeight - 6 * density).coerceAtLeast(8 * density)
        val tagLeft = outline.left.coerceIn(8 * density, (width - tagWidth - 8 * density).coerceAtLeast(8 * density))
        val tag = RectF(tagLeft, tagTop, tagLeft + tagWidth, tagTop + tagHeight)
        canvas.drawRoundRect(tag, tagHeight / 2, tagHeight / 2, ring.apply { style = Paint.Style.FILL })
        canvas.drawText("NEXT STEP", tag.left + 12 * density, tag.centerY() - (label.fontMetrics.ascent + label.fontMetrics.descent) / 2, label)
        // A small pointer makes the click zone easier to locate than a ring alone.
        val pointerX = outline.right - 12f
        val pointerY = outline.bottom + 14f
        val pointer = android.graphics.Path().apply {
            moveTo(pointerX, pointerY); lineTo(pointerX + 20f, pointerY + 8f); lineTo(pointerX + 8f, pointerY + 15f); lineTo(pointerX + 16f, pointerY + 30f); lineTo(pointerX + 9f, pointerY + 34f); lineTo(pointerX + 1f, pointerY + 18f); lineTo(pointerX - 6f, pointerY + 27f); close()
        }
        canvas.drawPath(pointer, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL; setShadowLayer(4f, 1f, 2f, Color.DKGRAY) })
        ring.style = Paint.Style.STROKE
    }
}
