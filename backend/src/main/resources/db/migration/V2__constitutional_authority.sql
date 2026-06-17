ALTER TABLE constitution_versions
  ADD CONSTRAINT chk_constitution_status
  CHECK (status IN ('RATIFIED', 'SUPERSEDED'));

CREATE UNIQUE INDEX ux_constitutions_one_ratified
  ON constitution_versions(polity_id)
  WHERE status = 'RATIFIED';

ALTER TABLE constitutional_powers
  ADD COLUMN holder_office_code text,
  ADD CONSTRAINT chk_power_holder_scope
  CHECK (holder_scope IN ('ACTIVE_MEMBER', 'OFFICE')),
  ADD CONSTRAINT chk_power_holder_office
  CHECK (
    (holder_scope = 'OFFICE' AND holder_office_code IS NOT NULL)
    OR (holder_scope <> 'OFFICE' AND holder_office_code IS NULL)
  );

ALTER TABLE procedures
  ADD CONSTRAINT chk_procedure_effect_type
  CHECK (effect_type IN (
    'ADOPT_RESOLUTION',
    'ASSIGN_OFFICE',
    'APPLY_SANCTION',
    'GRANT_APPEAL',
    'AMEND_CONSTITUTION'
  ));

ALTER TABLE procedures
  ADD COLUMN minimum_notice_hours integer NOT NULL DEFAULT 0,
  ADD COLUMN voting_period_hours integer NOT NULL DEFAULT 24,
  ADD CONSTRAINT chk_procedure_threshold
  CHECK (threshold IN (
    'SIMPLE_MAJORITY_CAST',
    'MAJORITY_OF_ELIGIBLE',
    'TWO_THIRDS_CAST',
    'TWO_THIRDS_ELIGIBLE'
  )),
  ADD CONSTRAINT chk_procedure_timing
  CHECK (minimum_notice_hours >= 0 AND voting_period_hours > 0);

ALTER TABLE motions
  ADD CONSTRAINT chk_motion_effect_type
  CHECK (effect_type IN (
    'ADOPT_RESOLUTION',
    'ASSIGN_OFFICE',
    'APPLY_SANCTION',
    'GRANT_APPEAL',
    'AMEND_CONSTITUTION'
  ));

ALTER TABLE motions
  ADD COLUMN voting_opens_at timestamp with time zone,
  ADD COLUMN voting_closes_at timestamp with time zone,
  ADD COLUMN certification_opens_at timestamp with time zone;

UPDATE motions
SET
  voting_opens_at = opened_at,
  voting_closes_at = opened_at + interval '24 hours',
  certification_opens_at = opened_at + interval '24 hours'
WHERE voting_opens_at IS NULL;

ALTER TABLE motions
  ALTER COLUMN voting_opens_at SET NOT NULL,
  ALTER COLUMN voting_closes_at SET NOT NULL,
  ALTER COLUMN certification_opens_at SET NOT NULL,
  ADD CONSTRAINT chk_motion_voting_window
  CHECK (
    voting_opens_at >= opened_at
    AND voting_closes_at > voting_opens_at
    AND certification_opens_at >= voting_closes_at
  );
