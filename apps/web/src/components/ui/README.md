# Registry-managed components

This directory is a read-only local mirror of components installed from the official shadcn registry.

- Do not edit component or hook source manually.
- Update components with the shadcn CLI and review the resulting replacement diff.
- Keep `components.json` aliases pointed at this directory.
- Application and feature code must use app-owned components from `../app/` instead of importing this
  directory directly.

The global theme remains preset-compatible and may be changed with `shadcn apply`. App-specific
behavior, composition, and product semantics belong outside this directory.
