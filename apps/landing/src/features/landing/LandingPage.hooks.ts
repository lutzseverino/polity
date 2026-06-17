import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import type { OnboardingStep, Visibility } from "./LandingPage.types";

const startTransitionMs = 1050;
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function isVisibility(value: string): value is Visibility {
  return value === "public" || value === "private";
}

export function useLandingOnboarding() {
  const [polityName, setPolityName] = useState("");
  const [visibility, setVisibility] = useState<Visibility>("private");
  const [step, setStep] = useState<OnboardingStep>("founding");
  const [inviteEmail, setInviteEmail] = useState("");
  const [invites, setInvites] = useState<string[]>([]);
  const [nameError, setNameError] = useState("");
  const [inviteError, setInviteError] = useState("");
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
  const displayName = trimmedName || "Your first polity";
  const canStart = trimmedName.length > 0 && !isStarting;
  const hasNamedPolity = step !== "founding";

  const visibilityCopy = useMemo(
    () =>
      visibility === "private"
        ? "Private keeps the room closed to invited members."
        : "Public lets people find the charter before joining.",
    [visibility],
  );

  const updateVisibility = useCallback((value: string) => {
    if (isVisibility(value)) {
      setVisibility(value);
    }
  }, []);

  const startGovernment = useCallback(() => {
    setNameError("");

    if (!trimmedName) {
      setNameError("Name your polity before continuing.");
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
    setInviteError("");

    if (!email) {
      setInviteError("Enter an email address.");
      return;
    }

    if (!emailPattern.test(email)) {
      setInviteError("Use a valid email address.");
      return;
    }

    if (invites.includes(email)) {
      setInviteError("That person is already on the list.");
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
    inviteError,
    invites,
    isStarting,
    nameError,
    setPolityName,
    setInviteEmail,
    startGovernment,
    step,
    updateVisibility,
    visibility,
    visibilityCopy,
  };
}
