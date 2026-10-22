# ADR-0002: Short invitation identifiers without fixed 15-character requirement

Status: Accepted product decision, implementation pending.

The user's requirement is a **short, easy-to-share code**, not precisely 15 characters.

- Do not treat a short code as the cryptographic identity or encryption key.
- Permanent invite aliases may be rotated; one-time invitations expire after 24 hours and must be consumed once.
- Short codes need an authenticated resolution mechanism. A purely serverless mode cannot guarantee global lookup of an arbitrary short code with no discovery infrastructure.
- Preferred no-directory approach: a compact human-readable pairing code for **live, interactive pairing** plus a QR/deep-link containing the full public identity and signed invitation payload. The pairing code is only meaningful within the active pairing session, never a globally resolvable identifier.
- If a globally resolvable short code is later required, explicitly document and obtain approval for distributed directory / DHT storage, collision handling, Sybil and enumeration defenses, and metadata exposure.
- Display codes in short grouped segments; allow copy and QR scan. Final length and alphabet must be set based on collision/guessing analysis and UX tests, not an arbitrary number.
- Do not silently claim the previously implemented 15-character generator fulfills the revised requirement; it remains a prototype pending redesign.
