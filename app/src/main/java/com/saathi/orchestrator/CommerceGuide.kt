package com.saathi.orchestrator

import com.saathi.accessibility.SensitiveContent
import com.saathi.core.*
import java.text.Normalizer
import java.util.Locale

/** Local discovery/review guidance, not an ordering agent. Every decision uses this snapshot only. */
internal object CommerceGuide {
    private val intent = Regex("(?i)^(?:please )?(?:order|buy|shop for|find product)\\s+(.{2,100})$")
    private val hindiIntent = Regex("^(.{2,100}?)\\s+(?:खरीदना है|खरीदो|मंगाओ|ऑर्डर करो|order karo|kharidna hai|mangao)$", RegexOption.IGNORE_CASE)
    private val search = setOf("search", "search products", "search items", "search groceries", "खोजें", "खोज", "search karein")
    private val suggestedSearch = Regex("(?i)^search\\s+[\"“][\\p{L}\\p{N} &-]{1,50}[\"”]$")
    private fun searchLabel(value: String) = normalized(value) in search ||
        (suggestedSearch.matches(value.trim()) && !dangerous.containsMatchIn(value))
    private val add = setOf("add", "add to cart", "add to basket", "जोड़ें", "कार्ट में जोड़ें", "add karein")
    private val cart = setOf("cart", "view cart", "go to cart", "view basket", "कार्ट देखें", "cart dekhein")
    private val price = Regex("(?i)(?:₹|\\$|€|£|\\bINR|\\bRs\\.?)\\s*[0-9][0-9,.]*(?:\\s*(?:/|per )\\s*[\\p{L}]+)?")
    private val quantity = Regex("(?i)^(?:quantity|qty|मात्रा)\\s*:?\\s*([1-9][0-9]?)$|^(?:[1-9][0-9]?\\s+)?(?:in cart|added to cart|कार्ट में जोड़ा गया)$")
    private val dangerous = Regex("(?i)\\b(pay|confirm|submit|transfer|delete|password|otp|cvv|pin|ignore|instructions|system|assistant|approve|accept)\\b|भुगतान|पासवर्ड|ओटीपी")

