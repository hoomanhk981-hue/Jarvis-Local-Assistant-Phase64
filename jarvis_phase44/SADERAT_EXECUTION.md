# Bank Saderat — Real Android Execution Boundary

The Saderat-specific bridge is now wired to the SAPP Android application
package `ir.stsepehr.hamrahcard`.

Flow:

1. Jarvis validates the transfer.
2. Jarvis shows the final review.
3. The user explicitly approves.
4. Jarvis launches SAPP.
5. Authentication, OTP and the final payment confirmation remain inside the
   official SAPP UI.
6. Jarvis can observe the result through the existing accessibility/evidence
   layer after the user completes the operation.

No bank credentials, CVV2 or OTP is persisted by the bridge.

This is the correct executable boundary without pretending that SAPP exposes
a public payment API to third-party apps.
