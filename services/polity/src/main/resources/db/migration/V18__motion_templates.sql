ALTER TABLE motions
  ADD COLUMN title_key text,
  ADD COLUMN body_key text,
  ADD COLUMN template_params jsonb NOT NULL DEFAULT '{}'::jsonb,
  ADD CONSTRAINT motion_template_check
  CHECK ((title_key IS NULL AND body_key IS NULL) OR (title_key IS NOT NULL AND body_key IS NOT NULL));
