import { mutationOptions, useMutation } from "@tanstack/react-query";

import {
  type RespondOfficeElectionCandidacyInput,
  respondOfficeElectionCandidacy,
} from "@/features/respond-office-election-candidacy/api/respond-office-election-candidacy-request";

function respondOfficeElectionCandidacyMutationOptions() {
  return mutationOptions<
    RespondOfficeElectionCandidacyInput,
    Error,
    RespondOfficeElectionCandidacyInput
  >({
    mutationFn: respondOfficeElectionCandidacy,
    mutationKey: ["motions", "respond-office-election-candidacy"],
  });
}

export function useRespondOfficeElectionCandidacy() {
  return useMutation(respondOfficeElectionCandidacyMutationOptions());
}
