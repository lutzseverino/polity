ALTER TABLE constitution_versions
  ADD COLUMN title_key text,
  ADD COLUMN body_key text,
  ADD COLUMN template_params jsonb NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE offices
  ADD COLUMN name_key text,
  ADD COLUMN description_key text;

ALTER TABLE procedures
  ADD COLUMN name_key text;

ALTER TABLE constitutional_powers
  ADD COLUMN name_key text;

UPDATE constitution_versions
SET
  title_key = 'constitution.structured_charter.title',
  body_key = 'constitution.structured_charter.body'
WHERE version = 1
  AND title IN ('Starter Constitution', 'Structured Charter')
  AND title_key IS NULL
  AND body_key IS NULL;

UPDATE offices
SET
  name_key = 'office.' || code || '.name',
  description_key = 'office.' || code || '.description'
WHERE code IN ('steward', 'tribune')
  AND name_key IS NULL
  AND description_key IS NULL;

UPDATE procedures
SET name_key =
  CASE code
    WHEN 'ordinary-resolution' THEN 'procedure.ordinary_resolution.name'
    WHEN 'office-election' THEN 'procedure.office_election.name'
    WHEN 'sanction' THEN 'procedure.sanction.name'
    WHEN 'appeal' THEN 'procedure.appeal.name'
    WHEN 'constitution-amendment' THEN 'procedure.constitution_amendment.name'
    WHEN 'disbandment' THEN 'procedure.disbandment.name'
  END
WHERE code IN (
    'ordinary-resolution',
    'office-election',
    'sanction',
    'appeal',
    'constitution-amendment',
    'disbandment'
  )
  AND name_key IS NULL;

UPDATE constitutional_powers
SET name_key =
  CASE code
    WHEN 'ADMIT_MEMBER' THEN 'constitutional_power.admit_member.name'
    WHEN 'INTRODUCE_MOTION' THEN 'constitutional_power.introduce_motion.name'
    WHEN 'INTRODUCE_OFFICE_ELECTION' THEN 'constitutional_power.introduce_office_election.name'
    WHEN 'INTRODUCE_SANCTION' THEN 'constitutional_power.introduce_sanction.name'
    WHEN 'INTRODUCE_APPEAL' THEN 'constitutional_power.introduce_appeal.name'
    WHEN 'INTRODUCE_AMENDMENT' THEN 'constitutional_power.introduce_amendment.name'
    WHEN 'INTRODUCE_DISBANDMENT' THEN 'constitutional_power.introduce_disbandment.name'
    WHEN 'REQUEST_CERTIFICATION' THEN 'constitutional_power.request_certification.name'
  END
WHERE code IN (
    'ADMIT_MEMBER',
    'INTRODUCE_MOTION',
    'INTRODUCE_OFFICE_ELECTION',
    'INTRODUCE_SANCTION',
    'INTRODUCE_APPEAL',
    'INTRODUCE_AMENDMENT',
    'INTRODUCE_DISBANDMENT',
    'REQUEST_CERTIFICATION'
  )
  AND name_key IS NULL;