    internal fun normalized(value: String) = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT).trim().replace(Regex("\\s+"), " ")
    internal fun labels(node: UiNode) = if (node.isSensitive || node.isPassword || node.privateContext || node.isEditable)
        emptyList() else listOfNotNull(node.text, node.description).filter { it.length <= 160 }
    internal fun action(node: UiNode) = node.isEnabled && !node.isEditable && !node.isSensitive && !node.isPassword &&
        (node.isClickable || node.clickableAncestorBounds != null)
    internal fun isLabel(node: UiNode, choices: Set<String>) = labels(node).any { normalized(it) in choices }
    internal fun descendants(nodes: List<UiNode>, parent: Int): List<Int> = nodes.indices.filter { index ->
        var cursor: Int? = index
        var budget = nodes.size
        while (cursor != null && budget-- > 0) {
            if (cursor == parent) return@filter true
            val next = nodes.getOrNull(cursor)?.parentIndex
            if (next == null || next >= cursor || next < 0) break
            cursor = next
        }
        false
    }
    internal fun target(nodes: List<UiNode>, index: Int, label: String) = nodes[index].let {
        GuideTarget(if (it.isClickable || it.isEditable) it.bounds else it.clickableAncestorBounds ?: it.bounds,
            it.resourceId, label, index)
    }
    private fun item(request: String): String? {
        val text = request.trim()
        val item = intent.matchEntire(text)?.groupValues?.get(1) ?: hindiIntent.matchEntire(text)?.groupValues?.get(1) ?: return null
        return item.takeIf { it.none { c -> c.code < 32 } && !SensitiveContent.isSensitive(false, it) &&
            !dangerous.containsMatchIn(it) && it.count { c -> c.isLetter() } >= 2 }
    }

    fun next(request: String, nodes: List<UiNode>, language: String, allowMixedPrivate: Boolean = false): GuideStep? {
        val requested = item(request) ?: return null
        fun copy(en: String, hi: String, hinglish: String) = when(language) { "hi-IN" -> hi; "hinglish" -> hinglish; else -> en }
        fun step(text: String, index: Int? = null, label: String = "") = GuideStep(text, language,
            index?.let { target(nodes, it, label) }, "Reobserve after the user's choice; no purchase or completion inferred.", false)
        val interruption = ScreenInterruption.reason(nodes)
        if (interruption != null && !(allowMixedPrivate && interruption == ScreenInterruption.Reason.PRIVATE)) return null
        val words = normalized(requested).split(Regex("[^\\p{L}\\p{N}]+" )).filter { it.isNotEmpty() }
        fun matches(node: UiNode) = labels(node).any { raw ->
            val tokens = normalized(raw).split(Regex("[^\\p{L}\\p{N}]+" )).toSet()
            words.all { it in tokens } && !dangerous.containsMatchIn(raw)
        }
        val review = nodes.any { !action(it) && isLabel(it, setOf("cart", "your cart", "basket", "checkout", "order summary", "कार्ट", "ऑर्डर सारांश")) }
        if (review) return step(copy("Review the items, quantities, delivery details and total in the app. You decide whether to continue; Saathi will not place or confirm an order.",
            "ऐप में सामान, मात्रा, डिलीवरी और कुल कीमत जाँचें। आगे बढ़ने का निर्णय आपका है; साथी ऑर्डर नहीं करेगा।",
            "Items, quantity, delivery aur total khud review karein. Aage badhna aapka decision hai; Saathi order confirm nahi karega."))
        val carts = nodes.indices.filter { action(nodes[it]) && isLabel(nodes[it], cart) }
            .distinctBy { target(nodes, it, "").bounds.let { r -> listOf(r.left, r.top, r.right, r.bottom) } }
        // A product must share a real ancestor with a price and exactly one ADD/quantity control.
        // Never select the nearest ADD on screen, nor use the entire root as a product card.
        data class Card(val scope: Int, val title: Int, val addIndex: Int?, val inCart: Boolean)
        val cards = nodes.indices.filter { matches(nodes[it]) }.mapNotNull { title ->
            var parent = nodes[title].parentIndex
            var budget = nodes.size
            while (parent != null && budget-- > 0) {
                val p = nodes.getOrNull(parent) ?: break
                if (p.parentIndex == null) break // Root is not evidence of a product/control relationship.
                val members = descendants(nodes, parent)
                val additions = members.filter { action(nodes[it]) && isLabel(nodes[it], add) }
                    .distinctBy { target(nodes, it, "").bounds.let { r -> listOf(r.left,r.top,r.right,r.bottom) } }
                val quantities = members.filter { labels(nodes[it]).any(quantity::matches) }
                // Some apps render an in-card quantity picker as separate `-`, `1`, `+` nodes
                // instead of exposing a combined "Quantity: 1" label. A lone positive number is
                // only treated as an added state when this exact card has no Add control and the
                // same screen exposes one unambiguous cart action. This remains snapshot-local;
                // it does not infer a purchase or choose a product.
                val compactQuantity = additions.isEmpty() && carts.size == 1 && members.any { index ->
                    labels(nodes[index]).any { raw -> normalized(raw).matches(Regex("^[1-9][0-9]?$")) }
                }
                val prices = members.filter { labels(nodes[it]).any(price::containsMatchIn) }
                val conflictingBranch = members.filter { nodes[it].parentIndex == parent }.any { child ->
                    val branch = descendants(nodes, child)
                    branch.any { action(nodes[it]) && isLabel(nodes[it], add) } &&
                        branch.any { labels(nodes[it]).any(price::containsMatchIn) } &&
                        branch.none { matches(nodes[it]) }
                }
                if (!conflictingBranch && prices.isNotEmpty() && additions.size <= 1 &&
                    (additions.size == 1 || quantities.isNotEmpty() || compactQuantity)) {
                    return@mapNotNull Card(parent, title, additions.singleOrNull(), quantities.isNotEmpty() || compactQuantity)
                }
                if (p.parentIndex == parent) break
                parent = p.parentIndex
            }
            null
        }.distinctBy { it.scope }
        if (cards.size > 1) return step(copy(
            "Several products match “$requested”. Compare the visible brand, size and price and open the one you want; I will not choose a substitute for you.",
            "“$requested” के कई उत्पाद हैं। ब्रांड, मात्रा और कीमत जाँचकर अपनी पसंद का उत्पाद खोलें।",
            "“$requested” ke kai products hain. Brand, size aur price compare karke apni pasand ka product kholein."))
        val card = cards.singleOrNull()
        if (card != null) {
            if (card.inCart) {
                return step(copy("This product shows an added/quantity state. Review its quantity and open the cart to review your choices; this is not an order confirmation.",
                    "इस उत्पाद पर मात्रा या जोड़े जाने का संकेत है। मात्रा जाँचें और कार्ट में अपनी पसंद की समीक्षा करें; ऑर्डर की पुष्टि नहीं हुई है।",
                    "Is product par added/quantity state hai. Quantity check karke cart review karein; order confirm nahi hua."),
                    carts.singleOrNull(), "View cart")
            }
            return step(copy("This product matches “$requested”. Check its brand, size and displayed price yourself. If it is the one you want, use the marked Add control.",
                "यह उत्पाद “$requested” से मेल खाता है। ब्रांड, मात्रा और कीमत जाँचें। यही चाहिए तो चिन्हित Add दबाएँ।",
                "Yeh product “$requested” se match karta hai. Brand, size aur price check karein. Yahi chahiye toh marked Add dabayein."), card.addIndex, "Add")
        }
        val searches = nodes.indices.filter { i ->
            val n = nodes[i]
            n.isEnabled && !n.isSensitive && !n.isPassword &&
                ((action(n) && labels(n).any(::searchLabel)) || (n.isEditable && n.hint?.let(::searchLabel) == true))
        }.distinctBy { target(nodes, it, "").bounds.let { r -> listOf(r.left,r.top,r.right,r.bottom) } }
        if (searches.size == 1) return step(copy("Use Search to look for “$requested”. Type or speak the search in the app, then review the results. I do not read entered values.",
            "Search में “$requested” खोजें। ऐप में खुद लिखें या बोलें, फिर परिणाम जाँचें। मैं भरे हुए मान नहीं पढ़ता।",
            "Search mein “$requested” khojein. App mein khud type ya speak karein, phir results dekhein. Main entered values nahi padhta."), searches.single(), "Search")
        return step(copy("I cannot uniquely connect “$requested” to a product and its controls on this screen. Open the product or Search in the app; I will check the next screen. No purchase has been made by Saathi.",
            "इस स्क्रीन पर “$requested” का सही उत्पाद और नियंत्रण स्पष्ट नहीं हैं। ऐप में उत्पाद या Search खोलें; अगली स्क्रीन फिर जाँचूँगा।",
            "Is screen par “$requested” ka product aur controls clear nahi hain. Product ya Search kholein; next screen phir check hogi."))
    }
}
