package com.saathi.gateway

import com.saathi.core.*

/** Evidence is shown as attributed text, never rendered as HTML, commands or auto-opened links. */
internal object ResearchCodec {
    private fun text(value: Any?, max: Int = 4000) = (value as String).also {
        require(it.length in 1..max && it.none { c -> c.code < 32 || c.isSurrogate() })
    }
    fun decode(body: String, expectedId: String, planning: Boolean, locale: String = "en-IN"): GatewayResult = try {
        fun local(en: String, hi: String, hinglish: String) = when (locale) { "hi-IN" -> hi; "hinglish" -> hinglish; else -> en }
        val data = MockGatewayCodec.parse(body, research = true)
        if (data["status"] == "rejected") {
            require(data.keys == setOf("status", "reason") || data.keys == setOf("status", "reason", "mode"))
            val reason = text(data["reason"], 80)
            require(reason.matches(Regex("[a-z_]+")))
            GatewayResult.Rejected(reason)
        } else {
            require(data["request_id"] == expectedId)
            var incidentPlan = false
            val container = if (planning) {
                require(data["status"] == "accepted" && data["mode"] == "dual_ai")
                require(data.keys == setOf("status", "mode", "request_id", "session_id", "screen_revision", "provenance", "plan"))
                require(data["screen_revision"] == 1L)
                require(text(data["session_id"],64).startsWith("plan_"))
                val providers = data["provenance"] as List<*>
                require(providers.size == 2)
                require(providers.map { item ->
                    val provider = item as Map<*, *>
                    require(provider.keys == setOf("provider", "model"))
                    require(text(provider["model"],100).matches(Regex("[A-Za-z0-9._/-]+")))
                    provider["provider"]
                }.toSet() == setOf("gemini", "groq"))
                val plan = data["plan"] as Map<*, *>
                incidentPlan = plan["kind"] == "incident_hypothesis"
                if (incidentPlan) {
                    require(plan.keys == setOf("kind","original_goal","jurisdiction","status","scope","citations","evidence","retry_payment","limitations"))
                    require(plan["status"] == "REVIEW_REQUIRED" && plan["retry_payment"] == false)
                } else {
                    require(plan.keys == setOf("original_goal", "jurisdiction", "steps", "criteria", "status", "eligibility", "evidence", "limitations"))
                    require(plan["status"] == "REVIEW_REQUIRED" && plan["eligibility"] == "NOT_EVALUATED")
                }
                plan
            } else {
                require(data["status"] == "researched" && (data.keys == setOf("status", "request_id", "evidence", "limitations") || data.keys == setOf("status", "request_id", "evidence", "limitations", "incident")))
                data
            }
            val evidence = container["evidence"] as List<*>
            require(evidence.size <= 4)
            val byId = evidence.associate { item ->
                val e = item as Map<*, *>
                val id = text(e["evidence_id"], 24); require(id.matches(Regex("[a-f0-9]{24}")))
                id to e
            }
            require(byId.size == evidence.size)
            val planSteps = mutableListOf<EvidenceStep>()
            val planCriteria = mutableListOf<EvidenceCriterion>()
            var oldest = System.currentTimeMillis()
            val report = buildString {
                if (incidentPlan) {
                    val scope = container["scope"]
                    require(scope in setOf("USER_SPECIFIC","SERVICE_WIDE","REGIONAL","ACCOUNT_SPECIFIC","UNKNOWN"))
                    appendLine(local("Proposed explanation · Review required", "संभावित कारण · जाँच बाकी", "Sambhav wajah · Jaanch baaki"))
                    appendLine(when(scope) {
                        "REGIONAL" -> local("The sources may describe a regional issue.", "स्रोत किसी क्षेत्र की समस्या बता सकते हैं।", "Sources kisi kshetra ki samasya bata sakte hain.")
                        "SERVICE_WIDE" -> local("The sources may describe a service-wide issue.", "स्रोत पूरी सेवा की समस्या बता सकते हैं।", "Sources poori service ki samasya bata sakte hain.")
                        "ACCOUNT_SPECIFIC", "USER_SPECIFIC" -> local("The sources may describe an account or user-specific issue. Saathi has not checked your account.", "स्रोत खाते या उपयोगकर्ता की समस्या बता सकते हैं। साथी ने आपका खाता नहीं जाँचा है।", "Sources account ya user ki samasya bata sakte hain. Saathi ne aapka account nahin jaancha hai.")
                        else -> local("The cause remains unknown.", "कारण अभी पता नहीं है।", "Wajah abhi pata nahin hai.")
                    })
                    appendLine(local("This hypothesis is not a confirmed diagnosis. Verify the excerpts; do not repeat a payment while its status is unclear.", "यह संभावित कारण है, पक्का निदान नहीं। स्रोत जाँचें; स्थिति अस्पष्ट होने पर दोबारा भुगतान न करें।", "Yeh sambhav wajah hai, pakka nidan nahin. Sources jaanchein; status saaf na ho toh dobara payment na karein."))
                    val citations = container["citations"] as List<*>
                    require(citations.size in 1..4)
                    val seen=mutableSetOf<Pair<String,String>>()
                    citations.forEach { raw ->
                        val citation=raw as Map<*, *>
                        require(citation.keys == setOf("evidence_id","quote"))
                        val id=text(citation["evidence_id"],24)
                        val source=requireNotNull(byId[id])
                        val quote=text(citation["quote"],300)
                        require(quote.length>=8 && text(source["snippet"]).contains(quote) && seen.add(id to quote))
                        require(source["claim_type"]=="outage")
                        if (scope!="UNKNOWN") {
                            require(source["source_type"] in setOf("official","primary"))
                            require(source["jurisdiction"] == container["jurisdiction"] || source["jurisdiction"] == "global")
                            if (scope=="REGIONAL") require(source["jurisdiction"]==container["jurisdiction"])
                        }
                        appendLine("“$quote”\n${text(source["source_title"],160)}\n${text(source["source_url"],1024)}")
                    }
                }
                if (data.containsKey("incident")) {
                    val incident = data["incident"] as Map<*, *>
                    require(incident.keys == setOf("scope","confidence","official","official_impairment","conflicting_official_reports","anecdotal","undated_reports","other","retry_payment","reason"))
                    require(incident["scope"] in setOf("SERVICE_WIDE","UNKNOWN") && incident["retry_payment"] == false)
                    require(incident["confidence"] == "possible_source_report_not_user_diagnosis")
                    for (key in listOf("official","official_impairment","anecdotal","undated_reports","other")) {
                        require((incident[key] as List<*>).all { it in byId })
                    }
                    appendLine(if (incident["scope"] == "SERVICE_WIDE") local("An official source reports service impairment. This may explain the issue; it does not prove the cause for your account.","आधिकारिक स्रोत सेवा में समस्या बता रहा है। यह संभावित कारण है, आपके खाते की समस्या का प्रमाण नहीं।","Official source service mein samasya bata raha hai. Yeh sambhav wajah hai, aapke account ki samasya ka pramaan nahin.")
                        else local("The available sources do not establish the cause. Missing reports do not prove an account-specific problem.","मिले स्रोत कारण तय नहीं करते। रिपोर्ट न मिलना आपके खाते की समस्या का प्रमाण नहीं है।","Mile sources wajah tay nahin karte. Report na milna account ki samasya ka pramaan nahin."))
                    if (incident["conflicting_official_reports"] == true) appendLine(local("Official reports conflict. Wait for clearer information.","आधिकारिक रिपोर्ट अलग बातें कह रही हैं। स्पष्ट जानकारी की प्रतीक्षा करें।","Official reports alag baatein keh rahi hain. Saaf jaankari ka intezaar karein."))
                    appendLine(local("Community reports are anecdotal. Do not repeat a payment while its status is uncertain.","समुदाय की रिपोर्ट व्यक्तिगत अनुभव हैं। भुगतान की स्थिति स्पष्ट होने तक दोबारा भुगतान न करें।","Community reports logon ke anubhav hain. Payment ka status saaf hone tak dobara payment na karein."))
                }
                if (planning && !incidentPlan) {
                    appendLine(local("Proposed plan · Review required\nOriginal goal: ", "प्रस्तावित योजना · जाँच बाकी\nमूल काम: ", "Sujhaaya plan · Jaanch baaki\nMool kaam: ") + text(container["original_goal"],160))
                    appendLine(local("Eligibility has not been established. These are proposed relationships, not verified instructions.","पात्रता तय नहीं हुई है। ये प्रस्तावित संबंध हैं, सत्यापित निर्देश नहीं।","Patrata tay nahin hui hai. Yeh sujhaaye sambandh hain, verify kiye nirdesh nahin."))
                    val steps = container["steps"] as List<*>
                    require(steps.size in 1..12)
                    steps.forEach { item ->
                        val step = item as Map<*, *>
                        require(step.keys == setOf("id","title","depends_on","evidence_id","quote","completion"))
                        require(step["completion"] == "USER_CONFIRMATION")
                        val id = text(step["id"],3); require(id.matches(Regex("s([1-9]|1[0-2])")))
                        val dependencies = (step["depends_on"] as List<*>).map { text(it,3).also { d -> require(d.matches(Regex("s([1-9]|1[0-2])"))) } }
                        val e = requireNotNull(byId[step["evidence_id"]])
                        require(e["source_type"] in setOf("official", "primary"))
                        require(e["jurisdiction"] == container["jurisdiction"] || e["jurisdiction"] == "global")
                        val quote = text(step["quote"],300); require(text(e["snippet"]).contains(quote))
                        planSteps += EvidenceStep(id, text(step["title"],120), dependencies, quote, text(step["evidence_id"],24), text(e["source_title"],160), text(e["source_url"],1024))
                        appendLine("\n$id · ${text(step["title"],120)}\n${local("Prerequisites", "पहले की शर्तें", "Pehle ki shartein")}: ${dependencies.joinToString().ifBlank { local("None proposed","कोई प्रस्तावित नहीं","Koi sujhaav nahin") }}")
                        appendLine("${local("Source excerpt", "स्रोत का अंश", "Source ka ansh")}: “$quote”\n${text(e["source_url"],1024)}\n${local("Completion requires your confirmation.", "पूरा होने की पुष्टि आपको करनी है।", "Poora hone ki pushti aapko karni hai.")}")
                    }
                }
                if (planning && !incidentPlan) {
                    val criteria = container["criteria"] as List<*>
                    require(criteria.size <= 12)
                    criteria.forEach { item ->
                        val criterion = item as Map<*, *>
                        require(criterion.keys == setOf("id","evidence_id","quote"))
                        val id = text(criterion["id"],3); require(id.matches(Regex("c([1-9]|1[0-2])")))
                        val eid = text(criterion["evidence_id"],24)
                        val quote = text(criterion["quote"],300)
                        val source = requireNotNull(byId[eid])
                        require(source["source_type"] in setOf("official", "primary"))
                        require(source["jurisdiction"] == container["jurisdiction"] || source["jurisdiction"] == "global")
                        require(text(source["snippet"]).contains(quote))
                        planCriteria += EvidenceCriterion(id, quote, eid, text(requireNotNull(byId[eid])["source_title"],160), text(requireNotNull(byId[eid])["source_url"],1024))
                    }
                }
                for (item in evidence) {
                    val e = item as Map<*, *>
                    val fields = setOf("evidence_id","source_url","source_title","source_type","authority_basis","retrieved_at_ms",
                        "published_or_updated","jurisdiction","snippet","content_hash","claim_type","confidence")
                    require(e.keys == fields || e.keys == fields + "freshness")
                    val kind = text(e["source_type"],20); require(kind in setOf("official","primary","secondary","community","unverified"))
                    require(e["confidence"] == "retrieved_not_independently_verified")
                    require(text(e["content_hash"],64).matches(Regex("[a-f0-9]{64}")))
                    require(e["claim_type"] in setOf("outage","pricing","deadline","eligibility","requirements","procedure","background"))
                    val authority = text(e["authority_basis"],160)
                    val freshness = e["freshness"]
                    if (freshness != null) require(freshness in setOf("expired", "publisher_date_invalid", "historical_report", "recent_fetch_date_unknown", "recent_fetch_publisher_date_unverified"))
                    val kindName = when (kind) {
                        "official" -> local("Official", "आधिकारिक", "Aadhikarik")
                        "primary" -> local("Primary", "प्राथमिक", "Prathmik")
                        "secondary" -> local("Secondary", "द्वितीयक", "Doosra source")
                        "community" -> local("Community", "समुदाय", "Community")
                        else -> local("Unverified", "असत्यापित", "Verify nahin")
                    }
                    val url = text(e["source_url"],1024)
                    val parsed = java.net.URI(url)
                    require(parsed.scheme == "https" && parsed.host != null && parsed.userInfo == null && parsed.query == null && parsed.fragment == null)
                    val at = e["retrieved_at_ms"] as Long; require(at > 0 && at <= System.currentTimeMillis() + 5000)
                    oldest = minOf(oldest, at)
                    appendLine("\n${text(e["source_title"],160)} · $kindName\n$url\n${local("Region", "क्षेत्र", "Kshetra")}: ${text(e["jurisdiction"],160)}")
                    appendLine(local("Authority review record: ", "स्रोत की जाँच का रिकॉर्ड: ", "Source ki jaanch ka record: ") + authority)
                    appendLine(local("Retrieval does not independently verify truth or current applicability.", "स्रोत मिलने से उसकी सच्चाई या मौजूदा लागू शर्तें साबित नहीं होतीं।", "Source milne se uski sachchai ya maujooda lagu shartein saabit nahin hoti."))
                    if (freshness in setOf("expired", "publisher_date_invalid", "historical_report")) appendLine(local("Old or invalid date: do not rely on this as current evidence.", "पुरानी या गलत तारीख: इसे मौजूदा प्रमाण न मानें।", "Purani ya galat tareekh: ise maujooda pramaan na maanein."))
                    appendLine("${local("Retrieved", "प्राप्त किया", "Prapt kiya")}: ${java.util.Date(at)}\n${local("Publisher date", "प्रकाशक की तारीख", "Publisher ki tareekh")}: ${e["published_or_updated"]?.let { text(it,80) } ?: local("Unknown","पता नहीं","Pata nahin")}")
                    appendLine("${local("Original source excerpt (untrusted; original language)", "स्रोत का मूल अंश (असत्यापित; मूल भाषा)", "Source ka asal ansh (verify nahin; asal bhasha)")}:\n“${text(e["snippet"])}”")
                    if (kind == "community") appendLine(local("Anecdotal report; not official policy or proof of an outage.","व्यक्तिगत अनुभव; आधिकारिक नियम या सेवा बंद होने का प्रमाण नहीं।","Kisi ka anubhav; official niyam ya service band hone ka pramaan nahin."))
                }
                append(local("\nSearch was bounded. Coverage, applicability and interpretation are not established. An empty result does not mean no requirements exist.", "\nखोज सीमित थी। पूरी जानकारी, लागू शर्तें और व्याख्या की शुद्धता तय नहीं हैं। खाली नतीजे का मतलब कोई शर्त न होना नहीं है।", "\nKhoj seemit thi. Poori jaankari, lagu shartein aur samajh ki sahi hone ki pushti nahin hai. Khaali natije ka matlab koi shart na hona nahin hai."))
            }
            GatewayResult.Research(expectedId, report, evidence.isNotEmpty(), if (planning && !incidentPlan)
                EvidencePlan(text(container["original_goal"],160), planSteps, planCriteria, oldest + 300_000, retrievedAtMs = oldest) else null)
        }
    } catch (_: Exception) { GatewayResult.Rejected("invalid_response") }
}
