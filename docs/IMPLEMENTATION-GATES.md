# FG Chat implementation gates

Current state: contracts/scaffolding only. No Tox binding, Tor transport, Android APK, working messaging or successful CI confirmed.

## Gate A — Tox proof of concept
- Verify c-toxcore license and upstream security caveats.
- Create two peers on separate devices; use independently operated bootstrap nodes.
- Test online-only delivery and confirm no offline mailbox.
- Measure connection behavior on mobile data, Wi-Fi, and NAT.
- Record whether peers expose IP addresses; investigate optional Tor transport separately.

## Gate B — identity and migration
- Design app-level identity and signed device authorization.
- Interactive QR pairing with verified key fingerprints.
- Encrypted device-to-device migration, rollback on failed import.
- Revoke old device; document consequences when it is lost.
- Multi-device requires separate keys and per-device delivery.

## Gate C — deletion and files
- Signed delete request and signed receipt, replay protection.
- UI distinguishes local deletion, remote pending, remote acknowledged.
- Encrypted chunked file transfers, resume and integrity checks.
- Verify no claim of deleting third-party backups/screenshots.

## Gate D — release
- Kotlin unit tests and CI green.
- Android 15 real-device tests, Windows tests.
- Privacy/security review, dependency audit, signed APK.
