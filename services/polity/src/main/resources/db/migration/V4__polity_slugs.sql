ALTER TABLE public.polities ADD COLUMN slug text;

DO $$
DECLARE
  polity_row record;
  base_slug text;
  candidate_slug text;
  suffix_number integer;
  suffix_text text;
BEGIN
  FOR polity_row IN
    SELECT id, name
    FROM public.polities
    ORDER BY id
  LOOP
    base_slug := trim(BOTH '-' FROM regexp_replace(
      translate(lower(replace(replace(polity_row.name, '''', ''), '’', '')),
        'áàäâãåéèëêíìïîóòöôõúùüûñçýÿ',
        'aaaaaaeeeeiiiiooooouuuuncyy'),
      '[^a-z0-9]+', '-', 'g'));
    IF base_slug = '' THEN
      base_slug := 'polity';
    END IF;
    base_slug := trim(TRAILING '-' FROM left(base_slug, 80));
    IF base_slug IN ('new', 'invitations', 'membership-invitations') THEN
      base_slug := base_slug || '-polity';
    END IF;

    candidate_slug := base_slug;
    suffix_number := 2;
    WHILE EXISTS (
      SELECT 1 FROM public.polities WHERE slug = candidate_slug
    ) LOOP
      suffix_text := '-' || suffix_number;
      candidate_slug := trim(
        TRAILING '-' FROM left(base_slug, 80 - length(suffix_text))
      ) || suffix_text;
      suffix_number := suffix_number + 1;
    END LOOP;

    UPDATE public.polities
    SET slug = candidate_slug
    WHERE id = polity_row.id;
  END LOOP;
END $$;

ALTER TABLE public.polities ALTER COLUMN slug SET NOT NULL;
ALTER TABLE public.polities ADD CONSTRAINT polities_slug_key UNIQUE (slug);
ALTER TABLE public.polities ADD CONSTRAINT chk_polities_slug
  CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$' AND length(slug) <= 80);
