ALTER TABLE public.membership_invitations
  ALTER COLUMN invited_user_id DROP NOT NULL,
  DROP COLUMN authorization_subject,
  ADD COLUMN cardo_invitation_id UUID,
  ADD COLUMN cardo_expires_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE public.membership_invitations
  DROP CONSTRAINT membership_invitations_status_check,
  ADD CONSTRAINT membership_invitations_status_check
    CHECK (status = ANY (ARRAY['PENDING'::text, 'ACCEPTED'::text, 'CANCELLED'::text]));

UPDATE public.membership_invitations
SET status = 'CANCELLED',
    responded_at = COALESCE(responded_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'PENDING'
  AND cardo_invitation_id IS NULL;

CREATE UNIQUE INDEX uq_membership_invitations_cardo_invitation
  ON public.membership_invitations (cardo_invitation_id)
  WHERE cardo_invitation_id IS NOT NULL;
