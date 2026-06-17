<div align="center">
    <h1 align="center">Polity</h1>
    <p>A product workspace for constitutional government software.</p>
    <p>
        <img alt="workspace" src="https://img.shields.io/badge/workspace-product-0f172a">
        <img alt="landing" src="https://img.shields.io/badge/landing-vite-111827">
        <img alt="mobile" src="https://img.shields.io/badge/mobile-expo-111827">
        <img alt="service" src="https://img.shields.io/badge/service-spring_boot-1f2937">
        <img alt="docs" src="https://img.shields.io/badge/docs-diataxis-374151">
        <img alt="license" src="https://img.shields.io/badge/license-MIT-4b5563">
    </p>
</div>

## Overview

Polity is a product workspace for small-group constitutional government.
It models membership, constitutions, institutions, offices, motions, voting, certification, and
official records as first-class product concepts.

## Getting Started

Install dependencies from the workspace root:

```bash
pnpm install
```

Run the surface you are working on:

```bash
pnpm dev:landing
pnpm dev:mobile
pnpm test:service
```

The install unit is the workspace, even when you only work on one app. Run installs from the root so
local workspace packages are linked consistently.

## Quality Checks

```bash
pnpm check
pnpm check:js
pnpm check:service
pnpm compile:service
```

`pnpm check` runs both the JS/TS workspace checks and the Polity service tests. Use scoped commands
when you only need feedback for one side of the workspace.

## Documentation

Start at the nearest owner:

- [Repository docs](docs/README.md)
- [Landing app](apps/landing/README.md)
- [Mobile app](apps/mobile/README.md)
- [Polity service](services/polity/README.md)
