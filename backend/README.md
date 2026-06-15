<div align="center">
    <h1 align="center">Polity Backend</h1>
    <p>The product service for constitutional government.</p>
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

## Documentation

Durable backend architecture is documented in [Architecture](../docs/architecture.md).
