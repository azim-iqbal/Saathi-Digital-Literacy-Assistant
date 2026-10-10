package com.saathi.core

import org.junit.Assert.*
import org.junit.Test

class ReactiveFormTest {
    private fun field(id: String, hint: String = "Field *", presence: FormPresence = FormPresence.EMPTY,
                      focused: Boolean = false, invalid: Boolean = false, enabled: Boolean = true) =
        FormField(id, hint, presence, focused, invalid, enabled)
    @Test fun invalidRequiredWinsAndFilledIsNotCertifiedValid() {
        val form = ReactiveForm.assess(listOf(field("a", presence=FormPresence.PRESENT), field("b",invalid=true)))
        assertEquals("b", form.next?.id)
        assertEquals(FormState.VALUE_PRESENT,form.states["a"])
        assertFalse(form.completed)
    }
    @Test fun freshClearingAndConditionalRebuildNeverReuseCompletion() {
        val a=field("a",presence=FormPresence.PRESENT)
        assertEquals("b",ReactiveForm.assess(listOf(a,field("b"))).next?.id)
        assertEquals("a",ReactiveForm.assess(listOf(a.copy(presence=FormPresence.EMPTY),field("b"))).next?.id)
        assertEquals("a",ReactiveForm.assess(listOf(a.copy(presence=FormPresence.EMPTY))).next?.id)
        assertNull(ReactiveForm.assess(listOf(a)).next)
        assertFalse(ReactiveForm.assess(listOf(a)).completed)
    }
    @Test fun requirementsStayExplicitInAllLocales() {
        for(label in listOf("City *","शहर *","Shehar *")) assertEquals(FormRequiredness.REQUIRED,ReactiveForm.requiredness(label))
        for(label in listOf("Company (optional)","संस्था (वैकल्पिक)","Company (vaikalpik)")) assertEquals(FormRequiredness.OPTIONAL,ReactiveForm.requiredness(label))
        for(label in listOf("Required if selected","चुने जाने पर आवश्यक","Chune jaane par zaroori")) assertEquals(FormRequiredness.CONDITIONAL,ReactiveForm.requiredness(label))
        assertEquals(FormRequiredness.UNKNOWN,ReactiveForm.requiredness("City"))
        assertEquals(FormRequiredness.UNKNOWN,ReactiveForm.requiredness("Company optional *"))
    }
    @Test fun optionalDoesNotBlockRequiredAndUnknownIsNotOptional() {
        val optional=field("a","Company (optional)")
        assertEquals("b",ReactiveForm.assess(listOf(optional,field("b"))).next?.id)
        assertNull(ReactiveForm.assess(listOf(optional)).next)
        assertEquals(FormState.OPTIONAL_SKIPPED,ReactiveForm.assess(listOf(optional)).states["a"])
        assertEquals("a",ReactiveForm.assess(listOf(field("a","City"))).next?.id)
    }
    @Test fun explicitNegationAndConflictingMetadataNeverInventRequirement() {
        for (label in listOf("Company (not required)", "संस्था (आवश्यक नहीं)", "Company (zaroori nahi)")) {
            assertEquals(FormRequiredness.OPTIONAL, ReactiveForm.requiredness(label))
            assertEquals(FormRequiredness.UNKNOWN, ReactiveForm.requiredness(label, true))
        }
        assertEquals(FormRequiredness.UNKNOWN, ReactiveForm.requiredness("Company (not optional)"))
        assertEquals(FormRequiredness.UNKNOWN, ReactiveForm.requiredness("Company (not required) *"))
    }
    @Test fun invalidRequiredPrecedesInvalidOptionalButOptionalErrorIsNotHidden() {
        val optional = field("a", "Company (optional)", invalid=true)
        val required = field("b", invalid=true)
        assertEquals("b", ReactiveForm.assess(listOf(optional, required)).next?.id)
        assertEquals("a", ReactiveForm.assess(listOf(optional, required.copy(invalid=false, presence=FormPresence.PRESENT))).next?.id)
    }
    @Test fun typingKeepsCurrentFieldButErrorAndDisabledDependenciesAreRespected() {
        val active=field("a",presence=FormPresence.PRESENT,focused=true)
        assertEquals("a",ReactiveForm.assess(listOf(active,field("b"))).next?.id)
        assertEquals("b",ReactiveForm.assess(listOf(active,field("b",invalid=true))).next?.id)
        assertNull(ReactiveForm.assess(listOf(field("a",enabled=false))).next)
        assertNull(ReactiveForm.assess(listOf(field("a",focused=true),field("b",focused=true))).next)
    }
}
