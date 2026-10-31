# Membership Invitation Integration

Polity owns membership admission and membership-access policy. Cardo Invite owns secret tokens,
delivery, expiry, identity completion, and its invitation lifecycle. Cardo Authorization owns
durable receipt processing and provider application.

## Creation

Creating a local `MembershipInvitation` and publishing
`CardoInvitationCreationRequested` share one transaction. The asynchronous adapter sends Cardo's
lifecycle-only request: local invitation `requestId`, polity tenant, `polity:polity` tenant resource
type, normalized email, inviter user ID, and the product-owned accept URL. It sends no access
profile or grant snapshot.

Cardo creation is idempotent by request ID. Polity records Cardo's invitation ID, invited user ID,
and expiry in a separate local transaction. Modulith retains incomplete creation publications for
restart and outage recovery.

The acceptance URL base must be an absolute HTTP(S) URL with a host and no query or fragment;
Invite appends the secret token as the final path segment. Configure the client and recovery
interval explicitly:

```yaml
polity:
  membership-invitations:
    accept-url-base: ${POLITY_MEMBERSHIP_INVITATION_ACCEPT_URL_BASE:https://app.example.com/polities/invitations}
    retry-delay: ${POLITY_MEMBERSHIP_INVITATION_RETRY_DELAY:PT1M}

cardo:
  invite:
    client:
      base-url: ${INVITE_BASE_URL:http://invite:8083/api/v1}
      service-token-scope: cardo-invite
```

Compile against `invite-client`; provide `invite-client-http` at runtime.

## Acceptance

Authenticated acceptance is admission intent, not immediate admission:

1. Polity proves that the authenticated Identity user owns the invitation.
2. A locked local transaction stores `REQUESTED` and the original request timestamp, then publishes
   exactly one `CardoInvitationAcceptanceRequested`.
3. The adapter performs Cardo's idempotent Invite acceptance with that original timestamp.
4. After Cardo reports `ACCEPTED`, one local transaction locks the invitation and membership,
   stages `PolityGrantPlanner.membership`, retains the returned receipt on the membership, creates or
   reactivates the membership, completes the invitation, and records admission.

The HTTP operation returns `202 Accepted` with an acceptance-request representation while the
request is pending. Repeating it returns the current local outcome without publishing duplicate
work and never represents pending work as an admitted member.

Transport failures, outages, and 5xx responses leave the Modulith publication incomplete for
retry. Cardo's terminal `invitation_revoked`, `invitation_unavailable`, and `invitation_expired`
outcomes become durable local `FAILED` outcomes and complete the publication, so they are not
retried forever.

Polity rejects an acceptance attempt until Cardo creation has registered the remote invitation and
also rejects an attempt after Cardo's authoritative expiry. The durable command retains the
original request timestamp; Cardo evaluates expiry against that committed business timestamp
rather than a later retry time.

## Membership access convergence

Every newly founded, invited, or reactivated membership retains its product-owned grant receipt.
Membership activity and usable authorization remain separate facts: only `APPLIED` means usable
access; `PENDING` and `FAILED` do not.

Legacy active memberships intentionally migrate without a receipt. The authenticated
`/polities/{polityId}/members/me/access` contract first proves membership ownership, then reports
`legacy`, `pending`, `applied`, or `failed`. Its idempotent reconciliation action stages the normal
membership plan exactly once for a legacy active membership. Generic member lists contain neither
receipt identifiers nor provider details.

This self-convergence route must remain authenticated when product authentication is adopted; it
cannot require the grant that it is repairing.

## Runtime credentials

Provision three optional Keycloak client scopes and attach them to `polity-outbound`. Each scope
has exactly one audience mapper and must not be a default scope or share a token with another Cardo
audience:

| Cardo client | Requested optional scope | Exact single `aud` value | Required client role |
| --- | --- | --- | --- |
| Identity | `identity` | `identity` | `identity:profile:read` |
| Billing | `billing` | `billing` | `billing:entitlement:read` |
| Invite | `cardo-invite` | `cardo-invite` | `cardo-invite:product-service` |

