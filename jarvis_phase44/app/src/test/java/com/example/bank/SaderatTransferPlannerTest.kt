package com.example.bank

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaderatTransferPlannerTest {
    private val adapter = FakeAdapter()

    @Test
    fun creates_ordered_semantic_plan() {
        val result = SaderatTransferPlanner(adapter).buildPlan(
            TransferRequest("6037997512345678", 100_000L, "6037691234567890")
        )
        assertTrue(result.isSuccess)
        assertEquals(
            listOf("انتقال وجه", "انتقال از", "انتقال به", "مبلغ", "پرداخت", "رمز دوم پویا"),
            result.getOrThrow().map { it.label }
        )
    }

    @Test
    fun payment_step_is_sensitive() {
        val step = SaderatTransferPlanner(adapter)
            .buildPlan(TransferRequest("6037997512345678", 100_000L, "6037691234567890"))
            .getOrThrow()[4]
        assertTrue(step.sensitive)
    }

    private class FakeAdapter : BankAdapter {
        override val bankName = "Saderat Test"
        override val bankCode = "603769"
        override val packageName = "ir.bsi.mobilebank"
        override val isTestMode = true
        override fun canHandleBank(cardPrefix: String) = true
    }
}
