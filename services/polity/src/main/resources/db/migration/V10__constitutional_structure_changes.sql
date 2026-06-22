ALTER TABLE constitution_amendment_proposals
  RENAME COLUMN procedure_change_summary TO change_summary;

CREATE TABLE constitution_office_change_proposals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL,
  amendment_proposal_id uuid NOT NULL,
  action text NOT NULL,
  office_code text NOT NULL,
  name text,
  description text,
  term_length_days int,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, amendment_proposal_id) REFERENCES constitution_amendment_proposals(polity_id, id),
  UNIQUE (amendment_proposal_id, office_code),
  CHECK (action IN ('CREATE', 'REVISE', 'RETIRE')),
  CHECK (office_code ~ '^[a-z][a-z0-9-]*$'),
  CHECK (term_length_days IS NULL OR term_length_days > 0)
);

CREATE TABLE constitution_power_change_proposals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL,
  amendment_proposal_id uuid NOT NULL,
  power_code text NOT NULL,
  holder_scope text NOT NULL,
  holder_office_code text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, amendment_proposal_id) REFERENCES constitution_amendment_proposals(polity_id, id),
  UNIQUE (amendment_proposal_id, power_code),
  CHECK (holder_scope IN ('ACTIVE_MEMBER', 'OFFICE')),
  CHECK (
    (holder_scope = 'OFFICE' AND holder_office_code IS NOT NULL)
    OR (holder_scope <> 'OFFICE' AND holder_office_code IS NULL)
  ),
  CHECK (holder_office_code IS NULL OR holder_office_code ~ '^[a-z][a-z0-9-]*$')
);
