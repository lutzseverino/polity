export type OfficeElectionCandidacyResponse = "accepted" | "declined";

export type RespondOfficeElectionCandidacyInput = Readonly<{
  motionId: string;
  polityId: string;
  response: OfficeElectionCandidacyResponse;
}>;

export function respondOfficeElectionCandidacy(
  input: RespondOfficeElectionCandidacyInput,
) {
  return Promise.resolve(input);
}
