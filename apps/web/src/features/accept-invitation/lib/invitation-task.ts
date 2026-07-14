export type InvitationTask = Readonly<{
  invitationId: string;
  kind: "invitation-response";
}>;

export function readInvitationTask(value: unknown): InvitationTask | undefined {
  if (
    typeof value !== "object" ||
    value === null ||
    !("kind" in value) ||
    value.kind !== "invitation-response" ||
    !("invitationId" in value) ||
    typeof value.invitationId !== "string" ||
    value.invitationId.length === 0
  ) {
    return undefined;
  }

  return {
    invitationId: value.invitationId,
    kind: "invitation-response",
  };
}
