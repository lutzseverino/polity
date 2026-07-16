# Source Architecture

This reference is authoritative for source ownership, naming, and import direction in the web app.

## Structure

| Path | Owner | Contents |
| --- | --- | --- |
| `src/app/` | application | provider installation, router construction, i18n runtime, shell, route-state UI |
| `src/routes/` | URL tree | file routes, params, search validation, loaders, and page composition |
| `src/domains/<noun>/` | product noun | reusable domain types, read models, read operations/query options, fixtures/adapters, and noun-oriented components |
| `src/features/<verb>/` | user capability | reusable action UI, action state, validation, request operations, and mutation orchestration |
| `src/components/app/` | design system | owner-neutral product components and shadcn wrappers |
| `src/components/ui/` | shadcn registry | generated registry components and hooks; read-only |
| `src/api/` | transport | shared HTTP mechanics, generated OpenAPI clients, and transport contracts |
| `src/lib/` | infrastructure | small framework- and product-neutral helpers |

Create only the subfolders an owner needs. A domain or feature can start with `index.ts` and one
`components/` or `lib/` folder; it does not need an empty copy of the entire structure.

## Rules

- Name domains with nouns (`polity`, `motion`, `membership`) and features with verb phrases
  (`cast-vote`, `accept-invitation`).
- Mirror the public URL hierarchy in `src/routes/`, including dynamic segments such as `$polityId`.
- Keep route params, search state, loaders, redirects, and page composition in route modules.
- Export every domain and feature through a manually curated root `index.ts`. Name each value and type
  explicitly; do not use wildcard re-exports. Import another owner only through that entrypoint, while
  owner-internal modules import sibling implementation modules directly rather than their own root index.
- Domains may depend on app components, low-level libraries, API contracts, and another domain's public
  contract when the aggregate relationship is real. They never depend on features, routes, or app
  composition.
- Features may depend on domain public contracts, app components, API contracts, and low-level libraries.
  They never depend on routes or app composition. Avoid feature-to-feature dependencies; compose features
  in a route or the app shell instead.
- Only `src/components/app/` may import `src/components/ui/`. Never manually edit files under
  `src/components/ui/`. Follow the [app component wrapper rules](app-component-wrappers.md) when
  exposing or extending a registry primitive.
- Package reusable components in `ComponentName/ComponentName.tsx` with a manually curated local `index.ts`.
  Consumers outside that component import its index rather than the implementation file.
- Prefer a presentational component plus slots or a compound API when consumers need to vary content or
  actions. A reusable component should not acquire route, loader, or fixture knowledge merely for
  convenience. Domain components do not import the router; the route or feature composition boundary wraps
  them in navigable surfaces and supplies any directional action slot.
- Keep types with their owning domain or feature. Generated OpenAPI types remain in generated API output;
  add a domain type only when it expresses a UI/domain contract rather than copying transport output.
- Keep plain asynchronous reads in the owning domain's `api/` folder and user-action requests in the owning
  feature's `api/` folder. TanStack query or mutation options and semantic hooks stay beside those operations.
- Only `src/api/` imports Axios. Callers provide the active accepted language explicitly, and localized
  queries include locale in their query key.
- Do not introduce `pages`, `widgets`, `entities`, or top-level `shared` folders. Classify code by actual
  ownership instead.

`pnpm check:architecture` enforces the mechanically checkable part of this graph, including acyclic
dependencies, shadcn isolation, upward-import bans, domain routing ownership, and public entrypoints. The
legacy `InboxItemLink` route dependency is an explicit migration exception rather than a general precedent.

## Component Example

`PolityCard` belongs to the `polity` domain and exposes `Header`, `Identity`, `Content`, `Footer`, `Meta`,
`Title`, and `Description` parts. Consumers compose those parts with route-specific links and actions; the
card itself does not load data or know which URL owns a polity.
