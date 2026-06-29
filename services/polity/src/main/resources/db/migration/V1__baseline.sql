CREATE EXTENSION IF NOT EXISTS pgcrypto;


SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

CREATE TABLE public.appeal_proposals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    sanction_id uuid NOT NULL,
    appellant_membership_id uuid NOT NULL,
    reason text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.appeals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    sanction_id uuid NOT NULL,
    appellant_membership_id uuid NOT NULL,
    status text NOT NULL,
    reason text NOT NULL,
    decided_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT appeals_status_check CHECK ((status = 'GRANTED'::text))
);

CREATE TABLE public.certifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    requested_by uuid NOT NULL,
    eligible_count integer NOT NULL,
    yes_count integer,
    no_count integer,
    abstain_count integer,
    quorum_required integer NOT NULL,
    quorum_met boolean NOT NULL,
    threshold_met boolean NOT NULL,
    passed boolean NOT NULL,
    outcome_reason text NOT NULL,
    certified_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    modality text DEFAULT 'YES_NO'::text NOT NULL,
    election_participation_count integer,
    election_decisive boolean,
    election_winner_membership_id uuid,
    election_winner_name text,
    CONSTRAINT certifications_outcome_reason_check CHECK ((outcome_reason = ANY (ARRAY['PASSED'::text, 'QUORUM_NOT_MET'::text, 'THRESHOLD_NOT_MET'::text, 'NO_DECISIVE_PLURALITY'::text]))),
    CONSTRAINT chk_certification_modality CHECK ((modality = ANY (ARRAY['YES_NO'::text, 'OFFICE_ELECTION'::text]))),
    CONSTRAINT chk_certification_yes_no_counts CHECK ((((modality = 'YES_NO'::text) AND (yes_count IS NOT NULL) AND (no_count IS NOT NULL) AND (abstain_count IS NOT NULL) AND (election_participation_count IS NULL) AND (election_decisive IS NULL) AND (election_winner_membership_id IS NULL) AND (election_winner_name IS NULL)) OR ((modality = 'OFFICE_ELECTION'::text) AND (yes_count IS NULL) AND (no_count IS NULL) AND (abstain_count IS NULL) AND (election_participation_count IS NOT NULL) AND (election_decisive IS NOT NULL) AND ((passed = false) OR (election_winner_membership_id IS NOT NULL)) AND ((passed = false) OR (election_winner_name IS NOT NULL)))))
);

CREATE TABLE public.constitution_amendment_proposals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    title text NOT NULL,
    body text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.constitution_office_change_proposals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    amendment_proposal_id uuid NOT NULL,
    action text NOT NULL,
    office_code text NOT NULL,
    jurisdiction_id uuid,
    name text,
    description text,
    term_length_days integer,
    seat_count integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT constitution_office_change_proposals_action_check CHECK ((action = ANY (ARRAY['CREATE'::text, 'REVISE'::text, 'RETIRE'::text]))),
    CONSTRAINT constitution_office_change_proposals_office_code_check CHECK ((office_code ~ '^[a-z][a-z0-9-]*$'::text)),
    CONSTRAINT constitution_office_change_proposals_seat_count_check CHECK (((seat_count IS NULL) OR (seat_count > 0))),
    CONSTRAINT constitution_office_change_proposals_term_length_days_check CHECK (((term_length_days IS NULL) OR (term_length_days > 0)))
);

CREATE TABLE public.constitution_institution_change_proposals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    amendment_proposal_id uuid NOT NULL,
    action text NOT NULL,
    institution_id uuid,
    jurisdiction_id uuid,
    name text,
    kind text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT constitution_institution_change_proposals_action_check CHECK ((action = ANY (ARRAY['CREATE'::text, 'REVISE'::text, 'RETIRE'::text]))),
    CONSTRAINT constitution_institution_change_proposals_kind_check CHECK (((kind IS NULL) OR (kind = ANY (ARRAY['ASSEMBLY'::text, 'JUDICIARY'::text])))),
    CONSTRAINT chk_constitution_institution_change_fields CHECK ((((action = 'CREATE'::text) AND (institution_id IS NULL) AND (jurisdiction_id IS NOT NULL) AND (name IS NOT NULL) AND (kind IS NOT NULL)) OR ((action = 'REVISE'::text) AND (institution_id IS NOT NULL) AND ((jurisdiction_id IS NOT NULL) OR (name IS NOT NULL) OR (kind IS NOT NULL))) OR ((action = 'RETIRE'::text) AND (institution_id IS NOT NULL) AND (jurisdiction_id IS NULL) AND (name IS NULL) AND (kind IS NULL))))
);

CREATE TABLE public.constitution_power_change_proposals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    amendment_proposal_id uuid NOT NULL,
    power_code text NOT NULL,
    holder_scope text NOT NULL,
    holder_office_code text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT constitution_power_change_proposals_check CHECK ((((holder_scope = 'OFFICE'::text) AND (holder_office_code IS NOT NULL)) OR ((holder_scope <> 'OFFICE'::text) AND (holder_office_code IS NULL)))),
    CONSTRAINT constitution_power_change_proposals_holder_office_code_check CHECK (((holder_office_code IS NULL) OR (holder_office_code ~ '^[a-z][a-z0-9-]*$'::text))),
    CONSTRAINT constitution_power_change_proposals_holder_scope_check CHECK ((holder_scope = ANY (ARRAY['ACTIVE_MEMBER'::text, 'OFFICE'::text])))
);

