import { Trans, useLingui } from "@lingui/react/macro";
import { AlertTriangle } from "lucide-react";
import { type FormEvent, useState } from "react";

import {
  AppAlert,
  AppAlertDescription,
  AppAlertTitle,
} from "@/components/app/AppAlert";
import { AppButton } from "@/components/app/AppButton";
import { AppInput } from "@/components/app/AppInput";
import { AppText } from "@/components/app/AppText";
import { useSignIn } from "@/features/sign-in/api/sign-in-mutation";
import { SignInError } from "@/features/sign-in/api/sign-in-request";

type SignInWorkflowProps = Readonly<{
  onSignedIn: () => void;
}>;

export function SignInWorkflow({ onSignedIn }: SignInWorkflowProps) {
  const { i18n } = useLingui();
  const mutation = useSignIn(i18n.locale);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const errorKind =
    mutation.error instanceof SignInError
      ? mutation.error.kind
      : mutation.isError
        ? "unavailable"
        : undefined;

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    mutation.mutate(
      { email: email.trim(), password },
      { onSuccess: onSignedIn },
    );
  }

  return (
    <form className="space-y-5" onSubmit={submit}>
      <div>
        <AppText as="h1" variant="pageTitle">
          <Trans>Sign in</Trans>
        </AppText>
        <AppText className="mt-2" variant="supporting">
          <Trans>Use your identity account to continue to Polity.</Trans>
        </AppText>
      </div>

      {errorKind ? (
        <AppAlert aria-live="assertive" variant="destructive">
          <AlertTriangle aria-hidden="true" />
          <AppAlertTitle>
            {errorKind === "invalid-credentials" ? (
              <Trans>Email or password not recognized</Trans>
            ) : errorKind === "rejected" ? (
              <Trans>Sign in was rejected</Trans>
            ) : (
              <Trans>Couldn’t sign you in</Trans>
            )}
          </AppAlertTitle>
          <AppAlertDescription>
            {errorKind === "invalid-credentials" ? (
              <Trans>Check your details and try again.</Trans>
            ) : errorKind === "rejected" ? (
              <Trans>Refresh the page and try again.</Trans>
            ) : (
              <Trans>Check your connection and try again.</Trans>
            )}
          </AppAlertDescription>
        </AppAlert>
      ) : null}

      <div className="space-y-2">
        <label className="text-sm font-medium" htmlFor="sign-in-email">
          <Trans>Email</Trans>
        </label>
        <AppInput
          autoComplete="email"
          disabled={mutation.isPending}
          id="sign-in-email"
          onChange={(event) => setEmail(event.currentTarget.value)}
          required
          type="email"
          value={email}
        />
      </div>

      <div className="space-y-2">
        <label className="text-sm font-medium" htmlFor="sign-in-password">
          <Trans>Password</Trans>
        </label>
        <AppInput
          autoComplete="current-password"
          disabled={mutation.isPending}
          id="sign-in-password"
          onChange={(event) => setPassword(event.currentTarget.value)}
          required
          type="password"
          value={password}
        />
      </div>

      <AppButton className="w-full" disabled={mutation.isPending} type="submit">
        {mutation.isPending ? (
          <Trans>Signing in…</Trans>
        ) : (
          <Trans>Sign in</Trans>
        )}
      </AppButton>
    </form>
  );
}
