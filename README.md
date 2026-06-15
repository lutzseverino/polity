<div align="center">
    <h1 align="center">Polity</h1>
    <p>A product workspace for constitutional government software.</p>
    <p>
        <img alt="workspace" src="https://img.shields.io/badge/workspace-product-0f172a">
        <img alt="frontend" src="https://img.shields.io/badge/frontend-vite-111827">
        <img alt="backend" src="https://img.shields.io/badge/backend-spring_boot-1f2937">
    </p>
</div>

## Overview

Polity is a friend-group government platform built around an explicit constitutional kernel.
It owns membership, versioned constitutions, institutions, powers, procedures, motions, voting,
certification, typed effects, and the official record.

The repository contains an empty Vite and Tailwind frontend shell and the Polity Spring backend.
Shared identity, authorization, invitation, and future billing capabilities remain provided by the
Odonta platform.

## Workspace

- `backend/` contains the Polity Spring service and OpenAPI contract.
- `web/` contains the Vite application and generated TypeScript API client.
- `docs/` contains durable product architecture documentation.
- Shared platform capabilities are consumed as Odonta Maven artifacts.

## Documentation

Product documentation is indexed by the area that owns it.

- [Architecture](docs/architecture.md)
- [Backend](backend/README.md)
