export type AcceptInvitationInput = Readonly<{
  invitationId: string;
}>;

export function acceptInvitation(input: AcceptInvitationInput) {
  return Promise.resolve(input);
}
