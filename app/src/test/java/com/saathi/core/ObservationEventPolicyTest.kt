package com.saathi.core
import org.junit.Assert.*
import org.junit.Test
class ObservationEventPolicyTest {
    @Test fun lateContentFromOldWindowDoesNotInvalidateCurrentRoot() {
        for (type in listOf(1,16,2048,4096)) assertFalse(ObservationEventPolicy.relevant(type,589,600))
    }
    @Test fun currentContentUnknownRootAndAllWindowTransitionsStillInvalidate() {
        for (type in listOf(1,16,2048,4096)) {
            assertTrue(ObservationEventPolicy.relevant(type,600,600))
            assertTrue(ObservationEventPolicy.relevant(type,589,null))
            assertTrue(ObservationEventPolicy.relevant(type,-1,600))
        }
        for (type in listOf(32,4194304)) assertTrue(ObservationEventPolicy.relevant(type,589,600))
    }
}
