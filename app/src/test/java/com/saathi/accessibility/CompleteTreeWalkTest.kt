package com.saathi.accessibility

import org.junit.Assert.*
import org.junit.Test

class CompleteTreeWalkTest {
    private class Node(val children: List<Node?> = emptyList()) { var releases = 0 }
    private fun walk(root: Node, max: Int = 600, depth: Int = 50, read: (Node) -> Unit = {}) =
        CompleteTreeWalk.visit(root, Unit, { it.children.size }, { n, i -> n.children[i] },
            { it.releases++ }, { n, _ -> read(n) }, max, depth)

    @Test fun truncatedTreeMustNotBeAcceptedAsSafe() {
        val root = Node(listOf(Node(), Node()))
        assertThrows(IllegalStateException::class.java) { walk(root, max = 2) }
        assertEquals(0, root.releases)
        assertEquals(1, root.children[0]!!.releases)
    }
    @Test fun missingChildAndDeepSubtreeMustNotBeSilentlySkipped() {
        assertThrows(IllegalStateException::class.java) { walk(Node(listOf(null))) }
        val grandchild = Node(); val child = Node(listOf(grandchild)); val root = Node(listOf(child))
        assertThrows(IllegalStateException::class.java) { walk(root, depth = 1) }
        assertEquals(1, child.releases); assertEquals(1, grandchild.releases)
    }
    @Test fun exactLimitIsCompleteAndAllChildReferencesReleasedOnce() {
        val leaves = List(599) { Node() }; val root = Node(leaves)
        var reads = 0; walk(root) { reads++ }
        assertEquals(600, reads); assertEquals(0, root.releases)
        assertTrue(leaves.all { it.releases == 1 })
    }
    @Test fun failedReadReleasesEveryAcquiredReferenceWithoutReadingNextSibling() {
        val bad = Node(); val child = Node(listOf(bad)); val untouched = Node()
        val root = Node(listOf(child, untouched))
        assertThrows(IllegalStateException::class.java) { walk(root) { if (it === bad) error("synthetic failure") } }
        assertEquals(1, child.releases); assertEquals(1, bad.releases); assertEquals(0, untouched.releases)
    }
    @Test fun cancellationDuringReadDiscardsPartialResultAndReleasesChildren() {
        val child = Node(); val root = Node(listOf(child)); var current = true
        assertThrows(IllegalStateException::class.java) {
            CompleteTreeWalk.visit(root, Unit, { it.children.size }, { n, index -> n.children[index] },
                { it.releases++ }, { node, _ -> if (node === child) current = false }, isCurrent = { current })
        }
        assertEquals(1, child.releases); assertEquals(0, root.releases)
    }
    @Test fun repeatedMixedFailuresNeverLeakAcquiredChildOwnership() {
        repeat(10000) { iteration ->
            val leaves = List(4) { Node() }; val root = Node(leaves)
            var acquired = 0; var released = 0
            runCatching {
                CompleteTreeWalk.visit(root, Unit, { it.children.size }, { n, i -> acquired++; n.children[i] },
                    { released++; it.releases++ }, { n, _ -> if (iteration % 2 == 0 && n === leaves[2]) error("synthetic failure") })
            }
            assertEquals(acquired, released)
            assertTrue(leaves.all { it.releases <= 1 })
        }
    }

}
