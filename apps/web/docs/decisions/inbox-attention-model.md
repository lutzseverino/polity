# Inbox as a User-Attention Read Model

## Status

Accepted

## Context

The application needs one place where a person can discover work across polities, including pending
membership invitations, votes, candidacy responses, and informational updates. These items have different
domain owners and lifecycles. Treating them as arbitrary notifications would duplicate domain state, let
features disagree about when an item is resolved, and eventually make the Inbox an inconsistent collection
of special-purpose cards.

The UI also needs to distinguish two independent concepts. An open task still requires a domain action,
while an unread item has not yet been seen. Reading a task must not resolve it, and reading an informational
update must not make it actionable.

## Decision

Model Inbox as a user-attention read model owned by `src/domains/inbox`.

Source domains remain authoritative. Membership owns whether an invitation is pending, motions own whether
a vote is available, and election proceedings own whether a candidacy response is pending. Inbox request
adapters read those source contracts and project them into typed `InboxItem` values; features and routes do
not create Inbox items directly.

Inbox items use a discriminated source that identifies the producing domain fact and the identifiers needed
to reach it. Their identifiers are stable and namespaced by producer. The projection owns cross-source
ordering, deduplication, and presentation data. Fixture-backed producers follow the same seam as future HTTP
producers.

The two Inbox categories have separate lifecycle rules:

- `needs-action` contains only currently open tasks. Opening one may change its read state, but it remains in
  this category until the source-domain action is completed, expires, or is otherwise made unavailable.
- `updates` contains informational events. Their read state may change independently and they do not
  contribute to the open-task count.

The shell badge and Inbox preview count open tasks, not unread items. Read indicators remain presentation
metadata inside Inbox. Mutations that complete a source-domain task update or invalidate both the source
query and the Inbox projection so the resolved item disappears.

Inbox list rows use the standard Inbox presentation for scanning. Selecting an item hands off to the feature
or route that owns the action. Specialized response visuals therefore remain feature-owned; they are not
embedded as custom card layouts in the Inbox list.

When sources eventually span multiple backend services, aggregation may move to a server-side attention
endpoint or event-backed projection. That projection may report domain facts but must not decide
constitutional eligibility or action validity.

## Consequences

- Every Inbox producer has one typed projection path and one source-domain owner.
- Open-task counts cannot drift into unread-notification counts.
- Completing an invitation, vote, or candidacy response removes the task through domain lifecycle rather
  than a separate notification-dismiss action.
- Inbox rows remain visually consistent while consequential actions retain specialized feature surfaces.
- Read persistence, cross-service aggregation, ranking, pagination, and retention remain explicit follow-up
  contracts instead of behavior hidden in components.
- Development scenario state resets on reload. Live persistence belongs to the service behind the same HTTP
  requests used by the application.

## Alternatives Considered

- A React notification context: rejected because context distributes state but does not establish source
  ownership, resolution semantics, deduplication, persistence, or cache synchronization. TanStack Query
  already owns shared server-state distribution.
- Feature-owned notification creation: rejected because every feature could invent incompatible lifecycle,
  count, and presentation rules.
- A generic persisted notification record as the source of truth: rejected for open tasks because it could
  disagree with the authoritative domain state about eligibility or completion.
- Custom Inbox cards per item kind: rejected because the feed is for scanning; specialized action UI belongs
  to the destination feature surface.
