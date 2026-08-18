# SMS Tool — Implementation Status

Implemented:
- Android READ_SMS permission gate.
- Android SEND_SMS permission gate.
- Reading recent SMS messages through the Android SMS provider.
- Bounded result size.
- Sending SMS through Android SmsManager.
- No external SMS API is required.

Important Android boundary:
The app must declare/request the relevant Android permissions and comply with
Android/Play distribution restrictions for SMS access. Runtime testing on the
target Android version is required before calling this feature 100% complete.

Current status: 90%.
