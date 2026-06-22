ALTER TABLE polities
  ADD COLUMN status text NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN disbanded_at timestamp with time zone;

ALTER TABLE polities
  ADD CONSTRAINT chk_polities_status CHECK (status IN ('ACTIVE', 'DISBANDED')),
  ADD CONSTRAINT chk_polities_disbanded_at CHECK (
    (status = 'ACTIVE' AND disbanded_at IS NULL)
    OR (status = 'DISBANDED' AND disbanded_at IS NOT NULL)
  );

ALTER TABLE procedures
  DROP CONSTRAINT chk_procedure_effect_type,
  ADD CONSTRAINT chk_procedure_effect_type
  CHECK (effect_type IN (
    'ADOPT_RESOLUTION',
    'ASSIGN_OFFICE',
    'ELECT_OFFICE',
    'APPLY_SANCTION',
    'GRANT_APPEAL',
    'AMEND_CONSTITUTION',
    'DISBAND_POLITY'
  ));

ALTER TABLE motions
  DROP CONSTRAINT chk_motion_effect_type,
  ADD CONSTRAINT chk_motion_effect_type
  CHECK (effect_type IN (
    'ADOPT_RESOLUTION',
    'ASSIGN_OFFICE',
    'ELECT_OFFICE',
    'APPLY_SANCTION',
    'GRANT_APPEAL',
    'AMEND_CONSTITUTION',
    'DISBAND_POLITY'
  ));

CREATE INDEX idx_polities_status_created ON polities(status, created_at DESC);

INSERT INTO procedures (
  polity_id,
  constitution_version_id,
  institution_id,
  code,
  name,
  quorum_numerator,
  quorum_denominator,
  threshold,
  effect_type,
  minimum_notice_hours,
  voting_period_hours
)
SELECT
  constitution.polity_id,
  constitution.id,
  institution.id,
  'disbandment',
  'Disbandment',
  COALESCE(amendment.quorum_numerator, 1),
  COALESCE(amendment.quorum_denominator, 2),
  'TWO_THIRDS_ELIGIBLE',
  'DISBAND_POLITY',
  COALESCE(amendment.minimum_notice_hours, 24),
  COALESCE(amendment.voting_period_hours, 120)
FROM constitution_versions constitution
JOIN institutions institution
  ON institution.polity_id = constitution.polity_id
  AND institution.constitution_version_id = constitution.id
  AND institution.kind = 'ASSEMBLY'
LEFT JOIN procedures amendment
  ON amendment.constitution_version_id = constitution.id
  AND amendment.code = 'constitution-amendment'
WHERE constitution.status = 'RATIFIED'
  AND NOT EXISTS (
    SELECT 1
    FROM procedures existing
    WHERE existing.constitution_version_id = constitution.id
      AND existing.code = 'disbandment'
  );

INSERT INTO constitutional_powers (
  polity_id,
  constitution_version_id,
  code,
  name,
  holder_scope,
  holder_office_code
)
SELECT
  constitution.polity_id,
  constitution.id,
  'INTRODUCE_DISBANDMENT',
  'Propose disbandment',
  'ACTIVE_MEMBER',
  NULL
FROM constitution_versions constitution
WHERE constitution.status = 'RATIFIED'
  AND NOT EXISTS (
    SELECT 1
    FROM constitutional_powers existing
    WHERE existing.constitution_version_id = constitution.id
      AND existing.code = 'INTRODUCE_DISBANDMENT'
  );
