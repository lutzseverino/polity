CREATE INDEX idx_sanctions_active_target
    ON public.sanctions (polity_id, target_membership_id, type, ends_at)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_office_terms_active_membership
    ON public.office_terms (polity_id, membership_id, ends_at)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_office_terms_active_office
    ON public.office_terms (polity_id, office_code, ends_at)
    WHERE status = 'ACTIVE';
