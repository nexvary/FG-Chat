-- Run inside a transaction, authenticated as :requester_id.
-- :code_mac must be HMAC-SHA256(server_secret, normalized_code).
-- A unique, unused ONE_TIME invite can be redeemed only once.
UPDATE invitations
SET redeemed_by = :requester_id,
    redeemed_at = now()
WHERE code_mac = :code_mac
  AND kind = 'ONE_TIME'
  AND redeemed_at IS NULL
  AND revoked_at IS NULL
  AND expires_at > now()
  AND owner_id <> :requester_id
RETURNING id, owner_id;
-- If zero rows returned: expired, already used, revoked, self-use, or invalid.
-- Insert contact request using returned invitation ID in same transaction,
-- then COMMIT. Any error must ROLLBACK.
