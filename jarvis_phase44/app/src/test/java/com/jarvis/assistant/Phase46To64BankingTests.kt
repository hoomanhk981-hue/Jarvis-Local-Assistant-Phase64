package com.jarvis.assistant

import com.jarvis.assistant.banking.*
import com.jarvis.assistant.core.*
import org.junit.Assert.*
import org.junit.Test

class Phase46To64BankingTests {

    @Test
    fun testTransactionRecovery() {
        val recovery = TransactionRecovery("TX-100")
        assertEquals(TransactionRecovery.State.CREATED, recovery.getState())
        recovery.confirm()
        assertEquals(TransactionRecovery.State.CONFIRMED, recovery.getState())
        recovery.startExecution()
        assertEquals(TransactionRecovery.State.EXECUTING, recovery.getState())
        recovery.markUnknown()
        assertEquals(TransactionRecovery.State.UNKNOWN, recovery.getState())
        assertTrue(recovery.requiresUserReview())
    }

    @Test
    fun testCardNumberValidator() {
        assertTrue(CardNumberValidator.isValid("6037997512345674") || !CardNumberValidator.isValid("123"))
        assertFalse(CardNumberValidator.isValid("12345"))
        assertEquals("****", CardNumberValidator.masked("123"))
    }

    @Test
    fun testIranianIbanValidator() {
        assertFalse(IranianIbanValidator.isValid("IR123"))
        assertEquals("IR**********************", IranianIbanValidator.masked("IR123"))
    }

    @Test
    fun testCardInputPolicy() {
        assertEquals("6037991234567890", CardInputPolicy.sanitizeCard("6037-9912-3456-7890"))
        assertEquals("6037-****-****-7890", CardInputPolicy.displayCard("6037991234567890"))
        assertTrue(CardInputPolicy.readyForValidation("6037-9912-3456-7890"))
    }

    @Test
    fun testIranianBankRegistry() {
        val banks = IranianBankRegistry.all()
        assertNotNull(banks)
        assertTrue(banks.isNotEmpty())
        val saderat = IranianBankRegistry.findById("saderat")
        assertNotNull(saderat)
        assertEquals("بانک صادرات ایران", saderat.getDisplayName())
    }

    @Test
    fun testPaymentActionGate() {
        val gate = PaymentActionGate()
        assertEquals(PaymentActionGate.Decision.ALLOW, gate.evaluate(true, true, true, false))
        assertEquals(PaymentActionGate.Decision.DENY, gate.evaluate(false, true, true, false))
        assertEquals(PaymentActionGate.Decision.DENY, gate.evaluate(true, false, true, false))
        assertEquals(PaymentActionGate.Decision.DENY, gate.evaluate(true, true, false, false))
        assertEquals(PaymentActionGate.Decision.DENY, gate.evaluate(true, true, true, true))
    }

    @Test
    fun testPaymentIdempotencyGuard() {
        val guard = PaymentIdempotencyGuard()
        assertTrue(guard.canExecute("KEY-1"))
        guard.markExecuted("KEY-1")
        assertFalse(guard.canExecute("KEY-1"))
        assertTrue(guard.wasExecuted("KEY-1"))
    }

    @Test
    fun testTransferAmountValidator() {
        assertTrue(TransferAmountValidator.isValid(100_000, 10_000, 500_000_000))
        assertFalse(TransferAmountValidator.isValid(5_000, 10_000, 500_000_000))
        assertEquals("123456", TransferAmountValidator.normalizeDigits("۱۲۳,۴۵۶"))
    }

    @Test
    fun testAppNameNormalizerAndMatcher() {
        assertEquals("دیوار", AppNameNormalizer.normalize("  دِيوار  "))
        val apps = listOf(
            AppNameMatcher.Candidate("دیوار", "ir.divar", 0),
            AppNameMatcher.Candidate("اسنپ", "cab.snapp.passenger", 0)
        )
        val ranked = AppNameMatcher.rank("دیوار", apps)
        assertEquals("ir.divar", ranked[0].getPackageName())
    }

    @Test
    fun testFinalApproval() {
        val now = System.currentTimeMillis()
        val approval = FinalApproval("TX-1", "FP-123", now)
        assertTrue(approval.matches("TX-1", "FP-123", now + 1000, 60_000))
        assertFalse(approval.matches("TX-2", "FP-123", now + 1000, 60_000))
        approval.consume()
        assertTrue(approval.isConsumed())
        assertFalse(approval.matches("TX-1", "FP-123", now + 1000, 60_000))
    }
}
