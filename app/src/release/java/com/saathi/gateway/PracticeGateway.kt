package com.saathi.gateway

import android.content.Context
import com.saathi.core.*

/** Local development transport and configuration are absent from release builds. */
object PracticeGateway {
    fun enabled() = false
    fun aiEnabled() = false
    fun requestLive(snapshot: LiveAiSnapshot, callback: (GatewayResult) -> Unit): GatewayCancellation {
        callback(GatewayResult.Rejected("unavailable")); return GatewayCancellation { }
    }
    fun assessIncident(snapshot: IncidentAssessmentRequest, consent: Boolean, callback: (GatewayResult) -> Unit): GatewayCancellation {
        callback(GatewayResult.Rejected("not_configured")); return GatewayCancellation { }
    }
    fun openSetup(context: Context) = Unit
    fun disable() = Unit
    fun request(snapshot: SanitizedScreenSnapshot, callback: (GatewayResult) -> Unit): GatewayCancellation {
        callback(GatewayResult.Rejected("unavailable"))
        return GatewayCancellation { }
    }
}
