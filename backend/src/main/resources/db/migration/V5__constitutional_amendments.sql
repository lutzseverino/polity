CREATE TABLE constitution_amendment_proposals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  motion_id uuid NOT NULL,
  title text NOT NULL,
  body text NOT NULL,
  procedure_change_summary text NOT NULL DEFAULT '',
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, motion_id) REFERENCES motions(polity_id, id),
  UNIQUE (polity_id, id),
  UNIQUE (motion_id)
);

CREATE TABLE constitution_procedure_change_proposals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  amendment_proposal_id uuid NOT NULL,
  procedure_code text NOT NULL,
  quorum_numerator integer,
  quorum_denominator integer,
  threshold text,
  minimum_notice_hours integer,
  voting_period_hours integer,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, amendment_proposal_id) REFERENCES constitution_amendment_proposals(polity_id, id),
  UNIQUE (amendment_proposal_id, procedure_code),
  CHECK (
    (quorum_numerator IS NULL AND quorum_denominator IS NULL)
    OR (quorum_numerator > 0 AND quorum_denominator > 0 AND quorum_numerator <= quorum_denominator)
  ),
  CHECK (threshold IS NULL OR threshold IN (
    'SIMPLE_MAJORITY_CAST',
    'MAJORITY_OF_ELIGIBLE',
    'TWO_THIRDS_CAST',
    'TWO_THIRDS_ELIGIBLE'
  )),
  CHECK (minimum_notice_hours IS NULL OR minimum_notice_hours >= 0),
  CHECK (voting_period_hours IS NULL OR voting_period_hours > 0)
);
