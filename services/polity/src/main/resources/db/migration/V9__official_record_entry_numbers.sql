CREATE TABLE official_record_sequences (
  polity_id uuid PRIMARY KEY REFERENCES polities(id) ON DELETE CASCADE,
  next_entry_number integer NOT NULL DEFAULT 1,
  CHECK (next_entry_number > 0)
);

ALTER TABLE official_record_entries
  ADD COLUMN entry_number integer;

WITH numbered_entries AS (
  SELECT
    id,
    row_number() OVER (
      PARTITION BY polity_id
      ORDER BY occurred_at ASC, created_at ASC, id ASC
    )::integer AS entry_number
  FROM official_record_entries
)
UPDATE official_record_entries entry
SET entry_number = numbered_entries.entry_number
FROM numbered_entries
WHERE entry.id = numbered_entries.id;

ALTER TABLE official_record_entries
  ALTER COLUMN entry_number SET NOT NULL,
  ADD CONSTRAINT ck_record_entry_number_positive CHECK (entry_number > 0),
  ADD CONSTRAINT uq_record_polity_entry_number UNIQUE (polity_id, entry_number);

INSERT INTO official_record_sequences (polity_id, next_entry_number)
SELECT polity_id, max(entry_number) + 1
FROM official_record_entries
GROUP BY polity_id;

INSERT INTO official_record_sequences (polity_id, next_entry_number)
SELECT polity.id, 1
FROM polities polity
WHERE NOT EXISTS (
  SELECT 1
  FROM official_record_sequences record_sequence
  WHERE record_sequence.polity_id = polity.id
);
