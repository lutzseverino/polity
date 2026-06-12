# Architecture

Polity owns product and constitutional behavior. Odonta supplies shared platform capabilities.

## Platform boundary

The backend depends on stable Odonta artifacts for common web behavior, authorization, and the
Identity client. Product code should consume those published contracts instead of copying platform
types or generated HTTP clients into this repository.

## Constitutional boundary

Product authorization controls whether a caller may access or attempt an operation. Constitutional
authority remains Polity state. Memberships, institutions, offices, powers, procedures, and effects
must not be represented as platform grants.

Proceedings retain the constitution version that governed them, and official records are append-only.

## Membership admission

The first slice admits a member by email and asks Identity to create or reuse a provisional user.
This proves polity membership and constitutional authority without duplicating Identity records.
Before invitations become a production feature, admission must be split into pending invitation and
accepted membership transitions so a provisional identity does not imply that an invitation was
delivered or accepted.
