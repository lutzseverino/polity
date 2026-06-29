# Backend Conventions

This reference is authoritative for Polity backend package roles, repository projections, and
service assembly rules.

## Structure

Top-level packages describe mechanical roles:

- `controller` owns HTTP adapters and generated API interfaces.
- `mapper` owns declarative boundary mappings.
- `model` owns entities, inputs, results, enums, and domain value objects.
- `repository` owns persistence ports, entity-owned projections, and explicit entity materialization
  for command paths.
- `service` owns application behavior for product entities and workflows.
- `effect` owns official-effect application components and effect-specific remedies.
- `resolver` owns repository-backed components that resolve a domain set or context for another
  owner.
- `template` owns preset and template seeding components.
- `authorization` owns product access and constitutional authority rules.
- `evaluator` owns pure calculation engines.
- `validation` owns Bean Validation annotations and validators.
- `config` and `exception` own Spring configuration and exception handling.

Do not add product-lore top-level packages. If a concept fits an existing mechanical role, use the
existing role.

## Service Rules

- Use the `Service` suffix and `@Service` stereotype for application services that own a product
  entity or a real workflow spanning multiple entities.
- Internal collaborators that resolve, adapt, plan, apply, or calculate one mechanic should use a
  precise noun such as `Resolver`, `Adapter`, `Planner`, `Applicator`, or `Evaluator` and the
  narrowest fitting stereotype/package.
- Public application service methods accept application-owned inputs or ordinary context values and
  return application-owned results or no value.
- Public application service methods must not accept or return entities, repository projections, or
  generated transport types.
- Prefer one service per entity or clear workflow owner.
- Put ordinary owner lookups and derived owner rules in that entity service.
- Avoid materializing entities for application flow. Prefer projections, scalar predicates, and
  explicit repository commands. Entity materialization is a command-side escape hatch for managed
  aggregate transitions that cannot be expressed cleanly otherwise.
- Do not create `Reader` or `Writer` classes for generic entity lookup or persistence commands.
- A separate non-entity service is acceptable only when it owns a real workflow boundary.
- Use `authorization` for product authorization rules instead of placing them in `service`.
- Use `evaluator` for pure tally or calculation code with no repository ownership.

## Platform Client Rules

- Platform clients may gate product access, delivery, billing, identity, or grants.
- Keep commercial rules outside constitutional state. Billing may decide whether a caller can create
  or use a paid product surface, but it must not grant membership, political standing, office
  authority, procedure eligibility, or official effects.
- Product services should consume published platform client contracts directly and keep product
  limits in product-owned terms.

## Projection Rules

- `EntityProjection` exposes fields owned by `Entity`.
- Generic entity projections must not borrow display names, versions, or labels from neighboring
  entities through joins.
- Repository read methods return projections or scalar values. A custom repository method that
  returns an entity must include `Entity` in the method name, such as `findEntityBy...` or
  `findEntitiesBy...`, and should only serve command-side state transitions that truly require a
  managed aggregate.
- When a result needs data from multiple owners, the application service assembles the result by
  asking the owning repository or service for each piece.
- Repository joins are allowed for filtering, existence predicates, locking, and persistence
  constraints when the selected projection remains owned by the repository entity. Do not use joins
  to smuggle neighboring-owner state into generic repository reads.
- Cross-owned read projections are exceptions. Use them only when the read model is a concrete
  product view that cannot be expressed cleanly by owner assembly. Name the projection after that
  concrete view, not as a generic `Slice` or `Summary`.

## Mapper Rules

- Mappers are named by owner and boundary: `{Owner}ApplicationMapper` or
  `{Owner}TransportMapper`.
- Do not group mappings under product-area umbrellas when the mapped types belong to separate
  owners.
- Do not create one mapper per request or response class unless that type is itself the stable
  owner.
- Application mappers stay declarative.
- They may map an entity-owned projection to an application result when no cross-owner assembly is
  needed.
- They must not hide repository joins or product orchestration.
- If mapping requires owner lookups, build the result in the owning service instead.

## Exception Rule

Before adding a new package, reader, writer, cross-owned projection, or non-entity service, first
try to fit the behavior into an existing convention. Add a new convention only when the simpler
shape would misrepresent ownership or obscure behavior.
