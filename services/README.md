# Services

Polity owns one product backend: the Spring Boot service in
[`polity/`](polity/README.md). Reusable identity, authorization, billing, and
API-support capabilities live in
[Cardo](https://github.com/lutzseverino/cardo).

## Local Validation

From the repository root:

```bash
pnpm test:service
pnpm check:service:architecture
pnpm check:service:static
```

Or run Maven directly:

```bash
./mvnw -f services/polity/pom.xml test
./mvnw -f services/polity/pom.xml compile spotbugs:check
```
