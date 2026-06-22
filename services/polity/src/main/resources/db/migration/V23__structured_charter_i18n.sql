ALTER TABLE institutions
  ADD COLUMN name_key text;

UPDATE institutions
SET name_key = 'polity.institution.citizensAssembly.name'
WHERE kind = 'ASSEMBLY'
  AND name = 'Citizens'' Assembly'
  AND name_key IS NULL;

UPDATE constitution_versions
SET
  title = 'Structured Charter',
  body = 'Binding constitutional rules are stored as structured institutions, procedures, offices, and powers.',
  title_key = 'constitution.structured_charter.title',
  body_key = 'constitution.structured_charter.body'
WHERE (
    title_key = 'constitution.starter_republic.title'
    AND body_key = 'constitution.starter_republic.body'
  )
  OR (
    title = 'Starter Constitution'
    AND title_key = 'constitution.structured_charter.title'
    AND body_key = 'constitution.structured_charter.body'
  );
