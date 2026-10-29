FG Chat Android early alpha: local identity, Keystore encrypted account/contact state, public Tox bootstrap/relays, encrypted text while both apps are open, QR/link contact invitations, approval/blocking, Arabic/English UI.

Install FG-Chat-debug.apk on a disposable Android test device (Android 8+; arm64). Temporarily allow installation from the app used to open the APK. Create identities on both phones, exchange complete invitations through a trusted channel, accept requests, compare full keys, wait for Available and open a conversation. Both apps must remain visible. No phone/email registration or FG-operated endpoint is needed. Public nodes may fail or be blocked.

Validation gate: Kotlin unit/native loopback tests, Android debug build/lint, runtime Maven dependency scan, and API35 emulator instrumentation completed in the linked CI run. These gates do not prove physical-device, cellular/CGNAT, battery, anonymity or independent security audit. Consult the exact run's test reports for actual coverage.

Development-signed APK, not a production release. Build-to-build debug signing keys can differ; installing a new build may require removing the previous test installation. Do not store a valuable identity in this alpha: there is no migration/backup/recovery yet, and uninstalling loses it. Messages are memory-only and disappear on process death. Screenshots are enabled for this debug build's test evidence.

No calls/media/files, multi-device, account transfer, rotating/one-time short invitations, signed mutual deletion, Tor, background service/notifications or Windows app. Direct peers may see IP; network timing metadata remains. Tox is experimental and has not had a complete independent security audit. “Transport received” is not a human read receipt. In-flight packets may arrive during undetected short interruptions; no app outbox/mailbox/automatic retry exists.

The attached public-network-probe.log records a separate best-effort experiment using public bootstrap/relay nodes. The two peers run on one host; success would not prove physical-phone or cellular/CGNAT compatibility. A failed experiment is retained, not counted as successful verification.

GPL-3.0-or-later. FG Chat corresponding source is the release tag's source archive; exact native source archives accompany the APK. Build instructions and third-party notices are in the repository.
