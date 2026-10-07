package com.saathi.accessibility

import org.junit.Assert.*
import org.junit.Test

class SensitiveContentTest {
    @Test fun `format characters full width cues and separated secrets cannot bypass classification`() {
        listOf("pass\u200bword fictional", "ｐａｓｓｗｏｒｄ fictional", "5\u200b8\u200b2\u200b1\u200b3\u200b9", "5 8 2 1 3 9", "५ ८ २ १ ३ ९", "58-21-39").forEach {
            assertTrue(it, SensitiveContent.isSensitive(false, it))
        }
    }
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
    @Test fun `travel dates fares and destinations remain public`() {
        listOf("From", "To", "Destination", "01/10/2026", "2026-10-01", "Cheapest from ₹5221", "₹6,398", "INR 12345.50", "Rs. 1234", "10:20 AM", "DEL to SXR").forEach {
            assertFalse(it, SensitiveContent.isSensitive(false, it))
        }
    }
    @Test fun `public display does not override secret field metadata or adjacent secrets`() {
        assertTrue(SensitiveContent.isSensitive(false, "01/10/2026", "dateOfBirth"))
        assertTrue(SensitiveContent.isSensitive(false, "₹5221", "accountNumber"))
        assertTrue(SensitiveContent.isSensitive(true, "₹5221"))
        assertTrue(SensitiveContent.isSensitive(false, "01/10/2026", "enterOtp"))
        assertTrue(SensitiveContent.isSensitive(false, "PIN ₹5221"))
        assertTrue(SensitiveContent.isSensitive(false, "₹5221 code 582139"))
        assertTrue(SensitiveContent.isSensitive(false, "https://example.com/01/10/2026"))
        assertTrue(SensitiveContent.isSensitive(false, "₹1234567890123456"))
    }
}
