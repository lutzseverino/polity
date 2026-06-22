INSERT INTO offices (
  polity_id,
  constitution_version_id,
  jurisdiction_id,
  code,
  name,
  description,
  term_length_days
)
SELECT
  constitution.polity_id,
  constitution.id,
  jurisdiction.id,
  'tribune',
  'Tribune',
  'Introduces sanction proceedings while citizens retain voting and appeal rights.',
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
      AND existing.code = 'tribune'
  );

UPDATE constitutional_powers
SET
  holder_scope = 'OFFICE',
  holder_office_code = 'tribune'
WHERE code = 'INTRODUCE_SANCTION'
  AND EXISTS (
    SELECT 1
    FROM constitution_versions constitution
    WHERE constitution.id = constitutional_powers.constitution_version_id
      AND constitution.status = 'RATIFIED'
  );
