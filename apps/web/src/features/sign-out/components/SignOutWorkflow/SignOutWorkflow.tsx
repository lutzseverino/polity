import { Trans, useLingui } from "@lingui/react/macro";

import {
  AppAlert,
  AppAlertDescription,
  AppAlertTitle,
} from "@/components/app/AppAlert";
import { AppButton } from "@/components/app/AppButton";
import { useSignOut } from "@/features/sign-out/api/sign-out-mutation";

export function SignOutWorkflow({
  onSignedOut,
}: Readonly<{ onSignedOut: () => void }>) {
  const { i18n } = useLingui();
  const mutation = useSignOut(i18n.locale);

  return (
    <div className="space-y-3">
      {mutation.isError ? (
        <AppAlert aria-live="assertive" variant="destructive">
          <AppAlertTitle>
            <Trans>Couldn’t sign you out</Trans>
          </AppAlertTitle>
          <AppAlertDescription>
            <Trans>Check your connection and try again.</Trans>
          </AppAlertDescription>
        </AppAlert>
      ) : null}
      <AppButton
        disabled={mutation.isPending}
        onClick={() => mutation.mutate(undefined, { onSuccess: onSignedOut })}
        variant="outline"
      >
        {mutation.isPending ? (
          <Trans>Signing out…</Trans>
        ) : (
          <Trans>Sign out</Trans>
        )}
      </AppButton>
    </div>
  );
}
