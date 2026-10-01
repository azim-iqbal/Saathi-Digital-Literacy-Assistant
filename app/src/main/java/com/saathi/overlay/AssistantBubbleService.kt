package com.saathi.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.ImageView
import com.saathi.AssistantActivity
import com.saathi.R
import com.saathi.orchestrator.SaathiSession

/** Small interactive window; all other screen coordinates remain available to the underlying app. */
class AssistantBubbleService : Service() {
    private var bubble: ImageView? = null
    private lateinit var manager: WindowManager
    private lateinit var position: WindowManager.LayoutParams
    override fun onCreate() { super.onCreate(); instance = this; manager = getSystemService(WindowManager::class.java) }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!SaathiSession.isActive() || !Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        if (bubble != null) return START_NOT_STICKY
        val size = (60 * resources.displayMetrics.density).toInt()
        position = WindowManager.LayoutParams(size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = resources.displayMetrics.widthPixels - size
            y = resources.displayMetrics.heightPixels / 3
        }
        val view = ImageView(this).apply {
            setImageResource(R.drawable.ic_saathi_mark)
            setColorFilter(android.graphics.Color.rgb(8, 121, 0))
            val padding = size / 4
            setPadding(padding, padding, padding, padding)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(android.graphics.Color.rgb(235, 251, 234))
                setStroke((2 * resources.displayMetrics.density).toInt(), android.graphics.Color.WHITE)
            }
            elevation = 6 * resources.displayMetrics.density
            contentDescription = "Saathi assistant. Tap to ask for help; drag to move."
            isFocusable = true
            setOnClickListener {
                runCatching { startActivity(Intent(this@AssistantBubbleService, AssistantActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)) }
            }
        }
        var downX = 0f; var downY = 0f; var initialX = 0; var initialY = 0; var dragged = false
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        view.setOnTouchListener { _, event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; initialX = position.x; initialY = position.y; dragged = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX; val dy = event.rawY - downY
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > slop) dragged = true
                    if (dragged) {
                        position.x = (initialX + dx.toInt()).coerceIn(0, (resources.displayMetrics.widthPixels - size).coerceAtLeast(0))
                        position.y = (initialY + dy.toInt()).coerceIn(0, (resources.displayMetrics.heightPixels - size * 2).coerceAtLeast(0))
                        runCatching { manager.updateViewLayout(view, position) }
                    }; true
                }
                MotionEvent.ACTION_UP -> { if (!dragged) view.performClick(); true }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
        bubble = view
        view.visibility = if (panelVisible) View.GONE else View.VISIBLE
        runCatching { manager.addView(view, position) }.onFailure { bubble = null; stopSelf() }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        bubble?.let { if (it.isAttachedToWindow) runCatching { manager.removeView(it) } }
        bubble = null
        if (instance === this) instance = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object {
        private var instance: AssistantBubbleService? = null
        fun ownsAccessibilityWindow(id: Int): Boolean {
            val view = instance?.bubble ?: return false
            if (id < 0 || !view.isAttachedToWindow) return false
            val node = view.createAccessibilityNodeInfo()
            return try { node.windowId == id } finally { @Suppress("DEPRECATION") node.recycle() }
        }
        private var panelVisible = false
        fun panelVisible(visible: Boolean) {
            panelVisible = visible
            instance?.bubble?.visibility = if (visible) View.GONE else View.VISIBLE
        }
    }
}
