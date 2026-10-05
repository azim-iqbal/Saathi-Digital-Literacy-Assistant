package com.saathi.core

import java.net.URI

/** A build-selected origin, never a link supplied by a model, web page or intent. */
object GatewayEndpoint {
    fun resolve(configured: String, debug: Boolean): String? {
        if (configured.isEmpty()) return if (debug) "http://127.0.0.1:8765" else null
        return runCatching {
            val uri = URI(configured)
            require(uri.scheme == "https" && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null)
            require(uri.rawPath.isNullOrEmpty() || uri.rawPath == "/")
            require(uri.port == -1 || uri.port in 1..65535)
            val host = requireNotNull(uri.host).lowercase()
            require(host.length <= 253 && host.split('.').size >= 2)
            require(host.split('.').all { it.matches(Regex("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")) })
            require(!host.all { it.isDigit() || it == '.' } && !host.endsWith(".localhost") && !host.endsWith(".local"))
            "https://$host" + if (uri.port == -1) "" else ":${uri.port}"
        }.getOrNull()
    }
}