CREATE TABLE public.constitution_procedure_change_proposals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    amendment_proposal_id uuid NOT NULL,
    procedure_code text NOT NULL,
    institution_id uuid,
    quorum_numerator integer,
    quorum_denominator integer,
    threshold text,
    minimum_notice_hours integer,
    voting_period_hours integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    electorate text,
    electorate_office_code text,
    minimum_elector_count integer,
    CONSTRAINT chk_constitution_procedure_change_electorate CHECK (((electorate IS NULL) OR (electorate = ANY (ARRAY['ACTIVE_MEMBERS'::text, 'OFFICE_HOLDERS'::text])))),
    CONSTRAINT chk_constitution_procedure_change_electorate_office CHECK ((((electorate = 'OFFICE_HOLDERS'::text) AND (electorate_office_code IS NOT NULL)) OR ((electorate IS NULL) AND (electorate_office_code IS NULL)) OR ((electorate = 'ACTIVE_MEMBERS'::text) AND (electorate_office_code IS NULL)))),
    CONSTRAINT chk_constitution_procedure_change_electorate_office_code CHECK (((electorate_office_code IS NULL) OR (electorate_office_code ~ '^[a-z][a-z0-9-]*$'::text))),
    CONSTRAINT chk_constitution_procedure_change_minimum_elector_count CHECK (((minimum_elector_count IS NULL) OR (minimum_elector_count > 0))),
    CONSTRAINT constitution_procedure_change_propos_minimum_notice_hours_check CHECK (((minimum_notice_hours IS NULL) OR (minimum_notice_hours >= 0))),
    CONSTRAINT constitution_procedure_change_proposa_voting_period_hours_check CHECK (((voting_period_hours IS NULL) OR (voting_period_hours > 0))),
    CONSTRAINT constitution_procedure_change_proposals_check CHECK ((((quorum_numerator IS NULL) AND (quorum_denominator IS NULL)) OR ((quorum_numerator > 0) AND (quorum_denominator > 0) AND (quorum_numerator <= quorum_denominator)))),
    CONSTRAINT constitution_procedure_change_proposals_threshold_check CHECK (((threshold IS NULL) OR (threshold = ANY (ARRAY['SIMPLE_MAJORITY_CAST'::text, 'MAJORITY_OF_ELIGIBLE'::text, 'TWO_THIRDS_CAST'::text, 'TWO_THIRDS_ELIGIBLE'::text, 'PLURALITY_CAST'::text]))))
);

CREATE TABLE public.constitution_versions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    version integer NOT NULL,
    title text NOT NULL,
    body text NOT NULL,
    status text NOT NULL,
    ratified_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    title_key text,
    body_key text,
    template_params jsonb DEFAULT '{}'::jsonb NOT NULL,
    CONSTRAINT chk_constitution_status CHECK ((status = ANY (ARRAY['RATIFIED'::text, 'SUPERSEDED'::text])))
);

CREATE TABLE public.constitutional_powers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    constitution_version_id uuid NOT NULL,
    code text NOT NULL,
    name text NOT NULL,
    holder_scope text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    holder_office_code text,
    name_key text,
    CONSTRAINT chk_power_holder_office CHECK ((((holder_scope = 'OFFICE'::text) AND (holder_office_code IS NOT NULL)) OR ((holder_scope <> 'OFFICE'::text) AND (holder_office_code IS NULL)))),
    CONSTRAINT chk_power_holder_scope CHECK ((holder_scope = ANY (ARRAY['ACTIVE_MEMBER'::text, 'OFFICE'::text])))
);

CREATE TABLE public.constitutional_review_proposals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    target_record_id uuid NOT NULL,
    petitioner_membership_id uuid NOT NULL,
    reason text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.constitutional_reviews (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    target_record_id uuid NOT NULL,
    petitioner_membership_id uuid NOT NULL,
    status text NOT NULL,
    reason text NOT NULL,
    decided_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT constitutional_reviews_status_check CHECK ((status = 'GRANTED'::text))
);

CREATE TABLE public.institutions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    jurisdiction_id uuid NOT NULL,
    constitution_version_id uuid NOT NULL,
    name text NOT NULL,
    kind text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    name_key text,
    CONSTRAINT institutions_kind_check CHECK ((kind = ANY (ARRAY['ASSEMBLY'::text, 'JUDICIARY'::text])))
);

CREATE TABLE public.jurisdictions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    name text NOT NULL,
    kind text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT jurisdictions_kind_check CHECK ((kind = ANY (ARRAY['ROOT'::text])))
);

CREATE TABLE public.membership_invitations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    invited_user_id uuid NOT NULL,
    authorization_subject text NOT NULL,
    email text NOT NULL,
    invited_by uuid NOT NULL,
    status text NOT NULL,
    invited_at timestamp with time zone NOT NULL,
    responded_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT membership_invitations_status_check CHECK ((status = ANY (ARRAY['PENDING'::text, 'ACCEPTED'::text])))
);

CREATE TABLE public.memberships (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    user_id uuid NOT NULL,
    authorization_subject text NOT NULL,
    email text NOT NULL,
    display_name text NOT NULL,
    status text NOT NULL,
    admitted_at timestamp with time zone NOT NULL,
    admitted_by uuid,
    resigned_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT memberships_resigned_at_check CHECK ((((status = 'RESIGNED'::text) AND (resigned_at IS NOT NULL)) OR ((status = 'ACTIVE'::text) AND (resigned_at IS NULL)))),
    CONSTRAINT memberships_status_check CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'RESIGNED'::text])))
);

