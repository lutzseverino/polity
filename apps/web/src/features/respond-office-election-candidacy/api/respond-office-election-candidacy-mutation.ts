import {
  mutationOptions,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";

import {
  type RespondOfficeElectionCandidacyInput,
  respondOfficeElectionCandidacy,
} from "@/features/respond-office-election-candidacy/api/respond-office-election-candidacy-request";

function respondOfficeElectionCandidacyMutationOptions(locale: string) {
  return mutationOptions<
    RespondOfficeElectionCandidacyInput,
    Error,
    RespondOfficeElectionCandidacyInput
  >({
    mutationFn: (input) =>
      respondOfficeElectionCandidacy({ ...input, acceptedLanguage: locale }),
    mutationKey: ["motions", "respond-office-election-candidacy"],
  });
}

export function useRespondOfficeElectionCandidacy(locale: string) {
  const queryClient = useQueryClient();
  return useMutation({
    ...respondOfficeElectionCandidacyMutationOptions(locale),
    onSuccess: (_, { polityId }) => {
      void queryClient.invalidateQueries({
        queryKey: ["polities", "detail", polityId],
      });
      void queryClient.invalidateQueries({ queryKey: ["inbox"] });
    },
  });
}
