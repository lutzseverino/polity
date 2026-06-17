CREATE TABLE sanction_proposals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  motion_id uuid NOT NULL,
  target_membership_id uuid NOT NULL,
  type text NOT NULL,
  reason text NOT NULL,
  duration_days integer NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, motion_id) REFERENCES motions(polity_id, id),
  FOREIGN KEY (polity_id, target_membership_id) REFERENCES memberships(polity_id, id),
  UNIQUE (motion_id),
  CHECK (type IN ('WARNING', 'SUSPENSION')),
  CHECK (duration_days > 0)
);

CREATE TABLE sanctions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  motion_id uuid NOT NULL UNIQUE,
  target_membership_id uuid NOT NULL,
  type text NOT NULL,
  status text NOT NULL,
  reason text NOT NULL,
  started_at timestamp with time zone NOT NULL,
  ends_at timestamp with time zone NOT NULL,
  vacated_at timestamp with time zone,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, motion_id) REFERENCES motions(polity_id, id),
  FOREIGN KEY (polity_id, target_membership_id) REFERENCES memberships(polity_id, id),
  UNIQUE (polity_id, id),
  CHECK (type IN ('WARNING', 'SUSPENSION')),
  CHECK (status IN ('ACTIVE', 'VACATED', 'EXPIRED')),
  CHECK (ends_at > started_at),
  CHECK ((status = 'VACATED' AND vacated_at IS NOT NULL) OR (status <> 'VACATED' AND vacated_at IS NULL))
);

CREATE TABLE appeal_proposals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  motion_id uuid NOT NULL,
  sanction_id uuid NOT NULL,
  appellant_membership_id uuid NOT NULL,
  reason text NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, motion_id) REFERENCES motions(polity_id, id),
  FOREIGN KEY (polity_id, sanction_id) REFERENCES sanctions(polity_id, id),
  FOREIGN KEY (polity_id, appellant_membership_id) REFERENCES memberships(polity_id, id),
  UNIQUE (motion_id)
);

CREATE TABLE appeals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  polity_id uuid NOT NULL REFERENCES polities(id),
  motion_id uuid NOT NULL UNIQUE,
  sanction_id uuid NOT NULL,
  appellant_membership_id uuid NOT NULL,
  status text NOT NULL,
  reason text NOT NULL,
  decided_at timestamp with time zone NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY (polity_id, motion_id) REFERENCES motions(polity_id, id),
  FOREIGN KEY (polity_id, sanction_id) REFERENCES sanctions(polity_id, id),
  FOREIGN KEY (polity_id, appellant_membership_id) REFERENCES memberships(polity_id, id),
  UNIQUE (polity_id, sanction_id),
  CHECK (status IN ('GRANTED'))
);

CREATE INDEX idx_sanctions_polity_status ON sanctions(polity_id, status);
CREATE INDEX idx_appeals_polity_decided ON appeals(polity_id, decided_at DESC);
