# Shell Context and Page Headings

## Status

Accepted

## Context

The application shell must feel native to each layout without creating separate mobile and desktop route
implementations. Compact layouts need a concise, contextual top bar, while wider layouts can expose more
navigation context. Route content still needs a stable semantic heading and may need explanatory copy that
does not belong in global navigation.

If shell labels, page titles, and descriptions are treated as one value, routes either repeat the same title
visually or force navigation-oriented copy into content. If each breakpoint owns a separate header, behavior
and copy drift between layouts.

## Decision

The shell and route content have separate, explicit responsibilities:

- Route `staticData.shell` owns navigation context: section state, compact top-bar labels, breadcrumbs, back
  targets, compact chrome, and global action availability. Dynamic entity labels come from route loader data.
- Route content owns its semantic `h1` through `AppPageHeader`. Eyebrows and supporting descriptions are
  optional route content, not required shell metadata.
- Page-heading copy uses product vocabulary and describes the content below it. Shell labels stay concise and
  navigational; they do not need to duplicate richer page copy.
- Compact layouts use the shell top bar as the visible title. The shell automatically hides `AppPageHeader`
  visually when the resolved route level is `root`, while keeping its semantic heading available to assistive
  technology. Wider layouts restore the same route-owned header to normal content flow.
- Shell level, rather than raw pathname segment count, defines hierarchy. Wider `root` layouts omit
  breadcrumbs; wider `workspace`, `detail`, and `task` layouts show them. Label-less child routes inherit the
  nearest contextual title instead of resetting the compact top bar to the section label.
- Workspace identity headers may remain visible in compact layouts when they add entity state, membership,
  switching, or local navigation. This is richer page context, not a second navigation title; plain title-only
  duplication remains hidden through the normal root-page rule.
- Routes do not choose responsive header visibility. They declare their semantic level through
  `staticData.shell`; `AppPageHeader` exposes a stable page-header slot, and the shell applies the presentation
  rule from those two existing contracts.
- Wider root layouts use the route-owned page heading and omit the redundant shell breadcrumb. Wider
  workspace, detail, and task layouts retain shell breadcrumbs because they add hierarchy beyond the page
  heading.
- A route has one content implementation across layouts. Responsive presentation belongs to the shell and
  shared header component rather than mobile-specific and desktop-specific route branches.

## Consequences

- Contextual top-bar behavior remains declarative and discoverable in each route definition.
- Every page retains one semantic level-one heading even when its visual title would be redundant on mobile.
- Root destinations have one visible title per layout rather than repeating the section label in shell and
  page content.
- Wider layouts can add breadcrumbs and global controls without changing route content.
- Supporting descriptions are written only when they help the user, so empty or generic filler copy is not
  part of the convention.
- New root routes inherit compact header behavior from their shell metadata rather than duplicating breakpoint
  rules at the call site.
- Every root destination, including planned placeholders, composes an `AppPageHeader`; cards and other content
  surfaces use distinct subordinate labels rather than acting as the page heading.

## Alternatives Considered

- Let the shell own the only page heading: this removes content duplication but couples document semantics
  and page-specific copy to global navigation infrastructure.
- Render separate mobile and desktop headers in every route: this permits complete visual freedom but
  duplicates markup, behavior, and copy ownership.
- Require route titles and descriptions entirely in route metadata: this centralizes strings but mixes
  navigation context with content semantics and makes rich route-owned composition harder.
