ALTER TABLE procedures
  ADD COLUMN electorate text NOT NULL DEFAULT 'ACTIVE_MEMBERS',
  ADD COLUMN electorate_office_code text;

ALTER TABLE constitution_procedure_change_proposals
  ADD COLUMN electorate text,
  ADD COLUMN electorate_office_code text;

ALTER TABLE procedures
  ADD CONSTRAINT chk_procedure_electorate
  CHECK (electorate IN ('ACTIVE_MEMBERS', 'OFFICE_HOLDERS')),
  ADD CONSTRAINT chk_procedure_electorate_office
  CHECK (
    (electorate = 'OFFICE_HOLDERS' AND electorate_office_code IS NOT NULL)
    OR (electorate <> 'OFFICE_HOLDERS' AND electorate_office_code IS NULL)
  ),
  ADD CONSTRAINT chk_procedure_electorate_office_code
  CHECK (electorate_office_code IS NULL OR electorate_office_code ~ '^[a-z][a-z0-9-]*$');

ALTER TABLE constitution_procedure_change_proposals
  ADD CONSTRAINT chk_constitution_procedure_change_electorate
  CHECK (electorate IS NULL OR electorate IN ('ACTIVE_MEMBERS', 'OFFICE_HOLDERS')),
  ADD CONSTRAINT chk_constitution_procedure_change_electorate_office
  CHECK (
    (electorate = 'OFFICE_HOLDERS' AND electorate_office_code IS NOT NULL)
    OR (electorate IS NULL AND electorate_office_code IS NULL)
    OR (electorate = 'ACTIVE_MEMBERS' AND electorate_office_code IS NULL)
  ),
  ADD CONSTRAINT chk_constitution_procedure_change_electorate_office_code
  CHECK (electorate_office_code IS NULL OR electorate_office_code ~ '^[a-z][a-z0-9-]*$');

INSERT INTO offices (
  polity_id,
  constitution_version_id,
  jurisdiction_id,
  code,
  name,
  description,
  name_key,
  description_key,
  term_length_days
)
SELECT
  constitution.polity_id,
  constitution.id,
  jurisdiction.id,
  'magistrate',
  'Magistrate',
  'Decides appeals so sanctions remain reviewable by an independent office.',
  'office.magistrate.name',
  'office.magistrate.description',
  COALESCE(steward.term_length_days, 14)
FROM constitution_versions constitution
JOIN jurisdictions jurisdiction
  ON jurisdiction.polity_id = constitution.polity_id
  AND jurisdiction.kind = 'ROOT'
LEFT JOIN offices steward
  ON steward.constitution_version_id = constitution.id
  AND steward.code = 'steward'
WHERE constitution.status = 'RATIFIED'
  AND NOT EXISTS (
    SELECT 1
    FROM offices existing
    WHERE existing.constitution_version_id = constitution.id
      AND existing.code = 'magistrate'
  );

UPDATE procedures
SET
  electorate = 'OFFICE_HOLDERS',
  electorate_office_code = 'magistrate'
WHERE code = 'appeal'
  AND EXISTS (
    SELECT 1
    FROM constitution_versions constitution
    WHERE constitution.id = procedures.constitution_version_id
      AND constitution.status = 'RATIFIED'
  );
