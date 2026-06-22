ALTER TABLE certifications RENAME COLUMN explanation TO outcome_reason;

UPDATE certifications
SET outcome_reason =
  CASE
    WHEN passed THEN 'PASSED'
    WHEN NOT quorum_met THEN 'QUORUM_NOT_MET'
    ELSE 'THRESHOLD_NOT_MET'
  END;

ALTER TABLE certifications
  ADD CONSTRAINT certifications_outcome_reason_check
  CHECK (outcome_reason IN (
    'PASSED',
    'QUORUM_NOT_MET',
    'THRESHOLD_NOT_MET',
    'NO_DECISIVE_PLURALITY'
  ));
