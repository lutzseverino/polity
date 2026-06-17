CREATE TABLE membership_invitations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  invited_user_id uuid NOT NULL,
  authorization_subject text NOT NULL,
  email text NOT NULL,
  invited_by uuid NOT NULL,
  status text NOT NULL,
  invited_at timestamp with time zone NOT NULL,
  responded_at timestamp with time zone,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, invited_by) REFERENCES memberships(polity_id, id),
  UNIQUE (polity_id, id),
  CHECK (status IN ('PENDING', 'ACCEPTED'))
);

CREATE UNIQUE INDEX uq_pending_membership_invitations_email
  ON membership_invitations(polity_id, lower(email))
  WHERE status = 'PENDING';

CREATE UNIQUE INDEX uq_pending_membership_invitations_invitee
  ON membership_invitations(polity_id, invited_user_id)
  WHERE status = 'PENDING';

CREATE INDEX idx_membership_invitations_invitee
  ON membership_invitations(invited_user_id, status, invited_at DESC);

CREATE INDEX idx_membership_invitations_polity
  ON membership_invitations(polity_id, invited_at DESC);
