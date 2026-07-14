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
- Compact layouts use the shell top bar as the visible title. Root routes opt into
  `compactVisibility="hidden"`; `AppPageHeader` keeps the heading available to assistive technology and
  restores its visual presentation at the wider shell breakpoint.
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
- Routes must deliberately choose compact title visibility when shell and content wording would otherwise
  repeat.

## Alternatives Considered

- Let the shell own the only page heading: this removes content duplication but couples document semantics
  and page-specific copy to global navigation infrastructure.
- Render separate mobile and desktop headers in every route: this permits complete visual freedom but
  duplicates markup, behavior, and copy ownership.
- Require route titles and descriptions entirely in route metadata: this centralizes strings but mixes
  navigation context with content semantics and makes rich route-owned composition harder.
