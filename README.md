<div align="center">
    <h1 align="center">Polity</h1>
    <p>A product workspace for constitutional government software.</p>
    <p>
        <img alt="workspace" src="https://img.shields.io/badge/workspace-product-0f172a">
        <img alt="landing" src="https://img.shields.io/badge/landing-vite-111827">
        <img alt="mobile" src="https://img.shields.io/badge/mobile-expo-111827">
        <img alt="service" src="https://img.shields.io/badge/service-spring_boot-1f2937">
    </p>
</div>

## Overview

Polity is a friend-group government platform built around an explicit constitutional kernel.
It owns membership, versioned constitutions, institutions, powers, procedures, motions, voting,
certification, typed effects, and the official record.

The repository contains a Vite landing app, an Expo mobile app, shared design tokens, and the
Polity Spring service.
Shared identity, authorization, invitation, and future billing capabilities remain provided by the
Odonta platform.

## Workspace

- `apps/landing/` contains the public Vite landing and onboarding front door.
- `apps/mobile/` contains the phone-first Expo product app.
- `packages/design/` contains shared cross-platform design tokens.
- `services/polity/` contains the Polity Spring service and OpenAPI contract.
- `docs/` contains durable product architecture documentation.
- Shared platform capabilities are consumed as Odonta Maven artifacts.

## Getting Started

This repository supports both pnpm and npm workspaces. Install dependencies from the workspace root
with the package manager you want to use:

```bash
pnpm install
# or
npm install
```

Then run the thing you are working on from the root:

```bash
pnpm dev:landing
pnpm dev:mobile
pnpm test:service

npm run dev:landing
npm run dev:mobile
npm run test:service
```

The install unit is the workspace, even when you only work on one app. Run installs from the root so
the local `@polity/design` package is linked consistently. Keep using the same package manager for
an install unless you intentionally switch and reinstall.

## Quality Checks

```bash
pnpm check
pnpm check:js
pnpm check:service
pnpm compile:service

npm run check
npm run check:js
npm run check:service
npm run compile:service
```

`check` runs both the JS/TS workspace checks and the Polity service tests. Use the scoped commands
when you only need feedback for one side of the workspace.

## Documentation

Product documentation is indexed by the area that owns it.

- [Architecture](docs/architecture.md)
- [Polity service](services/polity/README.md)
