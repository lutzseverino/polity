DELETE FROM constitutional_powers
WHERE code = 'INTRODUCE_OFFICE_ASSIGNMENT';

DELETE FROM procedures procedure
WHERE procedure.code = 'office-assignment'
  AND NOT EXISTS (
    SELECT 1
    FROM motions motion
    WHERE motion.procedure_id = procedure.id
  );
