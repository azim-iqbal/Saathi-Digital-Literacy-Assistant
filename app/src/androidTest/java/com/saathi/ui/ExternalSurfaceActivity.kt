package com.saathi.ui

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Installed in the separate test APK/UID. No production entry, real data or remote page. */
class ExternalSurfaceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("close_fixture", false)) { finish(); return }
        if (intent.getBooleanExtra("resume_fixture", false)) { showResumeChoices(); return }
        if (intent.getBooleanExtra("travel_fixture", false)) {
            val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 100, 32, 32) }
            for (label in arrayOf("From", "To", "01/10/2026", "Cheapest from ₹5221", "₹6,398", "10:20 AM", "Shopping")) {
                layout.addView(Button(this).apply { text = label; isAllCaps = false })
            }
            if (intent.getBooleanExtra("private_fixture", false)) layout.addView(android.widget.EditText(this).apply {
                hint = "OTP"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                setText("582139")
            })
            setContentView(layout); return
        }
        showChoices()
    }
    private fun showChoices() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 100, 32, 32) }
        layout.addView(TextView(this).apply { text = "Saathi compatibility fixture"; textSize = 24f })
        layout.addView(Button(this).apply {
            isAllCaps = false
            text = "Help"
            setOnClickListener { text = "Help opened" }
        })
        layout.addView(Button(this).apply {
            isAllCaps = false
            text = "Event storm"
            setOnClickListener {
                // Actual external accessibility events, with unchanged safe fixture content.
                val handler = android.os.Handler(mainLooper)
                repeat(600) { index -> handler.postDelayed({
                    layout.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
                }, (index * 2).toLong()) }
            }
        })
        layout.addView(Button(this).apply {
            isAllCaps = false
            text = "Changing event storm"
            setOnClickListener {
                val handler = android.os.Handler(mainLooper)
                repeat(60) { index -> handler.postDelayed({
                    layout.contentDescription = if (index % 2 == 0) "Updated fixture north" else "Updated fixture south"
                    layout.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
                }, (index * 20).toLong()) }
            }
        })
        layout.addView(Button(this).apply {
            isAllCaps = false
            text = "Explore"
            setOnClickListener { showDetour() }
        })
        layout.addView(WebView(this).apply {
            settings.javaScriptEnabled = true
            loadDataWithBaseURL(null, "<html><meta name='viewport' content='width=device-width, initial-scale=1'><body><h2>Local web fixture</h2><a href='#opened' onclick=\"this.textContent='Support opened';return false;\">Support</a></body></html>", "text/html", "UTF-8", null)
        }, LinearLayout.LayoutParams(-1, 600))
        setContentView(layout)
    }
    private fun showDetour() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 100, 32, 32) }
        layout.addView(TextView(this).apply { text = "Another page"; textSize = 24f })
        layout.addView(Button(this).apply {
            isAllCaps = false
            text = "Back to choices"
            setOnClickListener { showChoices() }
        })
        setContentView(layout)
    }
    private fun showResumeChoices() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 100, 32, 32) }
        val labels = arrayOf("Help", "Private interruption", "Human challenge", "Message example")
        for (index in labels.indices) {
            val label = labels[index]
            layout.addView(Button(this).apply {
                text = label; isAllCaps = false
                setOnClickListener {
                    if (index == 3) showMessages() else if (index != 0) showInterruption(index == 1)
                }
            })
        }
        setContentView(layout)
    }
    private fun showMessages() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,100,32,32) }
        layout.addView(TextView(this).apply { text = "Inbox" })
        layout.addView(TextView(this).apply { text = "A fictional private conversation with no numbers." })
        layout.addView(Button(this).apply { text = "Return to choices"; isAllCaps = false; setOnClickListener { showResumeChoices() } })
        setContentView(layout)
    }
    private fun showInterruption(privateField: Boolean) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 100, 32, 32) }
        if (privateField) layout.addView(android.widget.EditText(this).apply {
            hint = "OTP"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setText("582139")
        }) else layout.addView(TextView(this).apply { text = "CAPTCHA · Verify you are human" })
        layout.addView(Button(this).apply { text = "Return to choices"; isAllCaps = false; setOnClickListener { showResumeChoices() } })
        setContentView(layout)
    }
    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra("close_fixture", false) == true) finish()
        else if (intent?.getBooleanExtra("resume_fixture", false) == true) showResumeChoices()
    }
}
