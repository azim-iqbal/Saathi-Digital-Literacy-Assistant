package com.saathi.core

/** Local review checklist, not proof of model correctness or an official eligibility verdict. */
data class EvidenceStep(val id: String, val title: String, val dependencies: List<String>, val quote: String, val evidenceId: String, val sourceTitle: String = "", val sourceUrl: String = "")
data class EvidenceCriterion(val id: String, val quote: String, val evidenceId: String, val sourceTitle: String = "", val sourceUrl: String = "")
class EvidencePlan(val originalGoal: String, val steps: List<EvidenceStep>, val criteria: List<EvidenceCriterion>,
                   val expiresAtMs: Long) {
    private val completed = mutableSetOf<String>()
    private val facts = mutableMapOf<String, Boolean>()
    var reviewed = false; private set
    init {
        require(steps.size in 1..12 && criteria.size <= 12)
        val ids = steps.map { it.id }.toSet()
        require(ids.size == steps.size && criteria.map { it.id }.toSet().size == criteria.size)
        require(steps.all { s -> s.dependencies.distinct().size == s.dependencies.size && s.dependencies.all { it in ids } })
        val visiting = mutableSetOf<String>(); val visited = mutableSetOf<String>()
        fun visit(id: String) {
            require(id !in visiting)
            if (id in visited) return
            visiting += id; steps.single { it.id == id }.dependencies.forEach(::visit)
            visiting -= id; visited += id
        }
        ids.forEach(::visit)
    }
    fun review(nowMs: Long): Boolean { reviewed = nowMs < expiresAtMs; return reviewed }
    fun invalidateContext() { reviewed = false }
    fun next(nowMs: Long): EvidenceStep? = if (!reviewed || nowMs >= expiresAtMs) null else
        steps.firstOrNull { it.id !in completed && it.dependencies.all(completed::contains) }
    fun confirm(id: String, nowMs: Long): Boolean {
        if (next(nowMs)?.id != id) return false
        completed += id; return true
    }
    fun setFact(id: String, value: Boolean?) {
        require(criteria.any { it.id == id })
        if (value == null) facts.remove(id) else facts[id] = value
    }
    fun fact(id: String): Boolean? = facts[id]
    fun eligibility(nowMs: Long): String = when {
        !reviewed || nowMs >= expiresAtMs -> "NOT_EVALUATED"
        criteria.isEmpty() || facts.size < criteria.size -> "INSUFFICIENT_INFORMATION"
        facts.values.any { !it } -> "CONDITION_NOT_MET"
        else -> "POSSIBLY_ELIGIBLE"
    }
    fun complete(nowMs: Long) = reviewed && nowMs < expiresAtMs && completed.size == steps.size
}
