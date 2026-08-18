package com.example.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditRedactorTest {
    @Test fun redactsSensitiveValues() {
        val out = AuditRedactor.redact("card 6037 9912 3456 7890 otp: 123456 cvv2: 321 password: secret")
        assertTrue(out.contains("[CARD_PROTECTED]"))
        assertTrue(out.contains("[OTP_PROTECTED]"))
        assertTrue(out.contains("[CVV_PROTECTED]"))
        assertTrue(out.contains("[PASSWORD_PROTECTED]"))
        assertFalse(out.contains("123456"))
        assertFalse(out.contains("secret"))
    }
}
