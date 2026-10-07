package com.saathi.core
import org.junit.Assert.*
import org.junit.Test
class PlainLanguageTest {
    @Test fun glossaryHasAllLanguagesAndNeverEchoesEnteredValues() {
        for (term in PlainLanguage.terms) for (locale in listOf("en-IN","hi-IN","hinglish")) {
            assertFalse(PlainLanguage.explain(term,locale).isNullOrBlank())
        }
        assertNull(PlainLanguage.explain("OTP 123456","en-IN"))
        assertNull(PlainLanguage.explain("my account is private","en-IN"))
        assertNotNull(PlainLanguage.explain(" OTP ","hinglish"))
    }
}
