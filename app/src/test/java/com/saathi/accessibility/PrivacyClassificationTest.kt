package com.saathi.accessibility

import org.junit.Assert.*
import org.junit.Test

class PrivacyClassificationTest {
    @Test fun readonlyMoneyAndConsequentialActionAreNotCredentials() {
        assertEquals(PrivacyKind.PUBLIC_DISPLAY,PrivacyClassification.classify(false,false,0,null,"product_123456",false,false,true))
        assertEquals(PrivacyKind.PUBLIC_ACTION,PrivacyClassification.classify(false,false,0,"Pay ₹500",null,false,true,true))
        assertEquals(PrivacyKind.USER_INPUT_NORMAL,PrivacyClassification.classify(false,true,1,"Destination",null,false,true,false))
        assertEquals(PrivacyKind.USER_INPUT_PERSONAL,PrivacyClassification.classify(false,true,1,"Email address",null,false,true,false))
    }
    @Test fun strongCredentialEvidenceWinsButUnknownRedactionIsNotAnEditableField() {
        assertEquals(PrivacyKind.SENSITIVE_PAYMENT,PrivacyClassification.classify(false,true,1,"Enter CVC",null,true,true,false))
        assertEquals(PrivacyKind.SENSITIVE_AUTH,PrivacyClassification.classify(true,true,1,"Search",null,true,true,false))
        assertEquals(PrivacyKind.SENSITIVE_AUTH,PrivacyClassification.classify(false,true,0x81,"Search",null,true,true,false))
        assertEquals(PrivacyKind.UNKNOWN_SENSITIVE,PrivacyClassification.classify(false,false,0,null,null,true,false,false))
        assertEquals(PrivacyKind.NON_INTERACTIVE_DECORATION,PrivacyClassification.classify(false,false,0,null,null,false,false,false))
    }
}
