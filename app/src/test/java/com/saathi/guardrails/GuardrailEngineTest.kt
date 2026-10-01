package com.saathi.guardrails

import org.junit.Assert.assertEquals
import org.junit.Test

class GuardrailEngineTest {
    @Test fun `allows concrete on-screen tasks in English Hindi and Hinglish`() {
        val examples = listOf(
            "Pay my electricity bill", "Help me fill this scholarship form", "Open UPI and send money", "Book a train ticket", "Apply for a government scheme",
            "Pay my water bill", "Help with PhonePe payment", "Fill this passport application", "Book a bus ticket", "Open accessibility settings",
            "बिजली का बिल भरना है", "यह सरकारी फॉर्म भरना है", "UPI से भुगतान करना है", "ट्रेन टिकट बुक करना है", "सेटिंग में permission खोलना है",
            "bijli ka bill bharna hai", "form fill karna hai", "PhonePe se payment karna hai", "train ticket booking karni hai", "sarkari yojana apply karni hai"
        )
        examples.forEach { assertEquals("Expected allow: $it", GuardrailDecision.ALLOW, GuardrailEngine.classify(it).decision) }
    }

    @Test fun `refuses off-topic requests in English Hindi and Hinglish`() {
        val examples = listOf(
            "What is the capital of France", "Tell me a joke", "Play a song", "Write an essay", "Recommend a movie",
            "Tell me the weather", "Write a poem", "Give relationship advice", "What is Bitcoin", "Make a recipe",
            "मौसम कैसा है", "एक चुटकुला सुनाओ", "कोई गाना चलाओ", "कविता लिखो", "फिल्म बताओ",
            "joke sunao", "gaana chalao", "kahani batao", "essay likh do", "share bazaar advice do"
        )
        examples.forEach { assertEquals("Expected refuse: $it", GuardrailDecision.REFUSE, GuardrailEngine.classify(it).decision) }
    }

    @Test fun `flags unsafe screen categories`() {
        assertEquals(true, GuardrailEngine.screenIsUnsafe(sequenceOf("Play casino now")))
        assertEquals(true, GuardrailEngine.screenIsUnsafe(sequenceOf("Instant loan approved")))
        assertEquals(false, GuardrailEngine.screenIsUnsafe(sequenceOf("Electricity bill")))
    }
}
