package com.saathi.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class ObservationDiagnosticsTest {
    @Test fun observationFailuresNeverExposeExceptionText() {
        val seen = mutableListOf<String>()
        val prior = ObservationDiagnostics.failureObserver
        try {
            ObservationDiagnostics.failureObserver = { seen.add(it) }
            ObservationDiagnostics.observationFailure("fictional private exception body")
            ObservationDiagnostics.observationFailure("Incomplete observation")
            ObservationDiagnostics.observationFailure("Expired observation")
            ObservationDiagnostics.observationFailure("Missing observation branch")
            assertEquals(listOf("Observation unavailable", "Incomplete observation", "Expired observation", "Missing observation branch"), seen)
        } finally { ObservationDiagnostics.failureObserver = prior }
    }
    @Test fun diagnosticsContainOnlyAllowlistedReasonCodes() {
        val seen = mutableListOf<String>()
        val prior = ObservationDiagnostics.gatewayObserver
        try {
            ObservationDiagnostics.gatewayObserver = { seen.add(it) }
            ObservationDiagnostics.gatewayResult("fictional private error body")
            ObservationDiagnostics.gatewayResult("accepted")
            ObservationDiagnostics.gatewayResult("invalid_request")
            assertEquals(listOf("not_verified", "accepted", "invalid_request"), seen)
        } finally { ObservationDiagnostics.gatewayObserver = prior }
    }
}
