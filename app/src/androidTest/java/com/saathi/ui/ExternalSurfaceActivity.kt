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
    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra("close_fixture", false) == true) finish()
    }
}
