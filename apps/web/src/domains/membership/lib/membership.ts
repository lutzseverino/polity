export type MembershipInvitation = Readonly<{
  id: string;
  invitedAtLabel: string;
  invitedByName: string;
  polityName: string;
}>;

export type MembershipInvitationTokenContext = Readonly<{
  expiresAtLabel: string;
  invitedEmail: string;
  polityId: string;
  polityName: string;
}>;

export type MembershipInvitationCompletionStatus =
  | "requested"
  | "awaiting_identity"
  | "completed"
  | "failed";

export type MembershipInvitationCompletion = Readonly<{
  status: MembershipInvitationCompletionStatus;
}>;
