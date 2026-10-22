# ADR-0003: No operator-managed infrastructure

Status: Accepted product clarification; architecture selection pending prototype verification.

The requirement is NOT 'no intermediary network nodes'. The requirement is that FG Chat must not depend on any server hosted, paid for, maintained or monitored by FG Machines/NEXVARY or the user's personal infrastructure.

Allowed: public bootstrap nodes, Tor relays, public federated Matrix homeservers and other independently operated infrastructure, subject to privacy, availability and terms. No promise that public infrastructure is guaranteed to remain available.

User constraints:
- No offline message delivery or server-side mailbox; show 'unavailable' when recipient offline.
- End-to-end encryption; do not claim metadata is encrypted or hidden by default.
- Portable identity, secure device-to-device migration and eventually multiple devices.
- Large-file transfer, ideally encrypted peer-to-peer.
- Bilateral delete request with verified acknowledgment, without claiming control over screenshots/backups/uncooperative clients.
- Short human-friendly pairing; full cryptographic identities must still be authenticated.

Decision: Evaluate Tox as initial messaging/voice/video transport candidate; Tor as optional network privacy layer; Matrix only as alternative if its homeserver storage/sync semantics can be explicitly disabled or are accepted after evaluation. Tox bootstrap nodes and possible relay nodes are allowed under the clarified requirement. Avoid building a custom cryptographic protocol.

Implementation plan:
1. Proof-of-concept connectivity on Android and Windows, NAT/firewall matrix, Tor option.
2. Threat model: peer IP exposure, metadata, bootstrap reliability, malicious nodes, recovery and device revocation.
3. Design independent application-level identity and device authorization rather than assuming Tox native identity supports WhatsApp-like multi-device.
4. Validate transfer resumption and deletion acknowledgments with both peers online.
5. Audit licenses, security posture and maintenance before shipping.

Previous ADR-0001 strict-direct-only prohibition of relays is superseded by this clarification. Previous PostgreSQL server files are exploratory only, not a planned operator-run service.
