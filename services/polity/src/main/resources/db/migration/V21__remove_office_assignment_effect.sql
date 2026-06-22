-- Direct office assignment is no longer a current constitutional mechanic.
-- Preserve historical assignment motions and proposals; only remove unused
-- constitutional defaults so new and active presets use office elections.
DELETE FROM constitutional_powers
WHERE code = 'INTRODUCE_OFFICE_ASSIGNMENT';

DELETE FROM procedures procedure
WHERE procedure.code = 'office-assignment'
  AND NOT EXISTS (
    SELECT 1
    FROM motions motion
    WHERE motion.procedure_id = procedure.id
  );
