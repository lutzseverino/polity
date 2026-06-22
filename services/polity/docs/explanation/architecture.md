# Architecture

Polity owns product and constitutional behavior. Odonta supplies shared platform capabilities.

## Platform boundary

The service depends on stable Odonta artifacts for common web behavior, authorization, and the
Identity client. Product code should consume those published contracts instead of copying platform
types or generated HTTP clients into this repository.

## Application boundary

Generated requests and responses belong to the HTTP transport. Polity application services expose
product-owned inputs and results, while repository projections remain persistence-owned query
shapes.

Controllers cross the transport boundary through `...TransportMapper` interfaces. Application
mappers are declarative only: they map owner-local projections or entities when no product assembly
is needed. When a result spans multiple owners, services assemble it by asking the owner of each
piece rather than hiding that dependency behind a repository join or mapper method.

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
If that constitution is superseded before certification, the proceeding cannot enact an official
effect; old frozen procedure data remains historical evidence, not an alternate living government.

Procedures own quorum, thresholds, notice, and voting windows. Motions freeze the relevant procedure
rules and electorate snapshot when introduced, so later amendments do not rewrite old proceedings.
Plurality is reserved for office elections; yes/no procedures must use yes/no thresholds.
Motion responses keep those modalities separate: yes/no proceedings expose a vote tally, while
office elections expose an election tally with candidate ballot counts and any winner.

Founding uses a setup preset and a pace. The current preset is the Standard Republic: a starter
constitutional republic with a root Citizens' Assembly, a short bootstrap Steward term,
provisional admission authority, elected Steward and Tribune offices, typed constitutional powers,
citizen-held initiative for ordinary motions and elections, office-introduced sanctions with
citizen voting and appeal rights, disbandment, and stricter constitutional amendment rules. It is
not yet a full federal or branch-separated government; future presets may add divided institutions,
judicial offices, or federal layers when those mechanics are first class. Pace is a founding
parameter that changes timing such as bootstrap term length and voting windows without changing the
constitutional shape.

A polity is provisional until it has at least three citizens with political standing. Provisional
polities may admit members and may disband, but they cannot introduce ordinary governing motions,
sanctions, office elections, or constitutional amendments. If the founding Steward term expires
before the polity reaches three citizens with political standing, the founder retains only
provisional admission authority so the polity can finish forming; that fallback ends once the polity
reaches full government size and does not bypass missing constitutional powers or
political-standing checks. Once full government size is reached, the initial bootstrap Steward term
also ends; any later Steward authority must come from an elected or otherwise constitutionally
authorized office term. A sanctioned citizen may still introduce and certify an appeal for their own
active sanction, so due process does not depend on the sanction majority's permission.

Offices, office terms, sanctions, appeals, amendments, and typed official effects are Polity-owned
constitutional mechanics. Platform services may provide reusable access, invitation, billing,
notification, or delivery capabilities, but they must not own constitutional standing or decide
whether an official act is valid.

Office elections require candidate consent. Self-nominations are accepted immediately, but
third-party nominations begin pending and only accepted candidates can receive ballots, appear in
the election tally, or win office. Accepted candidates who lack political standing when the election
is certified are disqualified before the result is evaluated; later sanctions do not rewrite an
already-certified election. Declined, pending, or disqualified nominations remain historical
evidence, not valid ballot options.

Direct office assignment is legacy compatibility for historical proceedings, not a current public
constitution feature. New default constitutions and public creation contracts use office elections
for office terms; stored assignment motions, votes, certifications, and proposals are retained so
old records remain readable. The current service does not execute uncertified legacy assignment
effects, so any new office authority must come through office elections or a future constitutional
mechanic.

Certification is ministerial. After a motion's voting window closes, an active member may request
certification so the frozen electorate and recorded votes can be evaluated and the typed effect can
be applied. An appellant may also certify their own appeal even while suspended, so the appeal path
does not become a paper right. Certification must not become an office veto over a proceeding's
outcome.

Sanctions affect political standing, not product access. A suspended member may lose ordinary
political powers while retaining the ability to access the polity and pursue and certify their own
appeal when the constitution grants that path. Members without political standing cannot be
nominated or elected into offices while the sanction is active.

Constitutional amendments are typed domain changes, such as procedure, office, and power changes.
They are not arbitrary payload patches, and explanatory text does not itself mutate constitutional
state.

Disbandment is a certified constitutional effect, not deletion. A disbanded polity keeps its
official record readable, but it no longer accepts new motions, votes, certifications, invitations,
or admissions, and active office terms end when the effect is applied.

Official records should carry structured citations whenever they are produced by a proceeding:
motion, procedure, institution, power, certification, effect, and outcome where applicable. Durable
result fields should be structured outcome reasons rather than presentation prose. Narrative text is
useful for people, but the validity chain must remain queryable and localizable at the edge.
System-authored motions and official records expose template keys and parameters alongside fallback
text. User-authored motion text remains user content; backend-authored framing should not be the
only durable representation a client can present.

Parties, comments, likes, and reactions are deferred social layers. They may later help members
organize, endorse, discuss, and react to proceedings, but they must not silently change citizenship,
voting eligibility, office authority, or official effects. If parties ever receive formal powers or
seats, that is a constitutional feature rather than a social overlay.

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
