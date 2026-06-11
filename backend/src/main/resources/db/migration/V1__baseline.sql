CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE polities (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  founder_id uuid NOT NULL,
  name text NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TABLE jurisdictions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  name text NOT NULL,
  kind text NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  UNIQUE (polity_id, id)
);

CREATE TABLE constitution_versions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  version integer NOT NULL,
  title text NOT NULL,
  body text NOT NULL,
  status text NOT NULL,
  ratified_at timestamp with time zone NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  UNIQUE (polity_id, version),
  UNIQUE (polity_id, id)
);

CREATE TABLE institutions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  jurisdiction_id uuid NOT NULL,
  constitution_version_id uuid NOT NULL,
  name text NOT NULL,
  kind text NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, jurisdiction_id) REFERENCES jurisdictions(polity_id, id),
  FOREIGN KEY (polity_id, constitution_version_id) REFERENCES constitution_versions(polity_id, id),
  UNIQUE (polity_id, id)
);

CREATE TABLE procedures (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  constitution_version_id uuid NOT NULL,
  institution_id uuid NOT NULL,
  code text NOT NULL,
  name text NOT NULL,
  quorum_numerator integer NOT NULL,
  quorum_denominator integer NOT NULL,
  threshold text NOT NULL,
  effect_type text NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, constitution_version_id) REFERENCES constitution_versions(polity_id, id),
  FOREIGN KEY (polity_id, institution_id) REFERENCES institutions(polity_id, id),
  UNIQUE (constitution_version_id, code),
  UNIQUE (polity_id, id),
  CHECK (quorum_numerator > 0 AND quorum_denominator > 0 AND quorum_numerator <= quorum_denominator)
);

CREATE TABLE constitutional_powers (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  constitution_version_id uuid NOT NULL,
  code text NOT NULL,
  name text NOT NULL,
  holder_scope text NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, constitution_version_id) REFERENCES constitution_versions(polity_id, id),
  UNIQUE (constitution_version_id, code)
);

CREATE TABLE memberships (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  user_id uuid NOT NULL,
  authorization_subject text NOT NULL,
  email text NOT NULL,
  display_name text NOT NULL,
  status text NOT NULL,
  admitted_at timestamp with time zone NOT NULL,
  admitted_by uuid,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  UNIQUE (polity_id, user_id),
  UNIQUE (polity_id, id)
);

CREATE TABLE motions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  jurisdiction_id uuid NOT NULL,
  institution_id uuid NOT NULL,
  constitution_version_id uuid NOT NULL,
  procedure_id uuid NOT NULL,
  introduced_by uuid NOT NULL,
  title text NOT NULL,
  body text NOT NULL,
  status text NOT NULL,
  effect_type text NOT NULL,
  opened_at timestamp with time zone NOT NULL,
  certified_at timestamp with time zone,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, jurisdiction_id) REFERENCES jurisdictions(polity_id, id),
  FOREIGN KEY (polity_id, institution_id) REFERENCES institutions(polity_id, id),
  FOREIGN KEY (polity_id, constitution_version_id) REFERENCES constitution_versions(polity_id, id),
  FOREIGN KEY (polity_id, procedure_id) REFERENCES procedures(polity_id, id),
  FOREIGN KEY (polity_id, introduced_by) REFERENCES memberships(polity_id, id),
  UNIQUE (polity_id, id)
);

CREATE TABLE votes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  motion_id uuid NOT NULL,
  membership_id uuid NOT NULL,
  choice text NOT NULL,
  cast_at timestamp with time zone NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, motion_id) REFERENCES motions(polity_id, id),
  FOREIGN KEY (polity_id, membership_id) REFERENCES memberships(polity_id, id),
  UNIQUE (motion_id, membership_id)
);

CREATE TABLE motion_electors (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  motion_id uuid NOT NULL,
  membership_id uuid NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, motion_id) REFERENCES motions(polity_id, id),
  FOREIGN KEY (polity_id, membership_id) REFERENCES memberships(polity_id, id),
  UNIQUE (motion_id, membership_id)
);

CREATE TABLE certifications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  motion_id uuid NOT NULL UNIQUE,
  requested_by uuid NOT NULL,
  eligible_count integer NOT NULL,
  yes_count integer NOT NULL,
  no_count integer NOT NULL,
  abstain_count integer NOT NULL,
  quorum_required integer NOT NULL,
  quorum_met boolean NOT NULL,
  threshold_met boolean NOT NULL,
  passed boolean NOT NULL,
  explanation text NOT NULL,
  certified_at timestamp with time zone NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, motion_id) REFERENCES motions(polity_id, id),
  FOREIGN KEY (polity_id, requested_by) REFERENCES memberships(polity_id, id)
);

CREATE TABLE resolutions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  motion_id uuid NOT NULL UNIQUE,
  title text NOT NULL,
  body text NOT NULL,
  adopted_at timestamp with time zone NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, motion_id) REFERENCES motions(polity_id, id)
);

CREATE TABLE official_record_entries (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  jurisdiction_id uuid NOT NULL,
  constitution_version_id uuid NOT NULL,
  actor_membership_id uuid NOT NULL,
  type text NOT NULL,
  source_id uuid,
  title text NOT NULL,
  body text NOT NULL,
  occurred_at timestamp with time zone NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, jurisdiction_id) REFERENCES jurisdictions(polity_id, id),
  FOREIGN KEY (polity_id, constitution_version_id) REFERENCES constitution_versions(polity_id, id),
  FOREIGN KEY (polity_id, actor_membership_id) REFERENCES memberships(polity_id, id)
);

CREATE INDEX idx_memberships_user ON memberships(user_id, status);
CREATE INDEX idx_motions_polity_opened ON motions(polity_id, opened_at DESC);
CREATE INDEX idx_record_polity_occurred ON official_record_entries(polity_id, occurred_at DESC);
