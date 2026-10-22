# FG Chat — Serverless Direct-Peer Architecture (ADR-0001)

Status: Accepted product requirements; implementation pending.

## Non-negotiable behavior
- No central message store, offline queue, push relay or TURN/data relay in strict-direct mode.
- A message is sent only when sender and recipient are both connected; no deferred delivery.
- Encrypted local message history only.
- Contact invitations: permanent rotatable 15-character code and one-use 24-hour code. Codes must resolve to authenticated public identities; 15 characters cannot directly encode a full cryptographic identity. Without a directory service, share a QR payload containing the full identity and code, or use peer-distributed discovery; a short code alone cannot guarantee universal lookup without some directory/discovery infrastructure.
- Account migration to another device: authenticated local QR handshake and end-to-end encrypted transfer over a direct channel, with explicit confirmation and revocation of the old device. Backup recovery uses user-controlled encrypted export, never vendor escrow.
- Optional multi-device: each device has independent keys and is explicitly authorized by an existing device; no copying long-lived private keys between concurrently active devices. Track device revocation and contact trust changes.
- Large files: encrypted chunked peer-to-peer transfer, SHA-256 content verification, bounded memory, resume only when both peers reconnect and retain state, quotas and disk-space checks.
- Bilateral delete: signed delete request referencing conversation and message IDs; receiver deletes local copies and returns signed acknowledgment. Display pending when offline; never claim remote deletion until acknowledgment. No guarantee against screenshots, backups or malicious clients.
- Direct mode may fail behind restrictive NAT/firewalls; no covert fallback relay.
- Explicitly warn users that direct P2P peers may learn each other's IP addresses.

## Transport choice
Primary evaluation: c-toxcore DHT discovery and direct UDP transport; it uses public bootstrap nodes and may use TCP relays. Configure strict mode to forbid relay transport, test connectivity, and disclose reduced reachability. Tox is experimental and not independently audited as a complete protocol; security review required.
Tor onion services are NOT direct wire-level peer-to-peer: rendezvous and relay nodes carry encrypted traffic, so Tor is an optional privacy mode only if the user later permits intermediary network relays.
ZeroNet is not selected as a private chat transport.
No PostgreSQL-backed invitation service is part of the strict-serverless production architecture; previous server SQL is an abandoned prototype, not a deployment plan.

## Test gates
1. Both online: bidirectional messages and transfer checksums pass.
2. Recipient offline: sender receives unavailable status; no server storage, retry queue or phantom delivery.
3. NAT restricted: report connection unavailable rather than silently relaying.
4. Device migration: old-device approval, encrypted transfer, new device activation, old-device revocation.
5. Multi-device: authorize/revoke independent device keys, handle concurrent edits and duplicate delivery.
6. Delete both sides: signed request, local deletion, signed remote acknowledgment, offline pending state.
7. Network and security audit, Android 15 battery/process death tests, Windows compatibility.

## Clarification
Offline is not inherently more secure in every threat model. Local encrypted storage and peer authentication matter more than the absence of offline delivery alone.
