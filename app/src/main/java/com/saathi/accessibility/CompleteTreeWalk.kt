package com.saathi.accessibility

/** Owns child references only; the caller retains ownership of the root. */
internal object CompleteTreeWalk {
    fun <N, S> visit(root: N, initial: S, childCount: (N) -> Int, child: (N, Int) -> N?,
                    release: (N) -> Unit, read: (N, S) -> S, maxNodes: Int = 600, maxDepth: Int = 50,
                    isCurrent: () -> Boolean = { true }) {
        var visited = 0
        fun walk(node: N, inherited: S, depth: Int) {
            check(isCurrent() && visited++ < maxNodes && depth <= maxDepth) { "Incomplete observation" }
            val next = read(node, inherited)
            check(isCurrent()) { "Expired observation" }
            for (index in 0 until childCount(node)) {
                check(isCurrent() && visited < maxNodes) { "Incomplete observation" }
                val item = checkNotNull(child(node, index)) { "Missing observation branch" }
                try { walk(item, next, depth + 1) } finally { release(item) }
            }
        }
        walk(root, initial, 0)
    }
}
