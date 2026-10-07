package com.saathi.core

import java.io.ByteArrayOutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URL

/** One bounded exchange. A POST is never replayed after connection establishment. */
internal class GatewayHttpTransport(
    private val open: (String) -> HttpURLConnection = { URL(it).openConnection() as HttpURLConnection },
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000 }
) {
    data class Reply(val status: Int, val body: String)

    fun post(origins: List<String>, path: String, token: String, body: ByteArray, timeoutMs: Int,
             current: () -> Boolean, active: (HttpURLConnection?) -> Unit,
             connected: (String) -> Unit): Reply {
        require(origins.isNotEmpty() && body.size <= 8192)
        val deadline = nowMs() + timeoutMs
        fun remaining(): Int {
            if (!current()) throw java.io.InterruptedIOException("Cancelled")
            val left = deadline - nowMs()
            if (left <= 0) throw SocketTimeoutException("Gateway deadline")
            return left.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }
        for ((index, origin) in origins.withIndex()) {
            remaining()
            val connection = open(origin + path)
            active(connection)
            try {
                connection.apply {
                    requestMethod = "POST"; doOutput = true; useCaches = false; instanceFollowRedirects = false
                    connectTimeout = minOf(2500, remaining()); readTimeout = remaining()
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json")
                    setFixedLengthStreamingMode(body.size)
                }
                try {
                    connection.connect()
                } catch (error: java.io.IOException) {
                    // Only emulator loopback discovery, before any request body is written.
                    val refused = error is ConnectException || error is NoRouteToHostException || error is SocketTimeoutException
                    if (refused && index < origins.lastIndex && current()) continue
                    throw error
                }
                remaining()
                connected(origin)
                connection.outputStream.use { it.write(body) }
                connection.readTimeout = remaining()
                val status = connection.responseCode
                if (status !in 100..599) throw java.io.EOFException("No HTTP response")
                if (status != 200) return Reply(status, "")
                require(connection.contentType?.substringBefore(';')?.trim()?.equals("application/json", true) == true)
                val data = connection.inputStream.use { input ->
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(1024)
                    while (true) {
                        connection.readTimeout = remaining()
                        val size = input.read(buffer)
                        remaining()
                        if (size < 0) break
                        require(out.size() + size <= 8192)
                        out.write(buffer, 0, size)
                    }
                    out.toString("UTF-8")
                }
                return Reply(status, data)
            } finally {
                connection.disconnect()
                active(null)
            }
        }
        throw ConnectException("No gateway connection")
    }
}
