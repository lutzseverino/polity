# Routing and Localization

## Status

Accepted

## Context

The app is expected to grow into a deep polity workspace with reusable domain components and user actions.
The previous page-oriented folders did not represent the URL hierarchy, and centralized translation files
would become high-conflict, ownerless files as more screens and contributors arrive.

## Decision

Use TanStack Router v1 with file-based routes under `src/routes/`. The folder tree mirrors the public URL,
route modules own URL state and data-loading boundaries, and `src/routeTree.gen.ts` remains generated and
read-only. Shared components use typed TanStack links only when navigation is an explicit part of their API;
otherwise routes provide link or action slots.

Use Lingui v6 for localization. Source messages are colocated with their owning components, domains,
features, routes, or app infrastructure. PO catalogs mirror those architectural owners under
`locales/<locale>/`:

- one catalog for `app` and one for `components/app`
- one catalog per domain and feature
- one catalog per top-level route branch

Catalogs are intentionally not split per component: owner-level files remain discoverable without creating
hundreds of tiny translation files. Lingui compiles all owner catalogs into one runtime catalog per locale.
English is the source and fallback locale, Spanish is the first product locale, and a pseudo locale supports
layout and missing-localization testing.

## Consequences

- URL ownership is visible from the filesystem and route params/search are type-safe.
- Moving a message between owners moves its catalog ownership on the next extraction.
- Contributors edit small, predictable PO files instead of a global translation file.
- Route and message trees are generated during normal validation and builds; generated outputs must not be
  edited manually.
- Spanish displays English fallback text until individual messages are translated.
- Domain components that explicitly navigate depend on the registered router types; fully presentational
  structures remain router-independent through slots and composition.

## Alternatives Considered

- React Router data routes: dependable, but TanStack Router provides a clearer file-tree convention and
  stronger route/search typing for this project's expected depth.
- A single locale file per language: initially simpler, but it creates an ownerless, high-conflict file.
- One catalog per component: precise ownership, but too fragmented for translation review and component
  renames. Owner-level catalogs provide the better maintenance boundary.
- Catalogs that duplicate every source folder: mechanically exact, but too sensitive to internal refactors;
  architectural owners are the stable unit.
