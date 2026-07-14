import { mutationOptions, useMutation } from "@tanstack/react-query";

import {
  type RespondToNominationInput,
  respondToNomination,
} from "@/features/respond-to-nomination/api/respond-to-nomination-request";

function respondToNominationMutationOptions() {
  return mutationOptions<
    RespondToNominationInput,
    Error,
    RespondToNominationInput
  >({
    mutationFn: respondToNomination,
    mutationKey: ["motions", "respond-to-nomination"],
  });
}

export function useRespondToNomination() {
  return useMutation(respondToNominationMutationOptions());
}
