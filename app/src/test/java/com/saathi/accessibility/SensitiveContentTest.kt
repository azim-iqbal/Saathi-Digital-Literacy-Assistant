package com.saathi.accessibility

import org.junit.Assert.*
import org.junit.Test

class SensitiveContentTest {
    @Test fun `network ports do not hide public pages but URL secrets remain private`() {
        listOf("127.0.0.1:8766/browser.html", "https://example.com:8443/help", "localhost:12345").forEach {
            assertFalse(it, SensitiveContent.isSensitive(false, it))
        }
        listOf("https://example.com:8443/?code=123456", "https://example.com:8443/123456", "123456", "OTP at localhost:1234", "https://user:1234@example.com:8766", "example.com:99999").forEach {
            assertTrue(it, SensitiveContent.isSensitive(false, it))
        }
    }
    @Test fun `password flag masks unlabelled values`() { assertTrue(SensitiveContent.isSensitive(true, null)) }
    @Test fun `English Hindi and camel case identifiers are sensitive`() {
        listOf("OTP code", "पासवर्ड", "ओटीपी", "पिन", "cvv_input", "com.example:id/enterPin", "security code", "recovery-code").forEach {
            assertTrue(it, SensitiveContent.isSensitive(false, it))
        }
    }
    @Test fun `notification and ordinary text containing numeric secrets are withheld`() {
        listOf("Your code is 582139", "५८२१३९", "582139", "1234567890123456").forEach {
            assertTrue(it, SensitiveContent.isSensitive(false, it))
        }
    }
    @Test fun `shipping shopping and spinner are not pin matches`() {
        listOf("shipping", "shopping", "spinner", "Continue", "बिजली").forEach {
            assertFalse(it, SensitiveContent.isSensitive(false, it))
        }
    }
}
