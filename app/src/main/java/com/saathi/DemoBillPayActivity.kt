package com.saathi

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import com.saathi.ui.glass.applySaathiGlass

/** A safe, native demo surface: no money moves and its PIN input is a real password node. */
class DemoBillPayActivity : Activity() {
    private companion object {
        const val INK = 0xFF152A23.toInt()
        const val MUTED = 0xFF5D7069.toInt()
        const val CANVAS = 0xFFF4F8F6.toInt()
        const val BRAND = 0xFF075E19.toInt()
    }

    private lateinit var content: LinearLayout
    private var page = "home"
    private var selected = "electricity"

    private data class Biller(
        val title: String,
        val provider: String,
        val accountHint: String,
        val paymentNoun: String,
        val successMessage: String
    )

    private val electricity = Biller(
        title = "Electricity bill",
        provider = "Maharashtra State Electricity Board",
        accountHint = "Electricity account number",
        paymentNoun = "bill",
        successMessage = "Your electricity bill has been paid"
    )
    private val water = Biller(
        title = "Water bill",
        provider = "Municipal Water Services",
        accountHint = "Water consumer number",
        paymentNoun = "bill",
        successMessage = "Your water bill has been paid"
    )
    private val dth = Biller(
        title = "DTH recharge",
        provider = "Saathi TV Services",
        accountHint = "Subscriber ID",
        paymentNoun = "recharge",
        successMessage = "Your DTH recharge is complete"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected = savedInstanceState?.getString("practice_category") ?: "electricity"
        when (savedInstanceState?.getString("practice_page")) {
            "billers" -> showBillers()
            "form" -> showForm(when(selected) { "water" -> water; "dth" -> dth; else -> electricity })
            else -> showHome()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("practice_page", page)
        outState.putString("practice_category", selected)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Uses local practice back stack")
    override fun onBackPressed() {
        when (page) {
            "form" -> showBillers()
            "billers", "detour", "success" -> showHome()
            else -> super.onBackPressed()
        }
    }

    override fun onPause() {
        com.saathi.orchestrator.SaathiSession.invalidateScreen()
        super.onPause()
    }

    private fun showHome() {
        page = "home"
        base("Good afternoon")
        text("What would you like to take care of today?", 16, MUTED)
        sectionLabel("EVERYDAY TASKS")
        tile("Recharge & Pay Bills", "Electricity, water, DTH and more", R.id.recharge_bills, BRAND) { showBillers() }
        tile("Send Money", "Practice placeholder", R.id.send_money, 0xFF2563A9.toInt()) { showDetour() }
        tile("Scan QR", "Practice placeholder", R.id.scan_qr, 0xFF8656B4.toInt()) { showDetour() }
        tile("History", "Practice placeholder", R.id.history, 0xFF607069.toInt()) { showDetour() }
    }
    private fun showBillers() {
        page = "billers"
        base("Pay bills")
        backButton { showHome() }
        text("Choose a category. You can change it any time.", 16, MUTED)
        sectionLabel("SELECT A SERVICE")
        tile("Electricity", "Pay your power bill", R.id.electricity_biller, 0xFFE8A91F.toInt()) { showForm(electricity) }
        tile("Water", "Pay your water bill", R.id.water_biller, 0xFF3395C2.toInt()) { showForm(water) }
        tile("DTH", "Recharge your TV", R.id.dth_biller, 0xFF8058B0.toInt()) { showForm(dth) }
    }
    private fun showForm(biller: Biller) {
        page = "form"
        selected = when(biller) { water -> "water"; dth -> "dth"; else -> "electricity" }
        base(biller.title)
        text("Practice · " + biller.title, 16, MUTED).id = marker(biller)
        backButton { showBillers() }
        text(biller.provider, 16, MUTED)
        sectionLabel("PAYMENT DETAILS")
        val account = input(biller.accountHint, R.id.account_input, InputType.TYPE_CLASS_NUMBER)
        val amount = input("Amount (₹)", R.id.amount_input, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val pin = input("PIN", R.id.pin_input, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        val privacy = TextView(this).apply {
            text = "Made-up details only. The conversation microphone is off on this form."
            setTextColor(0xFF9D3140.toInt())
            textSize = 14f
            setPadding(0, dp(4), 0, dp(8))
        }
        content.addView(privacy)

        val pay = Button(this).apply {
            applySaathiGlass()
            id = R.id.pay_button
            text = "Finish practice"
            isAllCaps = false
            isEnabled = false
            setOnClickListener { showSuccess(biller) }
        }
        content.addView(pay, wide().apply { topMargin = dp(12) })
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { pay.isEnabled = account.text.isNotBlank() && amount.text.isNotBlank() && pin.text.isNotBlank() }
            override fun afterTextChanged(s: Editable?) = Unit
        }
        account.addTextChangedListener(watcher)
        amount.addTextChangedListener(watcher)
        pin.addTextChangedListener(watcher)
    }
    private fun showSuccess(biller: Biller) {
        page = "success"
        base("All set")
        text("Practice · " + biller.title, 16, MUTED).id = marker(biller)
        backButton { showBillers() }
        text("✓", 72, BRAND).apply { gravity = Gravity.CENTER }
        val title = text(biller.successMessage, 24, BRAND)
        title.id = R.id.success_title
        text("This is a safe demo. No real ${biller.paymentNoun} was made.", 16, MUTED)
        content.addView(Button(this).apply {
            applySaathiGlass()
            text = "Back to home"
            isAllCaps = false
            setOnClickListener { showHome() }
        }, wide().apply { topMargin = dp(20) })
    }
    private fun base(title: String) {
        com.saathi.orchestrator.SaathiSession.invalidateScreen()
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(36), dp(22), dp(26))
            setBackgroundColor(CANVAS)
        }
        setContentView(ScrollView(this).apply { addView(content) })
        text(title, 30, Color.rgb(22, 39, 54))
    }
    private fun marker(biller: Biller) = when(biller) {
        water -> R.id.practice_water
        dth -> R.id.practice_dth
        else -> R.id.practice_electricity
    }
    private fun backButton(action: () -> Unit) {
        content.addView(Button(this).apply {
            id = R.id.practice_back
            text = "Back to choices"
            isAllCaps = false
            applySaathiGlass()
            setOnClickListener { action() }
        }, wide())
    }
    private fun showDetour() {
        page = "detour"
        base("A different path")
        text("This part is outside your bill practice. You can go back whenever you're ready.", 17, MUTED).id = R.id.practice_detour
        backButton { showHome() }
    }
    private fun tile(title: String, subtitle: String, id: Int, color: Int, click: () -> Unit) {
        val box = LinearLayout(this).apply {
            this.id = id
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(color, 22)
            contentDescription = title
            isClickable = true
            isFocusable = true
            setOnClickListener { click() }
            setOnTouchListener { view, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> view.animate().scaleX(.985f).scaleY(.985f).setDuration(80).start()
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
                false
            }
        }
        box.addView(TextView(this).apply { text = title; textSize = 20f; setTextColor(Color.WHITE); setTypeface(null, android.graphics.Typeface.BOLD) })
        box.addView(TextView(this).apply { text = subtitle; textSize = 14f; setTextColor(Color.WHITE); setPadding(0, dp(4), 0, 0) })
        content.addView(box, wide().apply { topMargin = dp(14) })
    }
    private fun input(hint: String, id: Int, inputType: Int): EditText {
        val view = EditText(this).apply {
            this.id = id
            this.hint = hint
            this.inputType = inputType
            isSaveEnabled = false
            contentDescription = hint
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.WHITE, 16, 0xFFD6E4DE.toInt())
        }
        content.addView(view, wide().apply { topMargin = dp(12) })
        return view
    }

    private fun text(value: String, size: Int, color: Int): TextView = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        setPadding(0, dp(4), 0, dp(8))
        content.addView(this)
    }
    private fun sectionLabel(value: String) = content.addView(TextView(this).apply {
        text = value
        textSize = 12f
        letterSpacing = .12f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(MUTED)
        setPadding(0, dp(18), 0, dp(3))
    })
    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
    private fun wide() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    private fun rounded(color: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        stroke?.let { setStroke(dp(1), it) }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