CREATE TABLE public.motion_electors (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    membership_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.motions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
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
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    voting_opens_at timestamp with time zone NOT NULL,
    voting_closes_at timestamp with time zone NOT NULL,
    certification_opens_at timestamp with time zone NOT NULL,
    title_key text,
    body_key text,
    template_params jsonb DEFAULT '{}'::jsonb NOT NULL,
    CONSTRAINT chk_motion_effect_type CHECK ((effect_type = ANY (ARRAY['ADOPT_RESOLUTION'::text, 'ELECT_OFFICE'::text, 'APPLY_SANCTION'::text, 'GRANT_APPEAL'::text, 'VACATE_OFFICE_TERM'::text, 'VOID_OFFICIAL_ACT'::text, 'AMEND_CONSTITUTION'::text, 'DISBAND_POLITY'::text]))),
    CONSTRAINT chk_motion_voting_window CHECK (((voting_opens_at >= opened_at) AND (voting_closes_at > voting_opens_at) AND (certification_opens_at >= voting_closes_at))),
    CONSTRAINT motion_template_check CHECK ((((title_key IS NULL) AND (body_key IS NULL)) OR ((title_key IS NOT NULL) AND (body_key IS NOT NULL))))
);

CREATE TABLE public.office_election_ballots (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    membership_id uuid NOT NULL,
    candidate_membership_id uuid NOT NULL,
    cast_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.office_election_candidates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    membership_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    status text DEFAULT 'ACCEPTED'::text NOT NULL,
    responded_at timestamp with time zone,
    CONSTRAINT office_election_candidate_status_check CHECK ((status = ANY (ARRAY['PENDING'::text, 'ACCEPTED'::text, 'DECLINED'::text, 'DISQUALIFIED'::text])))
);

CREATE TABLE public.office_election_proposals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    office_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.office_term_review_proposals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    office_term_id uuid NOT NULL,
    petitioner_membership_id uuid NOT NULL,
    reason text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.office_term_reviews (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    office_term_id uuid NOT NULL,
    petitioner_membership_id uuid NOT NULL,
    status text NOT NULL,
    reason text NOT NULL,
    decided_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT office_term_reviews_status_check CHECK ((status = 'GRANTED'::text))
);

CREATE TABLE public.office_terms (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    office_id uuid NOT NULL,
    office_code text NOT NULL,
    membership_id uuid NOT NULL,
    assigned_by_motion_id uuid,
    status text NOT NULL,
    started_at timestamp with time zone NOT NULL,
    ends_at timestamp with time zone NOT NULL,
    ended_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT office_terms_check CHECK ((ends_at > started_at)),
    CONSTRAINT office_terms_check1 CHECK ((((status = 'ENDED'::text) AND (ended_at IS NOT NULL)) OR ((status = 'ACTIVE'::text) AND (ended_at IS NULL)))),
    CONSTRAINT office_terms_status_check CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'ENDED'::text])))
);

CREATE TABLE public.offices (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    constitution_version_id uuid NOT NULL,
    jurisdiction_id uuid NOT NULL,
    code text NOT NULL,
    name text NOT NULL,
    description text NOT NULL,
    term_length_days integer NOT NULL,
    seat_count integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    name_key text,
    description_key text,
    CONSTRAINT offices_seat_count_check CHECK ((seat_count > 0)),
    CONSTRAINT offices_term_length_days_check CHECK ((term_length_days > 0))
);

CREATE TABLE public.official_record_entries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    jurisdiction_id uuid NOT NULL,
    constitution_version_id uuid NOT NULL,
    actor_membership_id uuid NOT NULL,
    type text NOT NULL,
    source_id uuid,
    title text NOT NULL,
    body text NOT NULL,
    occurred_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    motion_id uuid,
    procedure_id uuid,
    institution_id uuid,
    power_code text,
    certification_id uuid,
    effect_type text,
    outcome text,
    entry_number integer NOT NULL,
    title_key text,
    body_key text,
    template_params jsonb DEFAULT '{}'::jsonb NOT NULL,
    CONSTRAINT ck_record_entry_number_positive CHECK ((entry_number > 0)),
    CONSTRAINT official_record_template_check CHECK ((((title_key IS NULL) AND (body_key IS NULL)) OR ((title_key IS NOT NULL) AND (body_key IS NOT NULL))))
);

CREATE TABLE public.official_record_sequences (
    polity_id uuid NOT NULL,
    next_entry_number integer DEFAULT 1 NOT NULL,
    CONSTRAINT official_record_sequences_next_entry_number_check CHECK ((next_entry_number > 0))
);

CREATE TABLE public.polities (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    founder_id uuid NOT NULL,
    name text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    visibility text DEFAULT 'PRIVATE'::text NOT NULL,
    status text DEFAULT 'ACTIVE'::text NOT NULL,
    disbanded_at timestamp with time zone,
    bootstrap_completed_at timestamp with time zone,
    CONSTRAINT chk_polities_disbanded_at CHECK ((((status = 'ACTIVE'::text) AND (disbanded_at IS NULL)) OR ((status = 'DISBANDED'::text) AND (disbanded_at IS NOT NULL)))),
    CONSTRAINT chk_polities_status CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'DISBANDED'::text]))),
    CONSTRAINT chk_polities_visibility CHECK ((visibility = ANY (ARRAY['PUBLIC'::text, 'PRIVATE'::text])))
);

