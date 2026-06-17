ALTER TABLE official_record_entries
  ADD COLUMN motion_id uuid,
  ADD COLUMN procedure_id uuid,
  ADD COLUMN institution_id uuid,
  ADD COLUMN power_code text,
  ADD COLUMN certification_id uuid,
  ADD COLUMN effect_type text,
  ADD COLUMN outcome text,
  ADD CONSTRAINT fk_record_motion
  FOREIGN KEY (polity_id, motion_id) REFERENCES motions(polity_id, id),
  ADD CONSTRAINT fk_record_procedure
  FOREIGN KEY (polity_id, procedure_id) REFERENCES procedures(polity_id, id),
  ADD CONSTRAINT fk_record_institution
  FOREIGN KEY (polity_id, institution_id) REFERENCES institutions(polity_id, id);
