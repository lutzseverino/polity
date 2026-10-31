# Page Layout Measures

## Status

Accepted

## Context

The application shell provides every route with the same responsive gutters and maximum canvas width. Route
content still needs different readable measures: directories and workspaces need the full canvas, ordinary
single-column pages need a moderate measure, focused tasks need a tighter measure, and confirmation flows
benefit from the narrowest measure.

Those differences were previously expressed as repeated `mx-auto`, `max-w-*`, and `space-y-*` classes at page
roots. The values broadly matched page purpose, but their ownership and alignment were implicit. Margin-based
page stacking also left residual space when the shell visually hid a root page header in compact layouts.

## Decision

Page layout has three ownership levels:

- `AppShell` owns global responsive gutters and the maximum application canvas.
- `AppPageLayout` owns page-root measure, alignment, and the standard top-level content gap.
- Route content owns local constraints for prose, cards, sidebars, and nested workspace sections.

Every page root selects one explicit semantic measure:

- `wide` uses the full shell canvas for directories, grids, dashboards, and workspaces.
- `standard` centers ordinary single-column destinations at `max-w-4xl`.
- `focused` centers tasks and low-density destinations at `max-w-3xl`.
- `narrow` centers card-dominant confirmation flows at `max-w-2xl`.

`AppPageLayout` uses a column flex layout with `gap-6` for top-level rhythm. When the shell makes an
`AppPageHeader` screen-reader-only, that absolutely positioned header leaves normal layout flow and does not
produce a phantom content gap.

Page measure is independent of `staticData.shell.level`. Route level describes navigation depth; page measure
describes content density. Nested workspace content may use a local maximum width without changing the
workspace's `wide` page measure.

## Consequences

- New page roots choose a named content measure instead of copying Tailwind width and centering classes.
- Similar page types remain aligned as shell gutters or breakpoint behavior evolve.
- Compact root destinations do not retain spacing for visually hidden page headings.
- A page can still contain narrower local regions when readability requires them.
- Adding or changing a measure affects multiple routes and must be treated as a design-system convention
  change rather than a call-site tweak.

## Alternatives Considered

- Infer measure from shell route level: rejected because navigation depth and content density are independent.
- Keep raw width utilities at route call sites: rejected because the repeated values did not communicate page
  purpose and allowed alignment to drift.
- Use one measure for every page: rejected because grids, workspaces, tasks, and confirmations have materially
  different space requirements.
