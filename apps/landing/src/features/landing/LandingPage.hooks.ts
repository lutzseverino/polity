import { useCallback, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";

import type { OnboardingStep, Visibility } from "./LandingPage.types";

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
type NameError = "nameRequired";
type InviteError = "emailRequired" | "emailInvalid" | "emailDuplicate";

function isVisibility(value: string): value is Visibility {
  return value === "public" || value === "private";
}

export function useLandingOnboarding() {
  const { t } = useTranslation("landing");
  const [polityName, setPolityName] = useState("");
  const [visibility, setVisibility] = useState<Visibility>("public");
  const [step, setStep] = useState<OnboardingStep | null>(null);
  const [inviteEmail, setInviteEmail] = useState("");
  const [invites, setInvites] = useState<string[]>([]);
  const [nameError, setNameError] = useState<NameError | null>(null);
  const [inviteError, setInviteError] = useState<InviteError | null>(null);
  const trimmedName = polityName.trim();
  const displayName = trimmedName || t("onboarding.defaultPolityName");
  const canStart = trimmedName.length > 0;

  const visibilityCopy = useMemo(
    () => t(`onboarding.visibility.options.${visibility}.description`),
    [t, visibility],
  );

  const updateVisibility = useCallback((value: string) => {
    if (isVisibility(value)) {
      setVisibility(value);
    }
  }, []);

  const startGovernment = useCallback(() => {
    setNameError(null);

    if (!trimmedName) {
      setNameError("nameRequired");
      return;
    }

    setStep("visibility");
  }, [trimmedName]);

  const continueToInvites = useCallback(() => {
    setStep("invites");
  }, []);

  const addInvite = useCallback(() => {
    const email = inviteEmail.trim().toLowerCase();
    setInviteError(null);

    if (!email) {
      setInviteError("emailRequired");
      return;
    }

    if (!emailPattern.test(email)) {
      setInviteError("emailInvalid");
      return;
    }

    if (invites.includes(email)) {
      setInviteError("emailDuplicate");
      return;
    }

    setInvites((current) => [...current, email]);
    setInviteEmail("");
  }, [inviteEmail, invites]);

  const finishSetup = useCallback(() => {
    setStep("ready");
  }, []);

  return {
    addInvite,
    canStart,
    continueToInvites,
    displayName,
    finishSetup,
    polityName,
    inviteEmail,
    inviteError: inviteError ? t(`onboarding.errors.${inviteError}`) : "",
    invites,
    nameError: nameError ? t(`onboarding.errors.${nameError}`) : "",
    setPolityName,
    setInviteEmail,
    startGovernment,
    step,
    updateVisibility,
    visibility,
    visibilityCopy,
  };
}