CREATE TABLE public.procedures (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    constitution_version_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    code text NOT NULL,
    name text NOT NULL,
    quorum_numerator integer NOT NULL,
    quorum_denominator integer NOT NULL,
    threshold text NOT NULL,
    effect_type text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    minimum_notice_hours integer DEFAULT 0 NOT NULL,
    voting_period_hours integer DEFAULT 24 NOT NULL,
    name_key text,
    electorate text DEFAULT 'ACTIVE_MEMBERS'::text NOT NULL,
    electorate_office_code text,
    minimum_elector_count integer DEFAULT 1 NOT NULL,
    CONSTRAINT chk_procedure_effect_type CHECK ((effect_type = ANY (ARRAY['ADOPT_RESOLUTION'::text, 'ELECT_OFFICE'::text, 'APPLY_SANCTION'::text, 'GRANT_APPEAL'::text, 'VACATE_OFFICE_TERM'::text, 'VOID_OFFICIAL_ACT'::text, 'AMEND_CONSTITUTION'::text, 'DISBAND_POLITY'::text]))),
    CONSTRAINT chk_procedure_electorate CHECK ((electorate = ANY (ARRAY['ACTIVE_MEMBERS'::text, 'OFFICE_HOLDERS'::text]))),
    CONSTRAINT chk_procedure_electorate_office CHECK ((((electorate = 'OFFICE_HOLDERS'::text) AND (electorate_office_code IS NOT NULL)) OR ((electorate <> 'OFFICE_HOLDERS'::text) AND (electorate_office_code IS NULL)))),
    CONSTRAINT chk_procedure_electorate_office_code CHECK (((electorate_office_code IS NULL) OR (electorate_office_code ~ '^[a-z][a-z0-9-]*$'::text))),
    CONSTRAINT chk_procedure_minimum_elector_count CHECK ((minimum_elector_count > 0)),
    CONSTRAINT chk_procedure_threshold CHECK ((threshold = ANY (ARRAY['SIMPLE_MAJORITY_CAST'::text, 'MAJORITY_OF_ELIGIBLE'::text, 'TWO_THIRDS_CAST'::text, 'TWO_THIRDS_ELIGIBLE'::text, 'PLURALITY_CAST'::text]))),
    CONSTRAINT chk_procedure_timing CHECK (((minimum_notice_hours >= 0) AND (voting_period_hours > 0))),
    CONSTRAINT procedures_check CHECK (((quorum_numerator > 0) AND (quorum_denominator > 0) AND (quorum_numerator <= quorum_denominator)))
);

CREATE TABLE public.resolutions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    title text NOT NULL,
    body text NOT NULL,
    status text DEFAULT 'ADOPTED'::text NOT NULL,
    adopted_at timestamp with time zone NOT NULL,
    voided_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_resolution_status CHECK ((status = ANY (ARRAY['ADOPTED'::text, 'VOIDED'::text]))),
    CONSTRAINT chk_resolution_voided_at CHECK ((((status = 'VOIDED'::text) AND (voided_at IS NOT NULL)) OR ((status <> 'VOIDED'::text) AND (voided_at IS NULL))))
);

CREATE TABLE public.sanction_proposals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    target_membership_id uuid NOT NULL,
    type text NOT NULL,
    reason text NOT NULL,
    duration_days integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT sanction_proposals_duration_days_check CHECK ((duration_days > 0)),
    CONSTRAINT sanction_proposals_type_check CHECK ((type = ANY (ARRAY['WARNING'::text, 'SUSPENSION'::text])))
);

CREATE TABLE public.sanctions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    target_membership_id uuid NOT NULL,
    type text NOT NULL,
    status text NOT NULL,
    reason text NOT NULL,
    started_at timestamp with time zone NOT NULL,
    ends_at timestamp with time zone NOT NULL,
    vacated_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT sanctions_check CHECK ((ends_at > started_at)),
    CONSTRAINT sanctions_check1 CHECK ((((status = 'VACATED'::text) AND (vacated_at IS NOT NULL)) OR ((status <> 'VACATED'::text) AND (vacated_at IS NULL)))),
    CONSTRAINT sanctions_status_check CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'VACATED'::text, 'EXPIRED'::text]))),
    CONSTRAINT sanctions_type_check CHECK ((type = ANY (ARRAY['WARNING'::text, 'SUSPENSION'::text])))
);

CREATE TABLE public.votes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    polity_id uuid NOT NULL,
    motion_id uuid NOT NULL,
    membership_id uuid NOT NULL,
    choice text NOT NULL,
    cast_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY public.appeal_proposals
    ADD CONSTRAINT appeal_proposals_motion_id_key UNIQUE (motion_id);

ALTER TABLE ONLY public.appeal_proposals
    ADD CONSTRAINT appeal_proposals_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_motion_id_key UNIQUE (motion_id);

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_polity_id_sanction_id_key UNIQUE (polity_id, sanction_id);

ALTER TABLE ONLY public.certifications
    ADD CONSTRAINT certifications_motion_id_key UNIQUE (motion_id);

ALTER TABLE ONLY public.certifications
    ADD CONSTRAINT certifications_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.constitution_amendment_proposals
    ADD CONSTRAINT constitution_amendment_proposals_motion_id_key UNIQUE (motion_id);

ALTER TABLE ONLY public.constitution_amendment_proposals
    ADD CONSTRAINT constitution_amendment_proposals_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.constitution_amendment_proposals
    ADD CONSTRAINT constitution_amendment_proposals_polity_id_id_key UNIQUE (polity_id, id);

ALTER TABLE ONLY public.constitution_office_change_proposals
    ADD CONSTRAINT constitution_office_change_pr_amendment_proposal_id_office__key UNIQUE (amendment_proposal_id, office_code);

ALTER TABLE ONLY public.constitution_office_change_proposals
    ADD CONSTRAINT constitution_office_change_proposals_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.constitution_institution_change_proposals
    ADD CONSTRAINT constitution_institution_change_pr_amendment_proposal_id_inst_key UNIQUE (amendment_proposal_id, institution_id);

ALTER TABLE ONLY public.constitution_institution_change_proposals
    ADD CONSTRAINT constitution_institution_change_proposals_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.constitution_power_change_proposals
    ADD CONSTRAINT constitution_power_change_pro_amendment_proposal_id_power_c_key UNIQUE (amendment_proposal_id, power_code);

ALTER TABLE ONLY public.constitution_power_change_proposals
    ADD CONSTRAINT constitution_power_change_proposals_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.constitution_procedure_change_proposals
    ADD CONSTRAINT constitution_procedure_change_amendment_proposal_id_procedu_key UNIQUE (amendment_proposal_id, procedure_code);

