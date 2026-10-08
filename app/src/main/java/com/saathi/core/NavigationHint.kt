package com.saathi.core

/** A cited label to locate locally. No click, text entry, clipboard or completion authority. */
data class NavigationHint(val kind: String, val label: String) {
    fun valid(quote: String): Boolean {
        if (java.text.Normalizer.normalize(label,java.text.Normalizer.Form.NFKC)!=label || label.any { Character.getType(it) in setOf(Character.FORMAT.toInt(),Character.CONTROL.toInt(),Character.SURROGATE.toInt()) }) return false
        if (Regex("(?i)खरीद|जमा|हटाएं|हटाएँ|मंजूर|मंज़ूर|\\b(bhugtan|bhejein|bhejo|kharidein|kharido|jama|hatao|manzoor)\\b").containsMatchIn(label)) return false
        if (kind !in setOf("READ_OPTION","FIELD_LABEL") || !LiveAiPolicy.allowed(label,80) || label != label.trim() ||
            label.any { it in "\"'“”‘’:/\\" } || DestinationPolicy.requiresProvenance(label) ||
            PrivateContextPolicy.blocksCloud(listOf(labelNode())) || BrowserSafetyPolicy.present(listOf(labelNode())) || ScreenErrorPolicy.present(listOf(labelNode()))) return false
        if (kind == "FIELD_LABEL" && Regex("(?i)\\b(name|email|phone|mobile|address|birth|account|card|identity|document|message|income|salary)\\b|नाम|ईमेल|फोन|पता|जन्म|खाता|naam|pata").containsMatchIn(label)) return false
        val quoted = listOf("“" to "”", "\"" to "\"", "‘" to "’", "'" to "'").map { it.first+label+it.second }.firstOrNull { it in quote } ?: return false
        val verb = if (kind=="READ_OPTION") "(?:select|choose|open|tap|click)" else "(?:find field|field)"
        val suffix = if (kind=="READ_OPTION") "(?:चुनें|खोलें|दबाएं|chunein|chuniye|kholein)" else "(?:फ़ील्ड|फील्ड|field)"
        return Regex("(?i)\\b$verb\\s+${Regex.escape(quoted)}").containsMatchIn(quote) ||
            Regex("${Regex.escape(quoted)}\\s+$suffix",RegexOption.IGNORE_CASE).containsMatchIn(quote)
    }
    private fun labelNode() = UiNode(android.graphics.Rect(),label,null,null,null,null,false,true,false)
    fun targetIndex(nodes: List<UiNode>): Int? {
        val candidates=nodes.mapIndexedNotNull { index,node ->
            if (!node.isEnabled || node.isSensitive || node.isPassword) return@mapIndexedNotNull null
            val matches = if (kind=="FIELD_LABEL") {
                // Values never supply a label. Filled fields are a manual handover.
                node.isEditable && !node.hasValue && listOf(node.hint,node.description).any { it?.trim()==label }
            } else !node.isEditable && !node.className.orEmpty().contains("EditText") &&
                (node.isClickable || node.clickableAncestorBounds!=null) && listOf(node.text,node.description).any { it?.trim()==label }
            index.takeIf { matches }
        }
        return candidates.singleOrNull()
    }
}
