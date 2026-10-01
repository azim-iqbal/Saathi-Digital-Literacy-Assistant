package com.saathi.ui

import android.accessibilityservice.AccessibilityServiceInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saathi.DemoBillPayActivity
import com.saathi.R
import com.saathi.accessibility.NodeMasker
import com.saathi.orchestrator.DemoGuide
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PracticeRecoveryTest {
    private fun step(goal: String): com.saathi.core.GuideStep {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val root = requireNotNull(automation.rootInActiveWindow)
        return try { DemoGuide.next(goal, NodeMasker.flatten(root), "en-IN", false) }
        finally { @Suppress("DEPRECATION") root.recycle() }
    }

    @Test fun wrongBillerGuidesBackThenToChosenBiller() {
        ActivityScenario.launch(DemoBillPayActivity::class.java).use {
            onView(withId(R.id.recharge_bills)).perform(click())
            onView(withId(R.id.electricity_biller)).perform(click())
            assertEquals("practice_back", step("Pay my water bill").target?.description)
            onView(withId(R.id.practice_back)).perform(click())
            assertEquals("water_biller", step("Pay my water bill").target?.description)
            onView(withId(R.id.water_biller)).perform(click())
            onView(withId(R.id.practice_water)).check(matches(isDisplayed()))
        }
    }

    @Test fun unrelatedTileHasVisibleRecoveryAndNoInventedTarget() {
        ActivityScenario.launch(DemoBillPayActivity::class.java).use {
            onView(withId(R.id.scan_qr)).perform(click())
            assertEquals("practice_back", step("Recharge my DTH").target?.description)
            onView(withId(R.id.practice_back)).perform(click())
            onView(withId(R.id.recharge_bills)).check(matches(isDisplayed()))
        }
    }

    @Test fun formRecreationKeepsCategoryButDiscardsPrivateValues() {
        ActivityScenario.launch(DemoBillPayActivity::class.java).use { scenario ->
            onView(withId(R.id.recharge_bills)).perform(click())
            onView(withId(R.id.water_biller)).perform(click())
            onView(withId(R.id.account_input)).perform(scrollTo(), replaceText("123456"), closeSoftKeyboard())
            onView(withId(R.id.pin_input)).perform(scrollTo(), replaceText("1111"), closeSoftKeyboard())
            scenario.recreate()
            onView(withId(R.id.practice_water)).check(matches(isDisplayed()))
            onView(withId(R.id.account_input)).check(matches(withText("")))
            onView(withId(R.id.pin_input)).check(matches(withText("")))
        }
    }
}
