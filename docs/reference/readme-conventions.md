# README Conventions

This reference is authoritative for README shape in this repository.

## Repository README

The root `README.md` is the only README that should use the centered repository header with badges by default.

Use it as a start page:

- one-line product or workspace identity
- badge row for stable repo metadata, including useful tech badges
- getting started commands
- quality commands
- links to the nearest durable documentation

Avoid using the root README as the long-form architecture document. Put durable concepts, rationale,
contracts, and decisions under `docs/` or the owning app/service `docs/` directory.

## Internal READMEs

Internal READMEs should be plain Markdown documents unless that directory is intentionally being shaped as
a future repository boundary.

Prefer this minimal shape:

- title
- one or two sentences naming what the area owns
- local development or validation commands when useful
- structure notes only when they help a contributor navigate the area
- links to the nearest Diataxis docs

Internal READMEs may differ because they answer different local questions. A feature asset directory,
an app, and a service do not need the same section list.

## Diataxis Docs

Durable docs belong in the nearest owner that will keep maintaining them:

- repo-level docs for cross-workspace conventions and navigation
- app docs for app behavior, workflows, and local implementation guidance
- service docs for service behavior, contracts, and architecture

Choose the section by reader intent:

- `tutorials/` for guided learning
- `how-to/` for focused tasks
- `reference/` for factual lookup material
- `explanation/` for concepts and rationale
- `decisions/` for durable choices and alternatives
