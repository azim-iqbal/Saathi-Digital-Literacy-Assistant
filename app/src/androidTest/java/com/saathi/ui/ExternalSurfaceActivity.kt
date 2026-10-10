package com.saathi.ui

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Installed in the separate test APK/UID. No production entry, real data or remote page. */
class ExternalSurfaceActivity : Activity() {
    private var formDependent: android.widget.EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("reactive_form", false)) { showReactiveForm(); return }
        if (intent.getBooleanExtra("portal_fixture", false)) { showPortal(); return }
        if (intent.getBooleanExtra("paste_fixture", false)) { showPaste(); return }
        if (intent.getBooleanExtra("close_fixture", false)) { finish(); return }
        if (intent.getBooleanExtra("oversized_fixture", false)) { showOversizedTree(); return }
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
    private fun showReactiveForm() {
        if (intent.getBooleanExtra("web_form", false)) {
            setContentView(WebView(this).apply {
                settings.javaScriptEnabled = true
                loadDataWithBaseURL(null, """<html><meta name='viewport' content='width=device-width, initial-scale=1'><body>
                    <input id='city' aria-label='City *' placeholder='City *' required><input aria-label='Company (optional)' placeholder='Company (optional)'>
                    <input aria-label='State *' placeholder='State *' required>
                    <button onclick='document.activeElement.blur()'>Finish editing</button>
                    <button onclick='document.getElementById("city").setAttribute("aria-invalid","true")'>Invalid city</button>
                    <button onclick='document.getElementById("city").removeAttribute("aria-invalid")'>Correct city</button>
                    <span id='conditional'></span>
                    <button onclick='setTimeout(function(){document.getElementById("conditional").innerHTML="&lt;input aria-label=\"Country *\" placeholder=\"Country *\" required&gt;"},600)'>Show dependent</button>
                    <button onclick='document.getElementById("conditional").innerHTML=""'>Hide dependent</button>
                    <button onclick='document.getElementById("conditional").innerHTML="&lt;input type=\"password\" aria-label=\"OTP\" value=\"fictional-secret-canary\"&gt;"'>Private step</button>
                    <button disabled>Submit</button></body></html>""", "text/html", "UTF-8", null)
            }); return
        }
        val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(32,100,32,32); isFocusableInTouchMode=true }
        fun field(h: String)=android.widget.EditText(this).apply { hint=h; inputType=android.text.InputType.TYPE_CLASS_TEXT; isSingleLine=true }
        val city=field("City *"); val company=field("Company (optional)"); val state=field("State *")
        layout.addView(city); layout.addView(company); layout.addView(state)
        layout.addView(Button(this).apply { isAllCaps=false; text="Finish editing"; setOnClickListener { layout.requestFocus(); getSystemService(android.view.inputmethod.InputMethodManager::class.java)?.hideSoftInputFromWindow(windowToken,0) } })
        layout.addView(Button(this).apply { isAllCaps=false; text="Invalid city"; setOnClickListener { city.error="Review this field"; layout.requestFocus() } })
        layout.addView(Button(this).apply { isAllCaps=false; text="Correct city"; setOnClickListener { city.error=null; layout.requestFocus() } })
        layout.addView(Button(this).apply { isAllCaps=false; text="Show dependent"; setOnClickListener { if(formDependent==null) { formDependent=field("Country *"); layout.addView(formDependent,3) }; layout.requestFocus() } })
        layout.addView(Button(this).apply { isAllCaps=false; text="Hide dependent"; setOnClickListener { layout.removeView(formDependent); formDependent=null; layout.requestFocus() } })
        layout.addView(Button(this).apply { isAllCaps=false; text="Submit"; isEnabled=false })
        setContentView(layout); layout.requestFocus()
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
    private fun showPaste() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,100,32,32) }
        layout.addView(android.widget.EditText(this).apply { hint = "Incident description"; id = 0x1020011 })
        setContentView(layout)
    }
    private fun showPortal() {
        setContentView(WebView(this).apply {
            settings.javaScriptEnabled = true
            loadDataWithBaseURL("https://portal.fixture.test/", """
                <html><meta name='viewport' content='width=device-width, initial-scale=1'>
                <style>body{padding:24px}button{font-size:22px;padding:16px;margin:12px}input,textarea{font-size:20px}</style>
                <body><h1>Controlled reporting fixture</h1><main id='page'></main>
                <script>
                const pages = [
                  '<button>Help</button><p>Public reporting information</p>',
                  '<label>Password<input type="password" value="fictional-secret-canary"></label>',
                  '<label>OTP<input type="password" inputmode="numeric" value="582139"></label>',
                  '<p>CAPTCHA: Verify you are human</p>',
                  '<button>Help</button><p>Category and evidence requirements</p>',
                  '<label>Incident description<textarea>Fictional private narrative canary</textarea></label>',
                  '<p>Service temporarily unavailable. Please try again later.</p>',
                  '<button>Help</button><p>Review your report privately.</p><label><input type="checkbox">Legal declaration</label><button disabled>Submit complaint</button>'
                ];
                let page=0;
                function render(){document.getElementById('page').innerHTML=pages[page]+(page<7?'<button onclick="page++;render()">Fixture next</button>':'');}
                render();
                </script></body></html>
            """.trimIndent(), "text/html", "UTF-8", null)
        })
    }

    private fun showOversizedTree() {
        setContentView(LargeAccessibilitySurface(this))
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra("close_fixture", false) == true) finish()
        else if (intent?.getBooleanExtra("oversized_fixture", false) == true) showOversizedTree()
        else if (intent?.getBooleanExtra("resume_fixture", false) == true) showResumeChoices()
    }
}
