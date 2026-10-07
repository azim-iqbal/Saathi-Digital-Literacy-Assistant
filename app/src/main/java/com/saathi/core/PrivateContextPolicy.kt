package com.saathi.core

/** Local context cues suppress cloud processing even when values have no numeric secrets.
 * This is conservative minimization, not semantic proof that every other page is public.
 */
object PrivateContextPolicy {
    private val privateSurface = Regex("(?i)(?:^|[^a-z])(inbox|conversation|message[ _-]?(body|thread)|email[ _-]?(body|subject)|recipient|compose[ _-]?(mail|message)|personal[ _-]?(details|information)|account[ _-]?details|document[ _-]?(body|editor))(?:$|[^a-z])|निजी जानकारी|व्यक्तिगत जानकारी|संदेश का पाठ|ईमेल|niji jaankari|vyaktigat jaankari")
    fun blocksCloud(nodes: List<UiNode>): Boolean = nodes.any { node ->
        sequenceOf(node.resourceId, node.hint, node.description, node.text).filterNotNull().any {
            privateSurface.containsMatchIn(it.take(512))
        }
    }
}
