# Backend Conventions

This reference is authoritative for Polity-specific backend packages, canonical owners, platform
constraints, and deliberate exceptions.

Polity adopts Cardo's repo-agnostic
[Application Boundaries](https://github.com/lutzseverino/cardo/blob/main/docs/reference/application-boundaries.md)
reference. That document defines service, workflow, resolver, mapper, transport, contract-type,
projection, and materialization semantics. This document adds the local choices needed to apply
those rules in Polity; it does not redefine them.

## Adopted Profile

Polity uses the Spring Boot, JPA, MapStruct, and generated OpenAPI form of the shared convention:

- application services use `@Service` and the `Service` suffix;
- public cross-owner application entrypoints use `@Component` and the
  `{Verb}{Object}Workflow` shape;
- repositories use projections and scalar predicates for reads and make exceptional entity
  materialization explicit;
- generated API interfaces, OpenAPI tags, controllers, and transport mappers align to semantic
  transport owners;
- public services and workflows expose application inputs, results, domain values deliberately
  established as contracts, or ordinary context values—not transport or persistence types.

## Package Map

Polity organizes most mechanical roles as top-level packages:

- `controller` owns HTTP adapters and generated API interfaces.
- `mapper` owns declarative application and transport mappings.
- `model` owns entities, enums, and domain value objects.
- `input` owns operation-specific application inputs using the `Input` suffix.
- `result` owns application results, named read views, and supporting semantic value types.
- `repository` owns persistence ports, entity projections, scalar predicates, locks, and explicit
  mutation-side entity materialization.
- `service` owns application behavior for stable domain and application owners.
- `workflow` owns transactional use cases that coordinate independent owners.
- `effect` owns official-effect application and effect-specific remedies.
- `resolver` owns narrow repository-backed context reconstruction for other application owners.
- `template` owns preset and template seeding components.
- `authorization` owns product access and constitutional authority rules.
- `integration` owns durable adapters to Cardo and other external application owners; it does not
  acquire the product domain decisions that trigger those adapters.
- `evaluator` owns pure calculation engines.
- `validation` owns Bean Validation annotations and validators.
- `config` and `exception` own Spring configuration and exception handling.

Do not add product-lore top-level packages when a concept fits an established mechanical role. A
feature-specific local package is warranted only when its contents form a real boundary that the
existing role packages would obscure.

## Canonical Owners

Use the backend and OpenAPI nouns as the source of truth. Frontend domains, application results,
mappers, controllers, and generated types keep the same canonical noun unless their boundary role
requires a suffix.

- `MotionService` owns generic motion reads. Voting, certification, candidacy, and introduction
  paths are named workflows because they coordinate motion state with memberships, procedures,
  effects, elections, or official records.
- A specialized motion route belongs to the object it creates or changes. Sanctions, appeals,
  office elections, office-term reviews, constitutional reviews, amendments, and disbandments each
  retain their own transport mapper and controller ownership even when the path is nested under a
  motion.
- `MembershipInvitation` is the canonical invitation noun across the entity, repository,
  application result, OpenAPI schema, generated client, frontend domain, and feature workflows.
  `MembershipInvitationService` owns reads; creation and acceptance are workflows.
- `GovernmentStructureService` and `PolityActionAvailabilityService` are stable application-view
  owners. They are not resolver implementations hidden behind service facades.

Route nesting does not transfer ownership. Do not group unrelated owners under umbrellas such as
`Justice`, or assign every polity-nested operation to `PolityController` and every motion-nested
operation to `MotionController`.

## Platform Boundaries

Cardo clients may gate product access, delivery, billing, identity, and authorization mechanics.
Polity keeps the resulting product decisions in Polity-owned terms.

- Billing may decide whether a caller can create or use a paid product surface, but it must not
  grant membership, political standing, office authority, procedure eligibility, or official
  effects.
- Identity owns platform users and authentication; Polity owns memberships and political identity
  inside a polity.
- Authorization provides shared mechanics; Polity owns constitutional powers, product resource
  types, actions, and grant planning.
- Product services consume stable Cardo client contracts rather than Cardo persistence or transport
  implementation types.

Membership invitations use Cardo Invite for token, delivery, provisional identity, expiry, and
captured authorization-grant mechanics. Polity retains the membership-invitation record,
constitutional admission authority, official records, membership admission, and its public read
model. The detailed ordering and retry contract is documented in
[Membership Invitation Integration](membership-invitation-integration.md).

## Local Exceptions

Before adding a package role, reader, writer, cross-owned projection, non-owner service, or public
application type, first try to fit the behavior into the shared convention and this package map.

An exception must identify the owner that cannot be represented cleanly, explain why owner assembly
or an existing boundary is insufficient, and remain narrower than a new project-wide convention.
Update architecture tests when an exception represents a durable rule; do not leave the exception
as an unverified naming precedent.
