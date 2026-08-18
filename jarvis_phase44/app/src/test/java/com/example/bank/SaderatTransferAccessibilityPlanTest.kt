package com.example.bank

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaderatTransferAccessibilityPlanTest {
    private val plan = SaderatTransferAccessibilityPlan()

    @Test fun paymentAndOtpRequireConfirmation() {
        assertTrue(plan.actionFor(SaderatTransferAccessibilityPlan.Step.PAYMENT).requiresUserConfirmation)
        assertTrue(plan.actionFor(SaderatTransferAccessibilityPlan.Step.OTP).requiresUserConfirmation)
    }

    @Test fun workflowOrderIsDeterministic() {
        var step = SaderatTransferAccessibilityPlan.Step.OPEN_TRANSFER
        val seen = mutableListOf(step)
        while (true) {
            val next = plan.next(step) ?: break
            seen += next
            step = next
        }
        assertEquals(
            listOf(
                SaderatTransferAccessibilityPlan.Step.OPEN_TRANSFER,
                SaderatTransferAccessibilityPlan.Step.SOURCE,
                SaderatTransferAccessibilityPlan.Step.DESTINATION,
                SaderatTransferAccessibilityPlan.Step.AMOUNT,
                SaderatTransferAccessibilityPlan.Step.REVIEW,
                SaderatTransferAccessibilityPlan.Step.PAYMENT,
                SaderatTransferAccessibilityPlan.Step.OTP,
                SaderatTransferAccessibilityPlan.Step.RESULT
            ), seen
        )
    }
}