ALTER TABLE ONLY public.constitution_procedure_change_proposals
    ADD CONSTRAINT constitution_procedure_change_proposals_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.constitution_versions
    ADD CONSTRAINT constitution_versions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.constitution_versions
    ADD CONSTRAINT constitution_versions_polity_id_id_key UNIQUE (polity_id, id);

ALTER TABLE ONLY public.constitution_versions
    ADD CONSTRAINT constitution_versions_polity_id_version_key UNIQUE (polity_id, version);

ALTER TABLE ONLY public.constitutional_powers
    ADD CONSTRAINT constitutional_powers_constitution_version_id_code_key UNIQUE (constitution_version_id, code);

ALTER TABLE ONLY public.constitutional_powers
    ADD CONSTRAINT constitutional_powers_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.constitutional_review_proposals
    ADD CONSTRAINT constitutional_review_proposals_motion_id_key UNIQUE (motion_id);

ALTER TABLE ONLY public.constitutional_review_proposals
    ADD CONSTRAINT constitutional_review_proposals_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.constitutional_reviews
    ADD CONSTRAINT constitutional_reviews_motion_id_key UNIQUE (motion_id);

ALTER TABLE ONLY public.constitutional_reviews
    ADD CONSTRAINT constitutional_reviews_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.constitutional_reviews
    ADD CONSTRAINT constitutional_reviews_polity_id_target_record_id_key UNIQUE (polity_id, target_record_id);

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT institutions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT institutions_polity_id_id_key UNIQUE (polity_id, id);

ALTER TABLE ONLY public.jurisdictions
    ADD CONSTRAINT jurisdictions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.jurisdictions
    ADD CONSTRAINT jurisdictions_polity_id_id_key UNIQUE (polity_id, id);

ALTER TABLE ONLY public.membership_invitations
    ADD CONSTRAINT membership_invitations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.membership_invitations
    ADD CONSTRAINT membership_invitations_polity_id_id_key UNIQUE (polity_id, id);

ALTER TABLE ONLY public.memberships
    ADD CONSTRAINT memberships_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.memberships
    ADD CONSTRAINT memberships_polity_id_id_key UNIQUE (polity_id, id);

ALTER TABLE ONLY public.memberships
    ADD CONSTRAINT memberships_polity_id_user_id_key UNIQUE (polity_id, user_id);

ALTER TABLE ONLY public.motion_electors
    ADD CONSTRAINT motion_electors_motion_id_membership_id_key UNIQUE (motion_id, membership_id);

ALTER TABLE ONLY public.motion_electors
    ADD CONSTRAINT motion_electors_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.motions
    ADD CONSTRAINT motions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.motions
    ADD CONSTRAINT motions_polity_id_id_key UNIQUE (polity_id, id);

ALTER TABLE ONLY public.office_election_ballots
    ADD CONSTRAINT office_election_ballots_motion_id_membership_id_key UNIQUE (motion_id, membership_id);

ALTER TABLE ONLY public.office_election_ballots
    ADD CONSTRAINT office_election_ballots_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.office_election_candidates
    ADD CONSTRAINT office_election_candidates_motion_id_membership_id_key UNIQUE (motion_id, membership_id);

ALTER TABLE ONLY public.office_election_candidates
    ADD CONSTRAINT office_election_candidates_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.office_election_proposals
    ADD CONSTRAINT office_election_proposals_motion_id_key UNIQUE (motion_id);

ALTER TABLE ONLY public.office_election_proposals
    ADD CONSTRAINT office_election_proposals_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.office_term_review_proposals
    ADD CONSTRAINT office_term_review_proposals_motion_id_key UNIQUE (motion_id);

ALTER TABLE ONLY public.office_term_review_proposals
    ADD CONSTRAINT office_term_review_proposals_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.office_term_reviews
    ADD CONSTRAINT office_term_reviews_motion_id_key UNIQUE (motion_id);

ALTER TABLE ONLY public.office_term_reviews
    ADD CONSTRAINT office_term_reviews_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.office_term_reviews
    ADD CONSTRAINT office_term_reviews_polity_id_office_term_id_key UNIQUE (polity_id, office_term_id);

ALTER TABLE ONLY public.office_terms
    ADD CONSTRAINT office_terms_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.offices
    ADD CONSTRAINT offices_constitution_version_id_code_key UNIQUE (constitution_version_id, code);

ALTER TABLE ONLY public.offices
    ADD CONSTRAINT offices_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.offices
    ADD CONSTRAINT offices_polity_id_id_key UNIQUE (polity_id, id);

ALTER TABLE ONLY public.official_record_entries
    ADD CONSTRAINT official_record_entries_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.official_record_sequences
    ADD CONSTRAINT official_record_sequences_pkey PRIMARY KEY (polity_id);

ALTER TABLE ONLY public.polities
    ADD CONSTRAINT polities_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.procedures
    ADD CONSTRAINT procedures_constitution_version_id_code_key UNIQUE (constitution_version_id, code);

ALTER TABLE ONLY public.procedures
    ADD CONSTRAINT procedures_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.procedures
    ADD CONSTRAINT procedures_polity_id_id_key UNIQUE (polity_id, id);

ALTER TABLE ONLY public.resolutions
    ADD CONSTRAINT resolutions_motion_id_key UNIQUE (motion_id);

ALTER TABLE ONLY public.resolutions
    ADD CONSTRAINT resolutions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sanction_proposals
    ADD CONSTRAINT sanction_proposals_motion_id_key UNIQUE (motion_id);

ALTER TABLE ONLY public.sanction_proposals
    ADD CONSTRAINT sanction_proposals_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sanctions
    ADD CONSTRAINT sanctions_motion_id_key UNIQUE (motion_id);

ALTER TABLE ONLY public.sanctions
    ADD CONSTRAINT sanctions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sanctions
    ADD CONSTRAINT sanctions_polity_id_id_key UNIQUE (polity_id, id);

