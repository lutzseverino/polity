CREATE TABLE offices (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  constitution_version_id uuid NOT NULL,
  jurisdiction_id uuid NOT NULL,
  code text NOT NULL,
  name text NOT NULL,
  description text NOT NULL,
  term_length_days integer NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, constitution_version_id) REFERENCES constitution_versions(polity_id, id),
  FOREIGN KEY (polity_id, jurisdiction_id) REFERENCES jurisdictions(polity_id, id),
  UNIQUE (constitution_version_id, code),
  UNIQUE (polity_id, id),
  CHECK (term_length_days > 0)
);

CREATE TABLE office_terms (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  office_id uuid NOT NULL,
  office_code text NOT NULL,
  membership_id uuid NOT NULL,
  assigned_by_motion_id uuid,
  status text NOT NULL,
  started_at timestamp with time zone NOT NULL,
  ends_at timestamp with time zone NOT NULL,
  ended_at timestamp with time zone,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, office_id) REFERENCES offices(polity_id, id),
  FOREIGN KEY (polity_id, membership_id) REFERENCES memberships(polity_id, id),
  FOREIGN KEY (polity_id, assigned_by_motion_id) REFERENCES motions(polity_id, id),
  CHECK (status IN ('ACTIVE', 'ENDED')),
  CHECK (ends_at > started_at),
  CHECK ((status = 'ENDED' AND ended_at IS NOT NULL) OR (status = 'ACTIVE' AND ended_at IS NULL))
);

CREATE UNIQUE INDEX ux_office_terms_one_active
  ON office_terms(polity_id, office_code)
  WHERE status = 'ACTIVE';

CREATE TABLE office_assignment_proposals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  motion_id uuid NOT NULL,
  office_id uuid NOT NULL,
  nominee_membership_id uuid NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, motion_id) REFERENCES motions(polity_id, id),
  FOREIGN KEY (polity_id, office_id) REFERENCES offices(polity_id, id),
  FOREIGN KEY (polity_id, nominee_membership_id) REFERENCES memberships(polity_id, id),
  UNIQUE (motion_id)
);

CREATE INDEX idx_offices_polity_constitution ON offices(polity_id, constitution_version_id);
CREATE INDEX idx_office_terms_member ON office_terms(membership_id, status);
