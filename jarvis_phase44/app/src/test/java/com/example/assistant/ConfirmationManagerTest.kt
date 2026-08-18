package com.example.assistant
import org.junit.Assert.*
import org.junit.Test
class ConfirmationManagerTest {
 @Test fun approvalIsSingleUse(){
  val m=ConfirmationManager(60_000); val r=m.create("send_sms",mapOf("to" to "0912"),"send",ConfirmationManager.Risk.MEDIUM)
  assertNotNull(m.approve(r.id)); assertNull(m.approve(r.id))
 }
 @Test fun dangerousTermuxCommandIsHighRisk(){
  assertEquals(ConfirmationManager.Risk.HIGH, ConfirmationManager.riskForTool("run_termux",mapOf("command" to "rm -rf /tmp/x")))
 }
}
