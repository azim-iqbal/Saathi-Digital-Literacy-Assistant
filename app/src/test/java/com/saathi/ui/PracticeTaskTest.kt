package com.saathi.ui

import org.junit.Assert.*
import org.junit.Test

class PracticeTaskTest {
    @Test fun `all three languages route to synthetic tasks`() {
        listOf("electricity bill", "बिजली का बिल", "bijli bill").forEach { assertEquals("Pay my electricity bill", PracticeTask.parse(it)) }
        listOf("water bill", "पानी का बिल", "paani bill").forEach { assertEquals("Pay my water bill", PracticeTask.parse(it)) }
    }
    @Test fun `unknown task cannot silently become bill practice`() { assertNull(PracticeTask.parse("book a train")); assertNull(PracticeTask.parse("")) }
}
