<div align="center">
    <h1 align="center">Polity</h1>
    <p>A product workspace for constitutional government software.</p>
    <p>
        <img alt="workspace" src="https://img.shields.io/badge/workspace-product-0f172a">
        <img alt="landing" src="https://img.shields.io/badge/landing-vite-111827">
        <img alt="mobile" src="https://img.shields.io/badge/mobile-expo-111827">
        <img alt="backend" src="https://img.shields.io/badge/backend-spring_boot-1f2937">
    </p>
</div>

## Overview

Polity is a friend-group government platform built around an explicit constitutional kernel.
It owns membership, versioned constitutions, institutions, powers, procedures, motions, voting,
certification, typed effects, and the official record.

The repository contains a Vite landing app, an Expo mobile app, shared design tokens, and the
Polity Spring backend.
Shared identity, authorization, invitation, and future billing capabilities remain provided by the
Odonta platform.

## Workspace

- `backend/` contains the Polity Spring service and OpenAPI contract.
- `apps/landing/` contains the public Vite landing and onboarding front door.
- `apps/mobile/` contains the phone-first Expo product app.
- `packages/design/` contains shared cross-platform design tokens.
- `docs/` contains durable product architecture documentation.
- Shared platform capabilities are consumed as Odonta Maven artifacts.

## Documentation

Product documentation is indexed by the area that owns it.

- [Architecture](docs/architecture.md)
- [Backend](backend/README.md)
