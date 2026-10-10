package com.saathi.accessibility

import org.junit.Assert.*
import org.junit.Test

class NodeContentPolicyTest {
    @Test fun publicProductWeightRangesAreNotSecretNumbers() {
        for(label in listOf("450 - 500 g", "200 - 300 g", "500-1000 ml", "Product, 450 - 500 g, ₹47")) {
            val result=NodeContentPolicy.read(false,false,null,null,0,false,{label},{null})
            assertFalse(label,result.sensitive)
        }
        assertTrue(SensitiveContent.isSensitive(false,"OTP 450 - 500 g"))
        assertTrue(SensitiveContent.isSensitive(false,"1234-5678"))
    }

    @Test fun publicCommerceIdentifiersAreNotEnteredSecrets() {
        for(label in listOf("₹47","₹36","₹30","₹१२४९","€١٢٤٩","₹16 platform fee","₹1,249 hotel rate","₹325 cab fare","₹250 ticket","₹149 burger","₹40 delivery fee","20% discount","₹895 order total","ADD","Checkout ₹650","Pay ₹500")) {
            val result=NodeContentPolicy.read(false,false,null,"shop:id/product_123456",0,false,{label},{null})
            assertFalse(label,result.sensitive);assertEquals(label,result.text)
        }
        val normal=NodeContentPolicy.read(false,true,"Destination","app:id/field_123456",1,true,{error("Value read")},{error("Description read")})
        assertFalse(normal.sensitive)
        val private=NodeContentPolicy.read(false,true,null,"app:id/enterOtp_123456",1,false,{error("Value read")},{error("Description read")})
        assertTrue(private.sensitive)
    }
    @Test fun credentialCorpusIsPrivateWithoutAnyEnteredValueRead() {
        for(label in listOf("Enter CVV", "CVC", "TOTP", "UPI PIN", "OTP", "Card number", "Password", "MPIN", "Security code", "Bank password", "Payment authentication OTP", "Card expiry", "Security answer", "Authentication token", "O.T.P.", "पासवर्ड", "सीवीवी", "ＣＶＣ")) {
            val result=NodeContentPolicy.read(false,true,label,null,1,false,{error("Value read")},{error("Description read")})
            assertTrue(label,result.sensitive);assertTrue(label,result.structuralPrivateField)
        }
    }
    @Test fun privateMetadataNeverCallsValueOrDescriptionGetters() {
        for ((password, hint, id, type) in listOf(
            listOf(true, null, null, 0), listOf(false, "OTP", null, 0),
            listOf(false, null, "field/password", 0), listOf(false, "पिन", null, 0),
            listOf(false, "ＣＶＶ", null, 0), listOf(false, null, null, 0x81),
            listOf(false, null, null, 0x91), listOf(false, null, null, 0xe1),
            listOf(false, null, null, 0x12))) {
            val result = NodeContentPolicy.read(password as Boolean, true, hint as String?, id as String?, type as Int, false,
                { error("Private text getter accessed") }, { error("Private description getter accessed") })
            assertTrue(result.sensitive); assertTrue(result.structuralPrivateField)
            assertFalse(result.hasValue); assertFalse(result.valueKnown)
            assertNull(result.text); assertNull(result.description)
        }
    }
    @Test fun unlabelledEditorsNeverReadValuesAndAbsenceOfHintIsUnknown() {
        for (showing in listOf(false, true)) {
            val result = NodeContentPolicy.read(false, true, "Destination", null, 1, showing,
                { error("Editable value getter accessed") }, { error("Editable description getter accessed") })
            assertFalse(result.hasValue); assertEquals(showing, result.valueKnown)
            assertNull(result.text); assertNull(result.description)
        }
    }
    @Test fun cursorMetadataShowsPresenceWithoutReadingValuesOrCertifyingValidity() {
        for(position in listOf(-1,0,4)) {
            val result=NodeContentPolicy.read(false,true,"City *",null,1,false,
                { error("No value access") },{ error("No description access") },position)
            assertEquals(position>0,result.hasValue);assertEquals(position>0,result.valueKnown)
            assertNull(result.text)
        }
        val cleared=NodeContentPolicy.read(false,true,"City *",null,1,true,{ error("No value") },{ null },0)
        assertTrue(cleared.valueKnown);assertFalse(cleared.hasValue)
        val secret=NodeContentPolicy.read(true,true,"OTP",null,0,false,{ error("No value") },{ error("No description") },8)
        assertFalse(secret.valueKnown);assertFalse(secret.hasValue)
    }
    @Test fun staticSecretsStillRedactedWhilePublicDatesAndPricesSurvive() {
        for (value in listOf("OTP 582139", "card number 4111 1111 1111 1111", "password synthetic-secret", "ＣＶＶ 123", "pass\u200Bword fictional")) {
            val result = NodeContentPolicy.read(false, false, null, null, 0, false, { value }, { null })
            assertTrue(result.sensitive); assertFalse(result.structuralPrivateField)
            assertFalse(result.hasValue); assertNull(result.text)
        }
        for (value in listOf("To", "01/10/2026", "₹5,221")) {
            val result = NodeContentPolicy.read(false, false, null, null, 0, false, { value }, { null })
            assertFalse(result.sensitive); assertEquals(value, result.text)
        }
    }
}
