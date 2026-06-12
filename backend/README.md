<div align="center">
    <h1 align="center">Polity</h1>
    <p>Constitutional government for friend groups.</p>
</div>

## Overview

Polity owns constitutional government behavior, including membership, versioned constitutions,
procedures, motions, voting, certification, typed official effects, and the official record.

Platform authorization controls coarse product access. Constitutional authority remains Polity
domain state and is evaluated synchronously by Polity application services.

## Development

```bash
cd backend
mvn test
```

The backend consumes Odonta platform artifacts through the local or configured Maven repository.

## Application Inputs

Mutation body schemas use the `...Input` suffix and may flow directly from generated API
interfaces into application services when their shape matches the use case. The OpenAPI schema
owns transport validation, while mapped domain enums own behavior and wire serialization.

Use `...Request` for types that represent a broader request interaction, not merely a mutation
body. Introduce a `...Command` only when the application actually dispatches or handles a command
as a distinct mechanism; do not mirror an OpenAPI input solely to rename its fields.
