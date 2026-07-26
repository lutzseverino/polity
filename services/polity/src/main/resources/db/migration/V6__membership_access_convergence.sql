ALTER TABLE public.memberships
  ADD COLUMN grant_receipt_id UUID,
  ADD CONSTRAINT uq_memberships_grant_receipt UNIQUE (grant_receipt_id);

ALTER TABLE public.membership_invitations
  ADD COLUMN acceptance_status TEXT,
  ADD COLUMN acceptance_requested_at TIMESTAMP WITH TIME ZONE,
  ADD COLUMN acceptance_completed_at TIMESTAMP WITH TIME ZONE,
  ADD COLUMN acceptance_failure_code TEXT,
  ADD CONSTRAINT membership_invitations_acceptance_status_check
    CHECK (acceptance_status IS NULL OR acceptance_status = ANY (
      ARRAY['REQUESTED'::text, 'COMPLETED'::text, 'FAILED'::text]
    )),
  ADD CONSTRAINT membership_invitations_acceptance_state_check
    CHECK (
      (acceptance_status IS NULL
        AND acceptance_requested_at IS NULL
        AND acceptance_completed_at IS NULL
        AND acceptance_failure_code IS NULL)
      OR
      (acceptance_status = 'REQUESTED'
        AND acceptance_requested_at IS NOT NULL
        AND acceptance_completed_at IS NULL
        AND acceptance_failure_code IS NULL)
      OR
      (acceptance_status = 'COMPLETED'
        AND acceptance_requested_at IS NOT NULL
        AND acceptance_completed_at IS NOT NULL
        AND acceptance_failure_code IS NULL)
      OR
      (acceptance_status = 'FAILED'
        AND acceptance_requested_at IS NOT NULL
        AND acceptance_completed_at IS NOT NULL
        AND acceptance_failure_code IS NOT NULL)
    ),
  ADD CONSTRAINT membership_invitations_acceptance_lifecycle_check
    CHECK (
      acceptance_status IS NULL
      OR (status = 'PENDING' AND acceptance_status = 'REQUESTED')
      OR (status = 'ACCEPTED' AND acceptance_status = 'COMPLETED')
      OR (status = 'CANCELLED' AND acceptance_status = 'FAILED')
    );
