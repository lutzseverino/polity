import { useMutation, useQueryClient } from "@tanstack/react-query";

import {
  clearCurrentSession,
  clearSessionDependentQueries,
} from "@/domains/session";
import { signOut } from "@/features/sign-out/api/sign-out-request";

export function useSignOut(locale: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => signOut(locale),
    mutationKey: ["session", "sign-out"],
    onSuccess: () => {
      clearSessionDependentQueries(queryClient);
      clearCurrentSession(queryClient);
    },
  });
}