If Invite's deployed resource-server client ID changes, both its sole audience and role namespace
change to that client ID. With the default client ID, add the outbound OAuth caller to Invite's
positive allowlist:

```shell
INVITE_PRODUCT_CLIENT_IDS=polity-outbound
```

Do not grant `cardo-invite:product-service` to end users. Invite rejects a caller missing any of
the exact audience, role, or allowlist entry.

Authorization provider application uses two additional, non-interchangeable credentials:

- `polity` owns the Polity Authorization Services resource catalog. Its service account obtains the
  PAT used for catalog protection operations and should hold only `polity:uma_protection`, with no
  realm-management authority.
- `polity-realm-admin` performs exact client lookup and assigns or removes already-defined Polity
  client roles on users. Grant only the constrained Keycloak administration needed for those
  operations (`view-clients` and `manage-users` in the standard realm-management model); it must
  not create clients, protocol mappers, or client roles and must not receive the catalog PAT role.
- `polity-outbound` supplies only the explicitly scoped service tokens used by Cardo HTTP clients.
  It must have neither Authorization Services nor realm-management authority.

Configure these through the `KEYCLOAK_POLITY_*`, `KEYCLOAK_POLITY_CATALOG_*`, and
`KEYCLOAK_POLITY_REALM_ADMIN_*` runtime inputs documented in the service README. Verify deployment
with non-secret decoded test credentials: every outbound token has the configured issuer,
expiration, exactly its one target audience, its required role, and no unrelated audience. Verify
the catalog token has `polity:uma_protection` and fails realm-admin calls, while the realm-admin
token can read the exact Polity client and perform the required user role mapping but fails Polity
catalog protection calls. Never record live bearer tokens as evidence.

## Token onboarding

Polity exposes public token inspection and identity-completion proxy operations under
`/invitation-tokens/{token}`. Every operation verifies that Cardo's invitation UUID is linked to a
pending local membership invitation before returning context or requesting work. The onboarding
response contains only the polity UUID, polity name, invited email, and expiry; it never exposes
the local invitation UUID, Cardo invitation UUID, invited Identity user UUID, or service
credentials.

`POST /invitation-tokens/{token}/completion` calls Cardo's idempotent
`requestCompletion(token)` operation and returns `202 Accepted` with the durable completion state.
The browser polls `GET /invitation-tokens/{token}/completion`, which delegates to
`getCompletion(token)`, until Cardo reports `COMPLETED` or `FAILED`. Polity preserves Cardo's
status, attempt count, last error, action expiry, completion timestamp, and operation timestamps
without reimplementing Cardo's retry saga. All public token and completion responses use
`Cache-Control: no-store`.

Identity completion and membership acceptance remain separate. After completion or sign-in, the
authenticated user lists pending invitations through `/invitations` and submits the returned local
invitation UUID to `/invitations/{invitationId}/accept`. Cardo triggers a Keycloak-owned action; no
password crosses the Polity boundary, and Polity never accepts, stores, logs, or relays an
invitation credential.

The browser must treat the emailed route segment as a secret Cardo token, not as a local invitation
UUID. No revocation dispatch is staged until Polity defines and persists a membership-invitation
cancellation transition; Cardo revocation must follow that local transition rather than inventing
a second source of domain truth.

## Pre-integration invitations

Pending invitation rows created before the Cardo integration have no Cardo invitation UUID and no
durable creation publication. Migration V3 marks those rows `CANCELLED` with a response timestamp,
which releases the pending-email uniqueness constraint without silently sending remote
invitations. An authorized member must reissue the invitation through the current creation
workflow.

Migration V6 deliberately leaves preexisting acceptance outcomes `NULL`, including historical
`ACCEPTED` and `CANCELLED` rows, because those rows do not contain authoritative evidence of a
Cardo acceptance outcome. New non-null outcomes are constrained as `PENDING`/`REQUESTED`,
`ACCEPTED`/`COMPLETED`, or `CANCELLED`/`FAILED`.
