import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";

import type { OnboardingStep, Visibility } from "./LandingPage.types";

const startTransitionMs = 1050;
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
type NameError = "nameRequired";
type InviteError = "emailRequired" | "emailInvalid" | "emailDuplicate";

function isVisibility(value: string): value is Visibility {
  return value === "public" || value === "private";
}

export function useLandingOnboarding() {
  const { t } = useTranslation("landing");
  const [polityName, setPolityName] = useState("");
  const [visibility, setVisibility] = useState<Visibility>("private");
  const [step, setStep] = useState<OnboardingStep>("founding");
  const [inviteEmail, setInviteEmail] = useState("");
  const [invites, setInvites] = useState<string[]>([]);
  const [nameError, setNameError] = useState<NameError | null>(null);
  const [inviteError, setInviteError] = useState<InviteError | null>(null);
  const [isStarting, setIsStarting] = useState(false);
  const startTimer = useRef<number | undefined>(undefined);

  useEffect(() => {
    return () => {
      if (startTimer.current !== undefined) {
        window.clearTimeout(startTimer.current);
      }
    };
  }, []);

  const trimmedName = polityName.trim();
  const displayName = trimmedName || t("onboarding.defaultPolityName");
  const canStart = trimmedName.length > 0 && !isStarting;
  const hasNamedPolity = step !== "founding";

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

    setIsStarting(true);
    startTimer.current = window.setTimeout(() => {
      setStep("visibility");
      setIsStarting(false);
    }, startTransitionMs);
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
    hasNamedPolity,
    inviteEmail,
    inviteError: inviteError ? t(`onboarding.errors.${inviteError}`) : "",
    invites,
    isStarting,
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
