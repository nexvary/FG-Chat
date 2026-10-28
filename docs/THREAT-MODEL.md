# Threat model — alpha, not independently audited

Assets: local Tox private identity, approved peer keys, plaintext content, contact graph, device storage. Adversaries: network MITM, malicious peers/bootstrap/relay nodes, stolen device/backup, hostile linked client and local malware.

| Threat | Current control | Residual risk / gate |
|---|---|---|
| Impersonation/MITM | Tox authenticates known keys; compare full key over trusted channel | Substituted invitation can introduce attacker; QR is not verified identity by itself |
| Device key substitution | Approved friend key persisted encrypted | Independent app root/certificates and device transparency absent |
| Compromised/lost device | Keystore encryption, OS sandbox; block a peer locally | Unlocked/malicious OS can read plaintext; no remote revocation/recovery yet |
| Stolen backup | Android backup disabled; encrypted noBackup vault | No user export/restore implemented; Keystore loss means unrecoverable identity |
| IP / metadata | No FG server; content encrypted | Peers/public network observe IP/timing; Tor not integrated |
| Malicious bootstrap/relay | Pinned node keys; authenticated peer content; multiple operators | Eclipse, DoS, traffic analysis and availability remain; no anonymity claim |
| Offline message delivery | No app queue/mailbox; ready check and session token; no retry | Already in-flight traffic during undetected network loss can arrive |
| Forged/replayed deletion | Remote deletion not exposed | Must add signed scoped monotonic requests/receipts and replay journal before feature |
| Malicious files | No incoming file handling yet; standalone FileOffer is not a transfer engine | Before enabling: consent, quotas, storage checks, bounded streaming, safe names, digest, content-URI handling |
| Invitation flood | Native request event and UI request caps; explicit accept; persistent block | Network-level DoS and many approved-contact history use remain possible |
| Secrets in logs | No app plaintext/key logging; no native logging callback | Debugger/root/memory dumps still expose data |
| Supply-chain | Version+SHA-256 pinned native downloads; dependency review/Dependabot | Upstream compilation not independently reproduced; security audit outstanding |

No voice/video/group/file APIs are wired. Do not market these as protected capabilities. No multi-device key copying, production signing key, network anonymity or guaranteed remote erasure is offered. Local clear only clears this process memory; cannot erase screenshots/external copies/modified clients. Any local database must be encrypted before persistent history is enabled.
