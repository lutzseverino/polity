ALTER TABLE polities
  ADD COLUMN bootstrap_completed_at timestamp with time zone;

WITH full_government_polities AS (
  SELECT membership.polity_id
  FROM memberships membership
  WHERE membership.status = 'ACTIVE'
    AND NOT EXISTS (
      SELECT 1
      FROM sanctions sanction
      WHERE sanction.polity_id = membership.polity_id
        AND sanction.target_membership_id = membership.id
        AND sanction.type = 'SUSPENSION'
        AND sanction.status = 'ACTIVE'
        AND sanction.ends_at > now()
    )
  GROUP BY membership.polity_id
  HAVING count(*) >= 3
)
UPDATE polities polity
SET
  bootstrap_completed_at = now(),
  updated_at = now()
FROM full_government_polities full_government
WHERE polity.id = full_government.polity_id
  AND polity.bootstrap_completed_at IS NULL;

UPDATE office_terms term
SET
  status = 'ENDED',
  ended_at = polity.bootstrap_completed_at,
  updated_at = polity.bootstrap_completed_at
FROM polities polity
WHERE term.polity_id = polity.id
  AND polity.bootstrap_completed_at IS NOT NULL
  AND term.office_code = 'steward'
  AND term.assigned_by_motion_id IS NULL
  AND term.status = 'ACTIVE';
