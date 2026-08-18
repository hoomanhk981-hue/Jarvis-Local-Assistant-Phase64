package com.example.bank

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BankWorkflowSecurityTest {
    @Test fun verifierNeverTreatsUnknownAsSuccess() {
        assertEquals(
            BankTransferResultVerifier.Result.UNKNOWN,
            BankTransferResultVerifier.verify(
                BankTransferResultVerifier.Observation("پرداخت در حال بررسی")
            )
        )
    }

    @Test fun explicitFailureWins() {
        assertEquals(
            BankTransferResultVerifier.Result.FAILURE,
            BankTransferResultVerifier.verify(
                BankTransferResultVerifier.Observation("موفق", reference = "123", explicitSuccess = true, explicitFailure = true)
            )
        )
    }

    @Test fun otpDetectorIsCandidateBased() {
        val candidates = BankOtpDetector.detect("بانک: رمز پویا ۱۲۳۴۵۶")
        assertTrue(candidates.any { it.code == "123456" })
    }
}
