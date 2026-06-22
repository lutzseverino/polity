ALTER TABLE certifications
  ADD COLUMN modality text NOT NULL DEFAULT 'YES_NO',
  ADD COLUMN election_participation_count integer,
  ADD COLUMN election_decisive boolean,
  ADD COLUMN election_winner_membership_id uuid,
  ADD COLUMN election_winner_name text,
  ALTER COLUMN yes_count DROP NOT NULL,
  ALTER COLUMN no_count DROP NOT NULL,
  ALTER COLUMN abstain_count DROP NOT NULL;

ALTER TABLE certifications
  ADD CONSTRAINT chk_certification_modality
  CHECK (modality IN ('YES_NO', 'OFFICE_ELECTION')),
  ADD CONSTRAINT chk_certification_yes_no_counts
  CHECK (
    (modality = 'YES_NO'
      AND yes_count IS NOT NULL
      AND no_count IS NOT NULL
      AND abstain_count IS NOT NULL
      AND election_participation_count IS NULL
      AND election_decisive IS NULL
      AND election_winner_membership_id IS NULL
      AND election_winner_name IS NULL)
    OR
    (modality = 'OFFICE_ELECTION'
      AND yes_count IS NULL
      AND no_count IS NULL
      AND abstain_count IS NULL
      AND election_participation_count IS NOT NULL
      AND election_decisive IS NOT NULL
      AND (passed = FALSE OR election_winner_membership_id IS NOT NULL)
      AND (passed = FALSE OR election_winner_name IS NOT NULL))
  );
