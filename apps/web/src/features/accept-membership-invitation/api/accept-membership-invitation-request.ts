export type AcceptMembershipInvitationInput = Readonly<{
  invitationId: string;
}>;

export function acceptMembershipInvitation(
  input: AcceptMembershipInvitationInput,
) {
  return Promise.resolve(input);
}
