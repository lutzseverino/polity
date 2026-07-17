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

- Name domains with canonical backend nouns (`polity`, `motion`, `membership`) and features with
  explicit verb phrases (`cast-motion-vote`, `accept-membership-invitation`). Do not introduce a
  frontend synonym when the backend already owns the concept: for example, code uses
  `OfficeElectionCandidacy`, even when product copy explains that concept as a nomination.
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
- Route ownership is transitive: domains and features cannot hide concrete URLs behind router-aware app
  components such as `AppLinkButton` or `AppLinkSurface`. Reusable owners expose link/action render props or
  slots; routes and app composition supply the router-aware wrapper and destination.
- Only `src/components/app/` may import `src/components/ui/`. Never manually edit files under
  `src/components/ui/`. Follow the [app component wrapper rules](app-component-wrappers.md) when
  exposing or extending a registry primitive.
- Package reusable components in `ComponentName/ComponentName.tsx` with a manually curated local `index.ts`.
  Consumers outside that component import its index rather than the implementation file.
- Prefer a presentational component plus slots or a compound API when consumers need to vary content or
  actions. A reusable component should not acquire route, loader, or fixture knowledge merely for
  convenience. Domain components do not import the router; the route or feature composition boundary wraps
  them in navigable surfaces and supplies any directional action slot.
- Name domain presenters for the representation they expose: `<Noun>Summary` for compact list/feed
  representations and `<Noun>Details` for fuller read-only representations. Do not suffix a presenter with
  its current primitive (`Card`, `Panel`) unless the primitive itself is the reusable contract, as it is for
  the compound `PolityCard` API.
- Name mutation-owning feature components `<Verb><CanonicalNoun>Workflow`. A workflow may own action state,
  mutation orchestration, and owner-specific success UI, but it receives the domain object it renders and
  never loads route data. Put a reusable read-only representation in its domain and compose it from the
  workflow. Name a side-effect-free input surface `<Verb><CanonicalNoun>Form`; a form receives values and
  callbacks and does not invoke a mutation hook.
- Routes and app-shell task boundaries own params, reads, navigation, dialogs/pages, and workflow
  composition. The dependency direction is `route/app shell -> workflow -> domain presenter`; the domain
  presenter never imports the feature.
- Keep types with their owning domain or feature. Generated OpenAPI types remain in generated API output;
  add a domain type only when it expresses a UI/domain contract rather than copying transport output.
- Keep plain asynchronous reads in the owning domain's `api/` folder and user-action requests in the owning
  feature's `api/` folder. TanStack query or mutation options and semantic hooks stay beside those operations.
- Only `src/api/` imports Axios. Callers provide the active accepted language explicitly, and localized
  queries include locale in their query key.
- Do not introduce `pages`, `widgets`, `entities`, or top-level `shared` folders. Classify code by actual
  ownership instead.

`pnpm check:architecture` enforces the mechanically checkable part of this graph, including acyclic
dependencies, shadcn isolation, upward-import bans, transitive domain and feature routing ownership, and
public entrypoints.

## Component Example

`PolityCard` belongs to the `polity` domain and exposes `Header`, `Identity`, `Content`, `Footer`, `Meta`,
`Title`, and `Description` parts. Consumers compose those parts with route-specific links and actions; the
card itself does not load data or know which URL owns a polity.

The current canonical examples are:

| Concern | Owner and name | Responsibility |
| --- | --- | --- |
| Compact motion representation | `domains/motion/MotionSummary` | renders one `Motion` and an optional action slot |
| Membership-invitation representation | `domains/membership/MembershipInvitationDetails` | renders one `MembershipInvitation` without action state |
| Invitation acceptance | `features/accept-membership-invitation/AcceptMembershipInvitationWorkflow` | accepts a supplied invitation and owns the acceptance mutation |
| Secret-token onboarding | `features/onboard-membership-invitation/OnboardMembershipInvitationWorkflow` | requests and polls Cardo-owned passwordless identity completion without accepting membership |
| Motion voting | `features/cast-motion-vote/CastMotionVoteWorkflow` | owns vote selection and mutation state for a supplied motion |
| Election candidacy response | `features/respond-office-election-candidacy/RespondOfficeElectionCandidacyWorkflow` | owns candidacy-response mutation state for a supplied motion |
| Action discovery | `features/launch-action/ActionLauncher` | owns action filtering and availability state while consumers supply route-owned action and empty-state link renderers |
