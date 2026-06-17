# Architecture

Polity owns product and constitutional behavior. Odonta supplies shared platform capabilities.

## Platform boundary

The backend depends on stable Odonta artifacts for common web behavior, authorization, and the
Identity client. Product code should consume those published contracts instead of copying platform
types or generated HTTP clients into this repository.

## Application boundary

Generated requests and responses belong to the HTTP transport. Polity application services expose
product-owned inputs and results, while repository projections remain persistence-owned query
shapes.

Controllers cross the transport boundary through `...TransportMapper` interfaces. Services cross
the persistence-to-application boundary through `...ApplicationMapper` interfaces. Mapper
interfaces contain declarative mappings, while product-specific enum and value conversions live in
dedicated mapper helpers.

## Constitutional boundary

Product authorization controls whether a caller may access or attempt an operation. Constitutional
authority remains Polity state. Memberships, institutions, offices, powers, procedures, and effects
must not be represented as platform grants.

Platform grants should stay coarse. The polity resource currently exposes `read` and `participate`
actions: they allow product access and participation attempts, but they do not grant constitutional
powers such as admitting members, certifying results, assigning offices, applying sanctions, or
amending the constitution.

Polity visibility is product access policy. Public polities may expose public-safe read views to any
authenticated user, but public visibility must not create membership, platform grants, voting
eligibility, office authority, or political standing. Private polities require membership-backed
access for reads, and member/contact surfaces remain member-only even when a polity is public.

Proceedings retain the constitution version that governed them, and official records are append-only.

Procedures own quorum, thresholds, notice, and voting windows. Motions freeze the relevant procedure
rules and electorate snapshot when introduced, so later amendments do not rewrite old proceedings.

Offices, office terms, sanctions, appeals, amendments, and typed official effects are Polity-owned
constitutional mechanics. Platform services may provide reusable access, invitation, billing,
notification, or delivery capabilities, but they must not own constitutional standing or decide
whether an official act is valid.

Sanctions affect political standing, not product access. A suspended member may lose ordinary
political powers while retaining the ability to access the polity and pursue an appeal when the
constitution grants that path.

Constitutional amendments are typed domain changes, such as procedure-rule deltas. They are not
arbitrary payload patches, and explanatory text does not itself mutate constitutional state.

Official records should carry structured citations whenever they are produced by a proceeding:
motion, procedure, institution, power, certification, effect, and outcome where applicable. Narrative
text is useful for people, but the validity chain must remain queryable.

Database migrations are split by Polity domain areas, following the service pattern used elsewhere
in Odonta. They still remain ordered Flyway versions, but each file should describe one coherent
domain increment instead of accumulating unrelated constitutional changes.

## Membership admission

Membership admission is split into an invitation and an acceptance. A member with the
constitutionally valid `ADMIT_MEMBER` power can create a pending invitation by email; the invitation
is Polity-owned domain state and is exposed through the API so the frontend can show it as a
notification. Creating the invitation may ask Identity to create or reuse a provisional user, but it
does not create membership and does not stage polity grants.

Acceptance is the admission transition. The invited user accepts a pending invitation, Polity creates
the active membership, stages the normal member access grants, and writes the admission to the
official record. Email delivery is intentionally not part of this slice; a future platform invitation
or messaging service can deliver notifications without changing the constitutional membership
transition.
