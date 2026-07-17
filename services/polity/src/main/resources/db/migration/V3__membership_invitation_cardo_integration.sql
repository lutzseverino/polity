ALTER TABLE public.membership_invitations
  ALTER COLUMN invited_user_id DROP NOT NULL,
  DROP COLUMN authorization_subject,
  ADD COLUMN cardo_invitation_id UUID,
  ADD COLUMN cardo_expires_at TIMESTAMP WITH TIME ZONE;

CREATE UNIQUE INDEX uq_membership_invitations_cardo_invitation
  ON public.membership_invitations (cardo_invitation_id)
  WHERE cardo_invitation_id IS NOT NULL;
