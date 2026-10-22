-- PostgreSQL 16+; invite codes are never stored in plaintext.
CREATE TABLE IF NOT EXISTS identities (
 id UUID PRIMARY KEY,
 public_key BYTEA NOT NULL UNIQUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS invitations (
 id UUID PRIMARY KEY,
 owner_id UUID NOT NULL REFERENCES identities(id) ON DELETE CASCADE,
 code_mac BYTEA NOT NULL UNIQUE,
 kind TEXT NOT NULL CHECK(kind IN ('PERMANENT','ONE_TIME')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 expires_at TIMESTAMPTZ,
 redeemed_by UUID REFERENCES identities(id),
 redeemed_at TIMESTAMPTZ,
 revoked_at TIMESTAMPTZ,
 CHECK ((kind='PERMANENT' AND expires_at IS NULL) OR (kind='ONE_TIME' AND expires_at IS NOT NULL)),
 CHECK ((redeemed_by IS NULL) = (redeemed_at IS NULL))
);
CREATE UNIQUE INDEX IF NOT EXISTS one_active_permanent_invite
ON invitations(owner_id) WHERE kind='PERMANENT' AND revoked_at IS NULL;
CREATE INDEX IF NOT EXISTS invite_owner_idx ON invitations(owner_id);
CREATE TABLE IF NOT EXISTS contact_requests (
 id UUID PRIMARY KEY,
 sender_id UUID NOT NULL REFERENCES identities(id),
 recipient_id UUID NOT NULL REFERENCES identities(id),
 invitation_id UUID NOT NULL REFERENCES invitations(id),
 status TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','ACCEPTED','REJECTED')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK(sender_id <> recipient_id)
);
