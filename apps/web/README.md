# Polity Web

Responsive, web-first product client for Polity.

## Development

Run from the repository root:

```bash
pnpm dev:web
pnpm build:web
pnpm check:ts:architecture:web
```

## Structure

- `src/app/` owns providers, router construction, i18n runtime, and the application shell.
- `src/routes/` mirrors the URL tree and owns route params, search state, loaders, and page composition.
- `src/domains/` owns reusable product nouns such as polities, motions, memberships, and inbox items.
- `src/features/` owns reusable user actions such as launching an action, voting, or accepting an invitation.
- `src/components/app/` owns owner-neutral product components and shadcn wrappers.
- `src/components/ui/` is registry-managed shadcn source. It is strictly read-only.
- `src/api/` is reserved for generated clients and transport adapters; `src/lib/` owns low-level helpers.

The authoritative ownership and import rules are in
[Source architecture](docs/reference/source-architecture.md) and are checked with
`pnpm check:architecture`.

## Domain and feature convention

- Name domains with nouns and features with verb phrases.
- Give every domain and feature a narrow, manually curated `index.ts` with explicit exports; external
  consumers import only that entrypoint, while owner internals import implementation modules directly.
- Duplicate only the subfolders an owner earns: usually `components/`, `lib/`, and eventually `api/`.
- Keep route knowledge in `src/routes/`. A component owns navigation only when navigation is part of its
  explicit contract.
- Prefer composition and slots for reusable visual structures. Keep data loading, URL state, and mutations
  in their owning route or feature boundary.
- Do not create generic `widgets`, `entities`, or `shared` dumping grounds.

## Routing and localization

- TanStack Router's file routes under `src/routes/` mirror the URL tree. `src/routeTree.gen.ts` is generated;
  never edit it manually.
- User-facing source messages stay beside the code that owns them. Lingui catalogs mirror architectural
  owners under `locales/<locale>/` and compile into the app i18n runtime.
- Run `pnpm i18n:sync` after adding or moving source messages. English is the source locale; Spanish and the
  pseudo locale are already wired.
- The accepted rationale and tradeoffs are recorded in
  [Routing and localization](docs/decisions/routing-and-localization.md).

## Data access

- `src/api/` owns shared Axios mechanics; domains own reusable read operations and features own user-action
  requests.
- Owner-local TanStack query and mutation options are the reusable convention. Semantic hooks stay thin and
  no application query wrapper is introduced.
- Route loaders ensure critical query data before rendering, and components subscribe with the matching
  owner hook.
- Localized server data includes locale in its query key, while the HTTP client applies `Accept-Language`.
- The accepted rationale and migration boundary are recorded in
  [Data access and server state](docs/decisions/data-access-and-server-state.md).

## Typography convention

- Use `AppText` for standalone headings, paragraphs, captions, metrics, and definition-list text.
- Choose semantic markup with `as` independently from the visual `variant`; heading levels must follow
  the document outline rather than a desired font size.
- Typography variants own font size, weight, line height, tracking, and text tone.
- Call sites may add layout-only classes such as margin, width, truncation, alignment, or tabular
  numerals.
- Interactive controls and composite components own their internal typography. Do not wrap every
  button, badge, navigation label, alert title, or card title in `AppText`.
- Add a new variant only after a repeated application text role exists; do not add one to reproduce a
  single screen-specific class list.

Product, domain, feature, route, and shell code must consume shadcn primitives through
`src/components/app/`. Update registry
components only through the shadcn CLI so upstream replacements remain explicit and reviewable.

## Shadcn maintenance

Run registry commands from this workspace:

```bash
pnpm dlx shadcn@latest add --all --overwrite
pnpm dlx shadcn@latest preset resolve
pnpm dlx shadcn@latest apply <preset> --only theme
```

The registry source is intentionally excluded from repository formatting and ESLint rewrites. It is
still type-checked and included in dependency architecture validation.

## Documentation

Durable web app docs live in [docs](docs/README.md).
