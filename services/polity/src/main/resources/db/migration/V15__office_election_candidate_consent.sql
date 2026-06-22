ALTER TABLE office_election_candidates
  ADD COLUMN status text NOT NULL DEFAULT 'ACCEPTED',
  ADD COLUMN responded_at timestamp with time zone;

UPDATE office_election_candidates
SET responded_at = created_at
WHERE status = 'ACCEPTED';

ALTER TABLE office_election_candidates
  ADD CONSTRAINT office_election_candidate_status_check
  CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'DISQUALIFIED'));
