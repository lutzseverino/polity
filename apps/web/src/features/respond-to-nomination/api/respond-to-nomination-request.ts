export type NominationResponse = "accepted" | "declined";

export type RespondToNominationInput = Readonly<{
  motionId: string;
  polityId: string;
  response: NominationResponse;
}>;

export function respondToNomination(input: RespondToNominationInput) {
  return Promise.resolve(input);
}
