# Registry-managed components

This directory is the read-only canonical mirror of components installed from
the official shadcn registry for every browser application.

- Do not edit component or hook source manually.
- Update components only with `pnpm --filter @polity/ui ui:add` or
  `pnpm --filter @polity/ui ui:update`, then review the replacement diff.
- Application code imports the public package entrypoints backed by `../app/`;
  this directory is not exported from `@polity/ui`.

App-specific behavior, composition, and product semantics belong outside this
directory.