ALTER TABLE ONLY public.office_terms
    ADD CONSTRAINT uq_office_terms_polity_id_id UNIQUE (polity_id, id);

ALTER TABLE ONLY public.official_record_entries
    ADD CONSTRAINT uq_official_record_entries_polity_id_id UNIQUE (polity_id, id);

ALTER TABLE ONLY public.official_record_entries
    ADD CONSTRAINT uq_record_polity_entry_number UNIQUE (polity_id, entry_number);

ALTER TABLE ONLY public.votes
    ADD CONSTRAINT votes_motion_id_membership_id_key UNIQUE (motion_id, membership_id);

ALTER TABLE ONLY public.votes
    ADD CONSTRAINT votes_pkey PRIMARY KEY (id);

CREATE INDEX idx_appeals_polity_decided ON public.appeals USING btree (polity_id, decided_at DESC);

CREATE INDEX idx_constitutional_reviews_polity_decided ON public.constitutional_reviews USING btree (polity_id, decided_at DESC);

CREATE INDEX idx_membership_invitations_invitee ON public.membership_invitations USING btree (invited_user_id, status, invited_at DESC);

CREATE INDEX idx_membership_invitations_polity ON public.membership_invitations USING btree (polity_id, invited_at DESC);

CREATE INDEX idx_memberships_user ON public.memberships USING btree (user_id, status);

CREATE INDEX idx_motions_polity_opened ON public.motions USING btree (polity_id, opened_at DESC);

CREATE INDEX idx_office_term_reviews_polity_decided ON public.office_term_reviews USING btree (polity_id, decided_at DESC);

CREATE INDEX idx_office_terms_member ON public.office_terms USING btree (membership_id, status);

CREATE INDEX idx_offices_polity_constitution ON public.offices USING btree (polity_id, constitution_version_id);

CREATE INDEX idx_polities_status_created ON public.polities USING btree (status, created_at DESC);

CREATE INDEX idx_polities_visibility_created ON public.polities USING btree (visibility, created_at DESC);

CREATE INDEX idx_record_polity_occurred ON public.official_record_entries USING btree (polity_id, occurred_at DESC);

CREATE INDEX idx_sanctions_polity_status ON public.sanctions USING btree (polity_id, status);

CREATE UNIQUE INDEX uq_pending_membership_invitations_email ON public.membership_invitations USING btree (polity_id, lower(email)) WHERE (status = 'PENDING'::text);

CREATE UNIQUE INDEX uq_pending_membership_invitations_invitee ON public.membership_invitations USING btree (polity_id, invited_user_id) WHERE (status = 'PENDING'::text);

CREATE UNIQUE INDEX ux_constitutions_one_ratified ON public.constitution_versions USING btree (polity_id) WHERE (status = 'RATIFIED'::text);

CREATE INDEX idx_office_terms_office_status ON public.office_terms USING btree (polity_id, office_code, status);

ALTER TABLE ONLY public.appeal_proposals
    ADD CONSTRAINT appeal_proposals_polity_id_appellant_membership_id_fkey FOREIGN KEY (polity_id, appellant_membership_id) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.appeal_proposals
    ADD CONSTRAINT appeal_proposals_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.appeal_proposals
    ADD CONSTRAINT appeal_proposals_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.appeal_proposals
    ADD CONSTRAINT appeal_proposals_polity_id_sanction_id_fkey FOREIGN KEY (polity_id, sanction_id) REFERENCES public.sanctions(polity_id, id);

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_polity_id_appellant_membership_id_fkey FOREIGN KEY (polity_id, appellant_membership_id) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.appeals
    ADD CONSTRAINT appeals_polity_id_sanction_id_fkey FOREIGN KEY (polity_id, sanction_id) REFERENCES public.sanctions(polity_id, id);

ALTER TABLE ONLY public.certifications
    ADD CONSTRAINT certifications_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.certifications
    ADD CONSTRAINT certifications_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.certifications
    ADD CONSTRAINT certifications_polity_id_requested_by_fkey FOREIGN KEY (polity_id, requested_by) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.constitution_amendment_proposals
    ADD CONSTRAINT constitution_amendment_proposals_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.constitution_amendment_proposals
    ADD CONSTRAINT constitution_amendment_proposals_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.constitution_office_change_proposals
    ADD CONSTRAINT constitution_office_change_pr_polity_id_amendment_proposal_fkey FOREIGN KEY (polity_id, amendment_proposal_id) REFERENCES public.constitution_amendment_proposals(polity_id, id);

ALTER TABLE ONLY public.constitution_office_change_proposals
    ADD CONSTRAINT constitution_office_change_proposals_polity_id_jurisdiction_fkey FOREIGN KEY (polity_id, jurisdiction_id) REFERENCES public.jurisdictions(polity_id, id);

ALTER TABLE ONLY public.constitution_institution_change_proposals
    ADD CONSTRAINT constitution_institution_change_pr_polity_id_amendment_proposal_fkey FOREIGN KEY (polity_id, amendment_proposal_id) REFERENCES public.constitution_amendment_proposals(polity_id, id);

ALTER TABLE ONLY public.constitution_institution_change_proposals
    ADD CONSTRAINT constitution_institution_change_proposals_polity_id_institution_fkey FOREIGN KEY (polity_id, institution_id) REFERENCES public.institutions(polity_id, id);

ALTER TABLE ONLY public.constitution_institution_change_proposals
    ADD CONSTRAINT constitution_institution_change_proposals_polity_id_jurisdiction_fkey FOREIGN KEY (polity_id, jurisdiction_id) REFERENCES public.jurisdictions(polity_id, id);

