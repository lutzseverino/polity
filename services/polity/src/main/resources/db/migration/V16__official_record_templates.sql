ALTER TABLE official_record_entries
  ADD COLUMN title_key text,
  ADD COLUMN body_key text,
  ADD COLUMN template_params jsonb NOT NULL DEFAULT '{}'::jsonb,
  ADD CONSTRAINT official_record_template_check
  CHECK ((title_key IS NULL AND body_key IS NULL) OR (title_key IS NOT NULL AND body_key IS NOT NULL));
