package com.example.bank
import org.junit.Assert.*
import org.junit.Test
class BankTransferWorkflowTest {
 private val adapter=SaderatTestAdapter()
 @Test fun invalidDestinationIsRejected(){
  val r=BankTransferWorkflow(adapter).validate(TransferRequest("1234",100_000)); assertFalse(r.ok); assertTrue(r.errors.any{it.contains("شماره کارت مقصد")})
 }
 @Test fun validRequestAwaitsConfirmation(){
  val q=TransferRequest("6037691234567890",100_000,"6037699876543210",12,30,"123")
  val s=BankTransactionStateMachine().validate(BankTransactionStateMachine.Session(q),adapter)
  assertEquals(BankTransactionStateMachine.State.AWAITING_CONFIRMATION,s.state)
 }
}
