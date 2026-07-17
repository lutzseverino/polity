# Membership Invitation Integration

Polity owns why a person is invited and what joining a polity means. Cardo Invite owns the shared
invitation mechanics: the secret token, expiry, delivery, provisional identity completion, Invite
lifecycle state, and durable authorization-grant application.

## Creation

`CreateMembershipInvitationWorkflow` performs Polity's constitutional authority checks, persists
the `MembershipInvitation`, appends the official invitation record, and stages a durable Cardo
creation event in one Polity transaction. The event listener runs with transaction propagation
`NOT_SUPPORTED`, so `InvitationsClient.create` is never called inside a Polity database
transaction.

The Cardo request uses:

- the Polity membership-invitation UUID as `requestId`;
- `polity:polity` as the tenant resource type;
- `polity:member` as the audit access profile;
- the exact `polity:polity/read` and `polity:polity/participate` grant snapshot;
- the identity user UUID of the member who authorized the invitation;
- `polity.membership-invitations.accept-url-base` as the product-owned link base.

The acceptance URL base must be an absolute HTTP(S) URL with a host and no query
or fragment; Invite appends the secret token as the final path segment.

Cardo creation is idempotent by `(product, requestId)`. After a successful call, Polity records the
Cardo invitation UUID, invited identity UUID, and authoritative expiry in a separate local
transaction. If Cardo succeeds but that local update fails, retrying creation returns the same
Cardo invitation and safely repeats the registration.

## Acceptance

`AcceptMembershipInvitationWorkflow` still owns invitee matching, member admission or
reactivation, bootstrap completion, and the official admission record. It commits those changes
together with a durable Cardo acceptance event. The listener calls `InvitationsClient.accept`
without a Polity transaction. Cardo then marks its invitation accepted and applies the grant
snapshot captured during creation; Polity does not stage a duplicate grant plan on this path.

Polity rejects an acceptance attempt while Cardo creation has not yet been registered, avoiding an
admission that could race ahead of delivery. The integration processor also treats a missing Cardo
identifier as a retryable failure defensively. The scheduled recovery retries incomplete creation
and acceptance publications after the configured delay, including work interrupted by a process
crash. Cardo's grant application is asynchronous, so callers must tolerate a
short interval between membership admission and authorization convergence. Configure the clients
and retry delay as follows:

Polity also rejects admission after the authoritative Cardo expiry it stored during creation. This
prevents a local membership commit whose later Cardo acceptance could only fail permanently.

The acceptance publication retains the same timestamp written to Polity's
membership invitation. Cardo validates expiry against that committed business
timestamp, not a later retry time, so a transient outage cannot invalidate an
admission that was timely when Polity committed it.

```yaml
polity:
  membership-invitations:
    accept-url-base: ${POLITY_MEMBERSHIP_INVITATION_ACCEPT_URL_BASE:https://app.example.com/polities/invitations}
    retry-delay: ${POLITY_MEMBERSHIP_INVITATION_RETRY_DELAY:PT1M}

cardo:
  invite:
    client:
      base-url: ${INVITE_BASE_URL:http://invite:8083/api/v1}
```

Compile against `invite-client`; provide `invite-client-http` at runtime. The HTTP implementation
uses Polity's existing Cardo client-credentials token provider.

Invite authenticates that service account positively. In Keycloak, the `polity` service account
must have the `product-service` client role from the `cardo-invite` client, and its access-token
audience mapper must include `cardo-invite`. The resulting token must contain the
`cardo-invite:product-service` authority and `cardo-invite` in `aud`. On the Invite deployment,
include Polity in the explicit caller allowlist:

```shell
INVITE_PRODUCT_CLIENT_IDS=polity
```

Do not grant the Invite product-service role to end users. These are Keycloak and Invite deployment
settings; Polity's client cannot add a missing role or audience to the token it receives.

## Token Onboarding

Polity exposes public token inspection and identity-completion proxy operations under
`/invitation-tokens/{token}`. Every operation verifies that Cardo's invitation UUID is linked to a
pending local membership invitation before returning context or requesting work. The onboarding
response contains only the polity UUID, polity name, invited email, and expiry; it never exposes the
local invitation UUID, the Cardo invitation UUID, the invited Identity user UUID, or Cardo service
credentials.

`POST /invitation-tokens/{token}/completion` calls Cardo's idempotent
`requestCompletion(token)` operation and returns `202 Accepted` with the durable completion state.
The browser polls `GET /invitation-tokens/{token}/completion`, which delegates to
`getCompletion(token)`, until Cardo reports `COMPLETED` or `FAILED`. Polity preserves Cardo's status,
attempt count, last error, action expiry, completion timestamp, and operation timestamps without
reimplementing Cardo's retry saga. All public token and completion responses use `Cache-Control:
no-store`.

Identity completion and membership acceptance remain separate. After completion or sign-in, the
authenticated user lists their pending invitations through `/invitations` and accepts the returned
local invitation UUID through the existing acceptance operation. Cardo triggers a Keycloak-owned
action; no password crosses the Polity boundary, and Polity never accepts, stores, logs, or relays
an invitation credential.

The browser must treat the emailed route segment as a secret Cardo token, not as a local invitation
UUID. No revocation dispatch is staged until Polity defines and persists a membership-invitation
cancellation transition; Cardo revocation must follow that local transition rather than inventing
a second source of domain truth.

Pending invitation rows created before this integration have no Cardo invitation UUID and no
durable creation publication. Migration V3 marks those rows `CANCELLED` with a response timestamp,
which releases the pending-email uniqueness constraint without silently sending remote invitations.
An authorized member can then reissue the invitation through the new creation workflow.
