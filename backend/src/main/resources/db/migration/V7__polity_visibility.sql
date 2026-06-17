ALTER TABLE polities
  ADD COLUMN visibility text NOT NULL DEFAULT 'PRIVATE';

ALTER TABLE polities
  ADD CONSTRAINT chk_polities_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE'));

CREATE INDEX idx_polities_visibility_created ON polities(visibility, created_at DESC);
