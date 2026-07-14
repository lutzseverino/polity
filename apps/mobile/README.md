# Polity Mobile

Dormant Expo shell reserved for a future native Polity product experience. Product development is
currently web-first; this workspace intentionally contains no product features or shared design-system
dependency.

## Development

Run from the repository root:

```bash
pnpm dev:mobile
```

Or run inside this package:

```bash
pnpm start
pnpm ios
pnpm android
pnpm web
```

## Scope

- `src/app/` owns the minimum Expo Router shell.
- Product features and design dependencies should not be added until native development resumes.
- Shared packages should be introduced only after both clients demonstrate stable, genuine reuse.

## Documentation

Durable mobile docs live in [docs](docs/README.md).
