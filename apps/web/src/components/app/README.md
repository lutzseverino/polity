# App-owned components

This directory owns conventional wrappers and reusable product components composed from the
registry-managed primitives in `../ui/`.

Create a component here only when the application needs shared behavior, semantics, or composition.
Domain and feature presentation stays with its owner. Package each app component in a named folder,
export it through that folder's `index.ts`, and keep registry primitives untouched.
