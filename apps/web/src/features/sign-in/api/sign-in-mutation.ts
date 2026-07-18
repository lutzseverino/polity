import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  clearSessionDependentQueries,
  setCurrentSession,
} from "@/domains/session";
import {
  type SignInInput,
  signIn,
} from "@/features/sign-in/api/sign-in-request";

export function useSignIn(locale: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: SignInInput) =>
      signIn({ ...input, acceptedLanguage: locale }),
    mutationKey: ["session", "sign-in"],
    onSuccess: (session) => {
      clearSessionDependentQueries(queryClient);
      setCurrentSession(queryClient, session);
    },
  });
}