ALTER TABLE ONLY public.constitution_power_change_proposals
    ADD CONSTRAINT constitution_power_change_pro_polity_id_amendment_proposal_fkey FOREIGN KEY (polity_id, amendment_proposal_id) REFERENCES public.constitution_amendment_proposals(polity_id, id);

ALTER TABLE ONLY public.constitution_procedure_change_proposals
    ADD CONSTRAINT constitution_procedure_change_polity_id_amendment_proposal_fkey FOREIGN KEY (polity_id, amendment_proposal_id) REFERENCES public.constitution_amendment_proposals(polity_id, id);

ALTER TABLE ONLY public.constitution_procedure_change_proposals
    ADD CONSTRAINT constitution_procedure_change_proposals_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.constitution_procedure_change_proposals
    ADD CONSTRAINT constitution_procedure_change_proposals_polity_id_institution_fkey FOREIGN KEY (polity_id, institution_id) REFERENCES public.institutions(polity_id, id);

ALTER TABLE ONLY public.constitution_versions
    ADD CONSTRAINT constitution_versions_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.constitutional_powers
    ADD CONSTRAINT constitutional_powers_polity_id_constitution_version_id_fkey FOREIGN KEY (polity_id, constitution_version_id) REFERENCES public.constitution_versions(polity_id, id);

ALTER TABLE ONLY public.constitutional_powers
    ADD CONSTRAINT constitutional_powers_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.constitutional_review_proposals
    ADD CONSTRAINT constitutional_review_proposa_polity_id_petitioner_members_fkey FOREIGN KEY (polity_id, petitioner_membership_id) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.constitutional_review_proposals
    ADD CONSTRAINT constitutional_review_proposals_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.constitutional_review_proposals
    ADD CONSTRAINT constitutional_review_proposals_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.constitutional_review_proposals
    ADD CONSTRAINT constitutional_review_proposals_polity_id_target_record_id_fkey FOREIGN KEY (polity_id, target_record_id) REFERENCES public.official_record_entries(polity_id, id);

ALTER TABLE ONLY public.constitutional_reviews
    ADD CONSTRAINT constitutional_reviews_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.constitutional_reviews
    ADD CONSTRAINT constitutional_reviews_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.constitutional_reviews
    ADD CONSTRAINT constitutional_reviews_polity_id_petitioner_membership_id_fkey FOREIGN KEY (polity_id, petitioner_membership_id) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.constitutional_reviews
    ADD CONSTRAINT constitutional_reviews_polity_id_target_record_id_fkey FOREIGN KEY (polity_id, target_record_id) REFERENCES public.official_record_entries(polity_id, id);

ALTER TABLE ONLY public.official_record_entries
    ADD CONSTRAINT fk_record_institution FOREIGN KEY (polity_id, institution_id) REFERENCES public.institutions(polity_id, id);

ALTER TABLE ONLY public.official_record_entries
    ADD CONSTRAINT fk_record_motion FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.official_record_entries
    ADD CONSTRAINT fk_record_procedure FOREIGN KEY (polity_id, procedure_id) REFERENCES public.procedures(polity_id, id);

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT institutions_polity_id_constitution_version_id_fkey FOREIGN KEY (polity_id, constitution_version_id) REFERENCES public.constitution_versions(polity_id, id);

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT institutions_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.institutions
    ADD CONSTRAINT institutions_polity_id_jurisdiction_id_fkey FOREIGN KEY (polity_id, jurisdiction_id) REFERENCES public.jurisdictions(polity_id, id);

ALTER TABLE ONLY public.jurisdictions
    ADD CONSTRAINT jurisdictions_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.membership_invitations
    ADD CONSTRAINT membership_invitations_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.membership_invitations
    ADD CONSTRAINT membership_invitations_polity_id_invited_by_fkey FOREIGN KEY (polity_id, invited_by) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.memberships
    ADD CONSTRAINT memberships_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.motion_electors
    ADD CONSTRAINT motion_electors_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.motion_electors
    ADD CONSTRAINT motion_electors_polity_id_membership_id_fkey FOREIGN KEY (polity_id, membership_id) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.motion_electors
    ADD CONSTRAINT motion_electors_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.motions
    ADD CONSTRAINT motions_polity_id_constitution_version_id_fkey FOREIGN KEY (polity_id, constitution_version_id) REFERENCES public.constitution_versions(polity_id, id);

ALTER TABLE ONLY public.motions
    ADD CONSTRAINT motions_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.motions
    ADD CONSTRAINT motions_polity_id_institution_id_fkey FOREIGN KEY (polity_id, institution_id) REFERENCES public.institutions(polity_id, id);

ALTER TABLE ONLY public.motions
    ADD CONSTRAINT motions_polity_id_introduced_by_fkey FOREIGN KEY (polity_id, introduced_by) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.motions
    ADD CONSTRAINT motions_polity_id_jurisdiction_id_fkey FOREIGN KEY (polity_id, jurisdiction_id) REFERENCES public.jurisdictions(polity_id, id);

ALTER TABLE ONLY public.motions
    ADD CONSTRAINT motions_polity_id_procedure_id_fkey FOREIGN KEY (polity_id, procedure_id) REFERENCES public.procedures(polity_id, id);

ALTER TABLE ONLY public.office_election_ballots
    ADD CONSTRAINT office_election_ballots_motion_id_candidate_membership_id_fkey FOREIGN KEY (motion_id, candidate_membership_id) REFERENCES public.office_election_candidates(motion_id, membership_id);

