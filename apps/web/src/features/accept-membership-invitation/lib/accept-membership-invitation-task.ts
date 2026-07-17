export type AcceptMembershipInvitationTask = Readonly<{
  invitationId: string;
  kind: "accept-membership-invitation";
}>;

export function readAcceptMembershipInvitationTask(
  value: unknown,
): AcceptMembershipInvitationTask | undefined {
  if (
    typeof value !== "object" ||
    value === null ||
    !("kind" in value) ||
    (value.kind !== "accept-membership-invitation" &&
      value.kind !== "invitation-response") ||
    !("invitationId" in value) ||
    typeof value.invitationId !== "string" ||
    value.invitationId.length === 0
  ) {
    return undefined;
  }

  return {
    invitationId: value.invitationId,
    kind: "accept-membership-invitation",
  };
}
