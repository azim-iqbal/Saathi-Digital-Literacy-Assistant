package com.saathi

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class TermsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(44, 48, 44, 48) }
        content.addView(TextView(this).apply { text = "Where Saathi can guide"; textSize = 27f; setTextColor(Color.rgb(11, 107, 90)); setTypeface(null, android.graphics.Typeface.BOLD) })
        content.addView(TextView(this).apply {
            text = "This build provides local Demo Bill Pay practice using synthetic data. It does not guide real payments, bookings, websites, or other apps.\n\nScreen capture and cloud AI guidance are disabled. Never enter real credentials in the demo.\n\nYou perform every tap. Sensitive-field filtering is imperfect.\n\nStop ends observation and removes session guidance. Voice input in the task screen uses your Android recognition service, which may use a network. Spoken prompts use the installed TTS engine. No offline voice guarantee is made."
            textSize = 16f; setTextColor(Color.DKGRAY); setLineSpacing(8f, 1f); setPadding(0, 28, 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(content) })
    }
}
