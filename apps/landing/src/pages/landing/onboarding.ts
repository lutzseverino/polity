import { useCallback, useState } from "react";
import { useTranslation } from "react-i18next";

export type Visibility = "public" | "private";

export type SetupPreset = "standard_republic";

type SetupPresetCopyKey = "standardRepublic";

export type Pace = "fast" | "standard" | "deliberate";

export type OnboardingStep = "government" | "visibility" | "invites" | "ready";

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
type NameError = "nameRequired";
type InviteError = "emailRequired" | "emailInvalid" | "emailDuplicate";
const stepIndex = {
  government: 0,
  invites: 2,
  ready: 3,
  visibility: 1,
} satisfies Record<OnboardingStep, number>;
const previousStep = {
  government: null,
  invites: "visibility",
  ready: "invites",
  visibility: "government",
} satisfies Record<OnboardingStep, OnboardingStep | null>;
const setupPresetCopyKeys = {
  standard_republic: "standardRepublic",
} satisfies Record<SetupPreset, SetupPresetCopyKey>;

function isVisibility(value: string): value is Visibility {
  return value === "public" || value === "private";
}

function isPace(value: string): value is Pace {
  return value === "fast" || value === "standard" || value === "deliberate";
}

export function useLandingOnboarding() {
  const { t } = useTranslation("landing");
  const [polityName, setPolityName] = useState("");
  const [setupPreset] = useState<SetupPreset>("standard_republic");
  const [pace, setPace] = useState<Pace>("standard");
  const [visibility, setVisibility] = useState<Visibility>("public");
  const [step, setStep] = useState<OnboardingStep | null>(null);
  const [inviteEmail, setInviteEmail] = useState("");
  const [invites, setInvites] = useState<string[]>([]);
  const [nameError, setNameError] = useState<NameError | null>(null);
  const [inviteError, setInviteError] = useState<InviteError | null>(null);
  const trimmedName = polityName.trim();
  const displayName = trimmedName || t("onboarding.defaultPolityName");
  const canStart = trimmedName.length > 0;

  const visibilityCopy = t(
    `onboarding.visibility.options.${visibility}.description`,
  );
  const paceCopy = t(`onboarding.government.pace.options.${pace}.description`);

  const updateVisibility = useCallback((value: string) => {
    if (isVisibility(value)) {
      setVisibility(value);
    }
  }, []);

  const updatePace = useCallback((value: string) => {
    if (isPace(value)) {
      setPace(value);
    }
  }, []);

  const startGovernment = useCallback(() => {
    setNameError(null);

    if (!trimmedName) {
      setNameError("nameRequired");
      return;
    }

    setStep("government");
  }, [trimmedName]);

  const continueToVisibility = useCallback(() => {
    setStep("visibility");
  }, []);

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

  const removeInvite = useCallback((email: string) => {
    setInvites((current) => current.filter((invite) => invite !== email));
    setInviteError(null);
  }, []);

  const finishSetup = useCallback(() => {
    setStep("ready");
  }, []);

  const goToStep = useCallback(
    (nextStep: OnboardingStep | null) => {
      if (nextStep === null) {
        setStep(null);
        return;
      }
      if (step === null) {
        return;
      }
      if (stepIndex[nextStep] <= stepIndex[step]) {
        setStep(nextStep);
      }
    },
    [step],
  );

  const goBack = useCallback(() => {
    if (step !== null) {
      setStep(previousStep[step]);
    }
  }, [step]);

  return {
    addInvite,
    canStart,
    continueToInvites,
    continueToVisibility,
    displayName,
    finishSetup,
    goBack,
    goToStep,
    polityName,
    inviteEmail,
    inviteError: inviteError ? t(`onboarding.errors.${inviteError}`) : "",
    invites,
    nameError: nameError ? t(`onboarding.errors.${nameError}`) : "",
    pace,
    paceCopy,
    removeInvite,
    setPolityName,
    setInviteEmail,
    setupPreset,
    setupPresetCopyKey: setupPresetCopyKeys[setupPreset],
    startGovernment,
    step,
    updatePace,
    updateVisibility,
    visibility,
    visibilityCopy,
  };
}

export type LandingOnboarding = ReturnType<typeof useLandingOnboarding>;
