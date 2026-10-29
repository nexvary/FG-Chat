# Archived operator-run directory prototype

Not part of the Android build or deployment. Retained for historical reference only. ADR-0003 and ADR-0004 supersede this design. Do not deploy it for FG Chat.

# Server design — NOT YET A RUNNING SERVICE

Database DDL and atomic redemption SQL are the first backend deliverables.

## Required before exposing an API
1. Authenticate device identity via nonce challenge signed by its private key; verify signature against stored public key.
2. Generate codes using CSPRNG; normalize before computing HMAC-SHA256 using a secret stored outside the database and repository.
3. Use constant-time comparison where applicable and uniform errors to avoid account enumeration.
4. Rate-limit redemption and creation by IP/device and globally, with abuse monitoring.
5. Implement atomic permanent-code rotation: revoke old and insert new in one transaction; enforce one active permanent code per owner.
6. Permanent-code lookups create pending contact requests, never automatically establish contacts.
7. For one-time redemption, insert pending contact request within the same transaction as the conditional UPDATE; roll back on failure.
8. Apply DB permissions, migrations, backups, secret rotation, logging redaction and integration tests.
9. Codes are invitations only; they cannot authenticate a user or serve as cryptographic keys.

The SQL uses named placeholders as a reference design; a backend must bind parameters with prepared statements.
