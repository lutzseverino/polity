ALTER TABLE procedures
  DROP CONSTRAINT chk_procedure_effect_type,
  ADD CONSTRAINT chk_procedure_effect_type
  CHECK (effect_type IN (
    'ADOPT_RESOLUTION',
    'ASSIGN_OFFICE',
    'ELECT_OFFICE',
    'APPLY_SANCTION',
    'GRANT_APPEAL',
    'AMEND_CONSTITUTION'
  ));

ALTER TABLE motions
  DROP CONSTRAINT chk_motion_effect_type,
  ADD CONSTRAINT chk_motion_effect_type
  CHECK (effect_type IN (
    'ADOPT_RESOLUTION',
    'ASSIGN_OFFICE',
    'ELECT_OFFICE',
    'APPLY_SANCTION',
    'GRANT_APPEAL',
    'AMEND_CONSTITUTION'
  ));

ALTER TABLE procedures
  DROP CONSTRAINT chk_procedure_threshold,
  ADD CONSTRAINT chk_procedure_threshold
  CHECK (threshold IN (
    'SIMPLE_MAJORITY_CAST',
    'MAJORITY_OF_ELIGIBLE',
    'TWO_THIRDS_CAST',
    'TWO_THIRDS_ELIGIBLE',
    'PLURALITY_CAST'
  ));

CREATE TABLE office_election_proposals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  motion_id uuid NOT NULL,
  office_id uuid NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, motion_id) REFERENCES motions(polity_id, id),
  FOREIGN KEY (polity_id, office_id) REFERENCES offices(polity_id, id),
  UNIQUE (motion_id)
);

CREATE TABLE office_election_candidates (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  motion_id uuid NOT NULL,
  membership_id uuid NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, motion_id) REFERENCES motions(polity_id, id),
  FOREIGN KEY (polity_id, membership_id) REFERENCES memberships(polity_id, id),
  UNIQUE (motion_id, membership_id)
);

CREATE TABLE office_election_ballots (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  motion_id uuid NOT NULL,
  membership_id uuid NOT NULL,
  candidate_membership_id uuid NOT NULL,
  cast_at timestamp with time zone NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, motion_id) REFERENCES motions(polity_id, id),
  FOREIGN KEY (polity_id, membership_id) REFERENCES memberships(polity_id, id),
  FOREIGN KEY (polity_id, candidate_membership_id) REFERENCES memberships(polity_id, id),
  FOREIGN KEY (motion_id, candidate_membership_id) REFERENCES office_election_candidates(motion_id, membership_id),
  UNIQUE (motion_id, membership_id)
);

INSERT INTO procedures (
  polity_id,
  constitution_version_id,
  institution_id,
  code,
  name,
  quorum_numerator,
  quorum_denominator,
  threshold,
  effect_type,
  minimum_notice_hours,
  voting_period_hours
)
SELECT
  constitution.polity_id,
  constitution.id,
  institution.id,
  'office-election',
  'Office election',
  COALESCE(assignment.quorum_numerator, 1),
  COALESCE(assignment.quorum_denominator, 2),
  'PLURALITY_CAST',
  'ELECT_OFFICE',
  COALESCE(assignment.minimum_notice_hours, 12),
  COALESCE(assignment.voting_period_hours, 48)
FROM constitution_versions constitution
JOIN institutions institution
  ON institution.polity_id = constitution.polity_id
  AND institution.constitution_version_id = constitution.id
  AND institution.kind = 'ASSEMBLY'
LEFT JOIN procedures assignment
  ON assignment.constitution_version_id = constitution.id
  AND assignment.code = 'office-assignment'
WHERE constitution.status = 'RATIFIED'
  AND NOT EXISTS (
    SELECT 1
    FROM procedures existing
    WHERE existing.constitution_version_id = constitution.id
      AND existing.code = 'office-election'
  );

INSERT INTO constitutional_powers (
  polity_id,
  constitution_version_id,
  code,
  name,
  holder_scope,
  holder_office_code
)
SELECT
  constitution.polity_id,
  constitution.id,
  'INTRODUCE_OFFICE_ELECTION',
  'Propose office elections',
  'ACTIVE_MEMBER',
  NULL
FROM constitution_versions constitution
WHERE constitution.status = 'RATIFIED'
  AND NOT EXISTS (
    SELECT 1
    FROM constitutional_powers existing
    WHERE existing.constitution_version_id = constitution.id
      AND existing.code = 'INTRODUCE_OFFICE_ELECTION'
  );

UPDATE constitutional_powers
SET holder_scope = 'ACTIVE_MEMBER',
    holder_office_code = NULL
WHERE code = 'REQUEST_CERTIFICATION';
