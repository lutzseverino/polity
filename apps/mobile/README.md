# Decreos Mobile

Expo app for the native Decreos product experience.

## Development

Run from the repository root:

```bash
pnpm dev:mobile
npm run dev:mobile
```

Or run inside this package:

```bash
pnpm start
pnpm ios
pnpm android
pnpm web

npm run start
npm run ios
npm run android
npm run web
```

## Structure

- `src/app/` owns Expo Router routes and should stay thin.
- `src/features/` owns product feature screens, hooks, and local logic.
- `src/components/ui/` owns reusable native UI primitives.
- `src/design/` owns native design tokens and theme helpers.
