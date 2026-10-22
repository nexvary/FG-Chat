# FG Chat

Privacy-first messaging project inspired by Twinme's contact-by-invitation model. Independent implementation; no Twinme code or assets.

## Confirmed product requirements
- No phone number or email required for local identity creation.
- Permanent 15-character alphanumeric invitation code, revocable and rotatable.
- One-time 15-character alphanumeric invitation code, expiring 24 hours after issuance.
- Invitations are discovery capabilities, **not** authentication credentials or encryption keys.
- Contact requests require explicit recipient approval.
- Android-first, Kotlin/Compose, Arabic RTL and English; Windows later.
- End-to-end encrypted messaging and WebRTC calls are **planned**, not implemented.

## Security rules
- Use a cryptographically secure random generator; avoid ambiguous characters in manual-entry codes.
- Store only a keyed hash of normalized invitation codes server-side, with unique constraints and atomic redemption.
- Enforce expiration and one-time redemption on the server in a single transaction; client-side checks are advisory.
- Authenticate code management with device identity keys and signed requests; do not equate possession of an invitation code with account ownership.
- Rate-limit attempts per IP/device and globally; use anti-abuse controls that preserve privacy.
- Rotate permanent codes without deleting existing contacts.
- Do not log raw invitation codes, secret keys, plaintext messages or sensitive metadata.
- Implement E2EE with an audited protocol/library; never invent cryptography.
- Public repository: never commit secrets, certificates or production endpoints.

## Milestones
1. Invitation-domain core and unit tests.
2. Backend persistence, atomic redeem, auth, throttling, abuse tests.
3. Android onboarding, contacts, QR and RTL accessibility.
4. Audited E2EE protocol integration and offline encrypted delivery.
5. Media, voice/video via WebRTC/TURN, push notifications.
6. Security review, Android 15 physical-device verification, signed release.

## Status
Initial architecture and invitation-domain scaffold only. No working messenger, backend, E2EE, APK or device-test evidence yet.
