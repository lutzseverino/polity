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
access for reads, and private creation is gated by the founder's product billing entitlement.
Private creation is serialized per founder before the active private-polity count is checked, so a
paid tenant limit cannot be bypassed by concurrent creation requests.
Member/contact surfaces remain member-only even when a polity is public.

Proceedings retain the constitution version that governed them, and official records are append-only.
If that constitution is superseded before certification, the proceeding cannot enact an official
effect; old frozen procedure data remains historical evidence, not an alternate living government.
Amendment records store typed change items and render localized summaries from those items at the
transport boundary, rather than storing freeform constitutional text as the enforceable rule.

Procedures own quorum, thresholds, notice, and voting windows. Motions freeze the relevant procedure
rules and electorate snapshot when introduced, so later amendments do not rewrite old proceedings.
Office-election result thresholds are reserved for office elections; yes/no procedures must use
yes/no thresholds. Motion responses keep those modalities separate: yes/no proceedings expose a vote
tally, while office elections expose election method, seat counts, ranked tally rounds, candidate
vote totals, and elected winners.

Founding uses a setup preset and a pace. The current preset is the Standard Constitutional Council
Republic: a starter constitutional republic with a root Citizens' Assembly, an elected Citizens'
Council for ordinary resolutions, short starter office terms, provisional admission authority,
elected Steward and Tribune offices, a Magistrates' Court and Magistrate office for appeals and
review, typed constitutional powers, citizen-held initiative for elections, office-introduced
sanctions with citizen voting and appeal rights, disbandment, and stricter constitutional amendment
rules. It is intentionally scoped as a constitutional-republic engine: jurisdictions are currently
constitutional scoping metadata, not layered sovereignty. Pace is a founding parameter that changes
timing such as starter office term length and voting windows without changing the constitutional
shape.

Government assessment has two axes. Readiness reports whether the polity can mechanically operate
right now: provisional, forming offices, ready, blocked, or disbanded. Constitutional health reports
whether the current constitution still defines robust paths such as admission, elections, appeal,
amendment, disbandment, and certification. Readiness is staffing-and-standing aware; health is
structural and should not treat an empty office as constitutional degradation. Action availability
keeps per-action reasons, but those reasons should line up with the same authority and electorate
facts used by readiness diagnostics.

A polity is provisional until it has at least three citizens with political standing. Provisional is
not a blanket engine lock: actions are blocked only when their constitutional authority or procedure
electorate cannot actually run. The standard preset makes active-member procedures require two
eligible electors, so a single citizen cannot start voting machinery alone, while two citizens can
use viable two-elector paths such as office elections or other meta procedures if the constitution
grants them. If the founding Steward term expires before the polity reaches three citizens with
political standing, the founder retains only provisional admission authority so the polity can finish
forming; that fallback ends once the polity reaches full government size and does not bypass missing
constitutional powers or political-standing checks. Once full government size is reached, the initial
bootstrap Steward term also ends; any later Steward authority must come from an elected or otherwise
constitutionally authorized office term.

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

Certification is ministerial over the voting result. After a motion's voting window closes, an
active member may request certification so the frozen electorate and recorded votes can be evaluated
and the typed effect can be applied. Applying a passed effect is still transactional: if the target
state no longer has a live remedy, no certification or official record is committed. An appellant may
also certify their own appeal even while suspended, so the appeal path does not become a paper right.
Certification must not become an office veto over a proceeding's outcome.

Sanctions affect political standing, not product access. A suspended member may lose ordinary
political powers while retaining the ability to access the polity and pursue and certify their own
appeal when the constitution grants that path. A new sanction cannot be introduced unless the
current constitution has an available appeal procedure, so due process is a live mechanism rather
than paper text. The appeal bench must also remain viable after recusing the sanction target, the
appellant, and the original sanction proposer, so a thin judiciary cannot sanction one of its own and
erase the target's appeal path. The sanction duration must also be long enough for the current appeal
procedure to complete, otherwise the appeal right would expire before it could be certified. Members
without political standing cannot be nominated or elected into offices while the sanction is active.

Constitutional amendments are typed domain changes, such as procedure, office, and power changes.
They are not arbitrary payload patches, and explanatory text does not itself mutate constitutional
state. Amendment validation should enforce engine invariants, such as known referenced offices,
compatible procedure effects, and office seat counts that do not invalidate currently active terms.
It should not reject risky-but-executable choices merely because they make a path hard or impossible
to use; those choices are constitutional risk and should surface through readiness and health.

Constitutional review is a voiding remedy for official acts with an active reversible effect:
adopted resolutions, applied sanctions, and elected office terms. It does not rewrite historical
records or pretend every official act can be undone; appeals, office-term reviews, amendments,
admissions, founding, certification, and disbandment are handled only by their own explicit
mechanics.

Disbandment is a certified constitutional effect, not deletion. A disbanded polity keeps its
official record readable, but it no longer accepts new motions, votes, certifications, invitations,
or admissions. The effect ends active office terms and revokes active members' participation grants
while preserving read access to the historical polity record. Disbandment is governed by the
disbandment procedure itself; constitutional-review availability does not gate dissolution because
constitutional review only voids active reversible acts and cannot void a disbandment.

Official records should carry structured citations whenever they are produced by a proceeding:
motion, procedure, institution, power, certification, effect, and outcome where applicable. Durable
result fields should be structured outcome reasons rather than presentation prose. Narrative text is
useful for people, but the validity chain must remain queryable and localizable at the edge.
System-authored motions and official records expose template keys and parameters alongside fallback
text or durable message keys. User-authored motion text remains user content; backend-authored
framing should not be the only durable representation a client can present.

Parties, comments, likes, and reactions are deferred social layers. They may later help members
organize, endorse, discuss, and react to proceedings, but they must not silently change citizenship,
voting eligibility, office authority, or official effects. If parties ever receive formal powers or
seats, that is a constitutional feature rather than a social overlay.

Polity is still pre-production, so the database schema is kept as a resettable baseline while the
core mechanics are settling. Once an applied schema becomes a deployed contract, new changes should
move to ordered Flyway versions split by coherent Polity domain increments instead of editing the
baseline.

## Membership admission

Membership admission is split into an invitation and an acceptance. A member with the
constitutionally valid `ADMIT_MEMBER` power can create a pending invitation by email; the invitation
is Polity-owned domain state and is exposed through the API so the frontend can show it as a
notification. Creating the invitation may ask Identity to create or reuse a provisional user, but it
does not create membership and does not stage polity grants.

Acceptance is the admission transition. The invited user accepts a pending invitation, Polity creates
or reactivates the active membership, stages the normal member access grants, and writes the admission to the
official record. Email delivery is intentionally not part of this slice; a future platform invitation
or messaging service can deliver notifications without changing the constitutional membership
transition.

## Membership exit

Voluntary resignation is the canonical membership-exit mechanic. An active member may resign their
own membership without a motion; Polity marks the membership resigned, ends any active office terms
held by that member, stages revocation of the normal member access grants, and writes the resignation
to the official record. Resignation does not rewrite prior votes, certifications, office records, or
other historical evidence.

The last active citizen normally cannot resign and orphan an active polity. They should use
disbandment when the constitutional disbandment path is available. If degradation has made formal
disbandment unavailable, the final active citizen may resign and close the polity so the engine does
not trap them inside dead government machinery. The founding citizen also cannot resign before
bootstrap completes unless that same final-citizen closure applies, because provisional admission
authority is the only path for the polity to reach full government size. Forced member removal or
expulsion is deferred due-process work, distinct from sanctions; if added, it should be its own
constitutional mechanic rather than a hidden sanction variant.