ALTER TABLE ONLY public.office_election_ballots
    ADD CONSTRAINT office_election_ballots_polity_id_candidate_membership_id_fkey FOREIGN KEY (polity_id, candidate_membership_id) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.office_election_ballots
    ADD CONSTRAINT office_election_ballots_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.office_election_ballots
    ADD CONSTRAINT office_election_ballots_polity_id_membership_id_fkey FOREIGN KEY (polity_id, membership_id) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.office_election_ballots
    ADD CONSTRAINT office_election_ballots_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.office_election_candidates
    ADD CONSTRAINT office_election_candidates_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.office_election_candidates
    ADD CONSTRAINT office_election_candidates_polity_id_membership_id_fkey FOREIGN KEY (polity_id, membership_id) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.office_election_candidates
    ADD CONSTRAINT office_election_candidates_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.office_election_proposals
    ADD CONSTRAINT office_election_proposals_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.office_election_proposals
    ADD CONSTRAINT office_election_proposals_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.office_election_proposals
    ADD CONSTRAINT office_election_proposals_polity_id_office_id_fkey FOREIGN KEY (polity_id, office_id) REFERENCES public.offices(polity_id, id);

ALTER TABLE ONLY public.office_term_review_proposals
    ADD CONSTRAINT office_term_review_proposals_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.office_term_review_proposals
    ADD CONSTRAINT office_term_review_proposals_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.office_term_review_proposals
    ADD CONSTRAINT office_term_review_proposals_polity_id_office_term_id_fkey FOREIGN KEY (polity_id, office_term_id) REFERENCES public.office_terms(polity_id, id);

ALTER TABLE ONLY public.office_term_review_proposals
    ADD CONSTRAINT office_term_review_proposals_polity_id_petitioner_membersh_fkey FOREIGN KEY (polity_id, petitioner_membership_id) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.office_term_reviews
    ADD CONSTRAINT office_term_reviews_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.office_term_reviews
    ADD CONSTRAINT office_term_reviews_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.office_term_reviews
    ADD CONSTRAINT office_term_reviews_polity_id_office_term_id_fkey FOREIGN KEY (polity_id, office_term_id) REFERENCES public.office_terms(polity_id, id);

ALTER TABLE ONLY public.office_term_reviews
    ADD CONSTRAINT office_term_reviews_polity_id_petitioner_membership_id_fkey FOREIGN KEY (polity_id, petitioner_membership_id) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.office_terms
    ADD CONSTRAINT office_terms_polity_id_assigned_by_motion_id_fkey FOREIGN KEY (polity_id, assigned_by_motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.office_terms
    ADD CONSTRAINT office_terms_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.office_terms
    ADD CONSTRAINT office_terms_polity_id_membership_id_fkey FOREIGN KEY (polity_id, membership_id) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.office_terms
    ADD CONSTRAINT office_terms_polity_id_office_id_fkey FOREIGN KEY (polity_id, office_id) REFERENCES public.offices(polity_id, id);

ALTER TABLE ONLY public.offices
    ADD CONSTRAINT offices_polity_id_constitution_version_id_fkey FOREIGN KEY (polity_id, constitution_version_id) REFERENCES public.constitution_versions(polity_id, id);

ALTER TABLE ONLY public.offices
    ADD CONSTRAINT offices_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.offices
    ADD CONSTRAINT offices_polity_id_jurisdiction_id_fkey FOREIGN KEY (polity_id, jurisdiction_id) REFERENCES public.jurisdictions(polity_id, id);

ALTER TABLE ONLY public.official_record_entries
    ADD CONSTRAINT official_record_entries_polity_id_actor_membership_id_fkey FOREIGN KEY (polity_id, actor_membership_id) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.official_record_entries
    ADD CONSTRAINT official_record_entries_polity_id_constitution_version_id_fkey FOREIGN KEY (polity_id, constitution_version_id) REFERENCES public.constitution_versions(polity_id, id);

ALTER TABLE ONLY public.official_record_entries
    ADD CONSTRAINT official_record_entries_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.official_record_entries
    ADD CONSTRAINT official_record_entries_polity_id_jurisdiction_id_fkey FOREIGN KEY (polity_id, jurisdiction_id) REFERENCES public.jurisdictions(polity_id, id);

ALTER TABLE ONLY public.official_record_sequences
    ADD CONSTRAINT official_record_sequences_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.procedures
    ADD CONSTRAINT procedures_polity_id_constitution_version_id_fkey FOREIGN KEY (polity_id, constitution_version_id) REFERENCES public.constitution_versions(polity_id, id);

ALTER TABLE ONLY public.procedures
    ADD CONSTRAINT procedures_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.procedures
    ADD CONSTRAINT procedures_polity_id_institution_id_fkey FOREIGN KEY (polity_id, institution_id) REFERENCES public.institutions(polity_id, id);

ALTER TABLE ONLY public.resolutions
    ADD CONSTRAINT resolutions_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.resolutions
    ADD CONSTRAINT resolutions_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.sanction_proposals
    ADD CONSTRAINT sanction_proposals_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.sanction_proposals
    ADD CONSTRAINT sanction_proposals_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.sanction_proposals
    ADD CONSTRAINT sanction_proposals_polity_id_target_membership_id_fkey FOREIGN KEY (polity_id, target_membership_id) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.sanctions
    ADD CONSTRAINT sanctions_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.sanctions
    ADD CONSTRAINT sanctions_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);

ALTER TABLE ONLY public.sanctions
    ADD CONSTRAINT sanctions_polity_id_target_membership_id_fkey FOREIGN KEY (polity_id, target_membership_id) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.votes
    ADD CONSTRAINT votes_polity_id_fkey FOREIGN KEY (polity_id) REFERENCES public.polities(id);

ALTER TABLE ONLY public.votes
    ADD CONSTRAINT votes_polity_id_membership_id_fkey FOREIGN KEY (polity_id, membership_id) REFERENCES public.memberships(polity_id, id);

ALTER TABLE ONLY public.votes
    ADD CONSTRAINT votes_polity_id_motion_id_fkey FOREIGN KEY (polity_id, motion_id) REFERENCES public.motions(polity_id, id);
