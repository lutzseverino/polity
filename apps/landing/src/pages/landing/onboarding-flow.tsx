import { useTranslation } from "react-i18next";

import { Stepper, StepperStep } from "@/components/app/stepper";

import type { OnboardingStep } from "./onboarding";

type OnboardingStepperProps = Readonly<{
  onStepSelect: (step: OnboardingStep | null) => void;
  step: OnboardingStep | null;
}>;

type StepIntroProps = Readonly<{
  description: string;
  title: string;
}>;

type SummaryGridProps = Readonly<{
  rows: ReadonlyArray<
    Readonly<{
      label: string;
      value: string;
    }>
  >;
}>;

const flowSteps = [
  { key: "name", target: null },
  { key: "government", target: "government" },
  { key: "visibility", target: "visibility" },
  { key: "invites", target: "invites" },
] as const;
const stepIndex = {
  government: 1,
  invites: 3,
  ready: 4,
  visibility: 2,
} satisfies Record<OnboardingStep, number>;

export function OnboardingStepper({
  onStepSelect,
  step,
}: OnboardingStepperProps) {
  const { t } = useTranslation("landing");
  const currentIndex = step === null ? 0 : stepIndex[step];

  return (
    <Stepper>
      {flowSteps.map(({ key, target }, index) => {
        const isComplete = currentIndex > index;
        const isCurrent = currentIndex === index;
        const canNavigate = isComplete || isCurrent;
        const state = isComplete
          ? "complete"
          : isCurrent
            ? "current"
            : "upcoming";

        return (
          <StepperStep
            disabled={!canNavigate}
            key={key}
            label={t(`onboarding.flow.${key}`)}
            number={index + 1}
            onClick={() => onStepSelect(target)}
            state={state}
          />
        );
      })}
    </Stepper>
  );
}

export function StepIntro({ description, title }: StepIntroProps) {
  return (
    <div className="flex flex-col gap-1">
      <h2 className="font-display text-2xl">{title}</h2>
      <p className="text-sm leading-6 text-muted-foreground">{description}</p>
    </div>
  );
}

export function SummaryGrid({ rows }: SummaryGridProps) {
  return (
    <div className="grid gap-px border bg-border">
      {rows.map(({ label, value }) => (
        <div
          className="grid grid-cols-[7rem_1fr] items-center gap-3 bg-card px-4 py-3 text-sm"
          key={label}
        >
          <span className="font-mono text-[0.7rem] leading-none tracking-[0.16em] text-muted-foreground uppercase">
            {label}
          </span>
          <span className="truncate font-medium">{value}</span>
        </div>
      ))}
    </div>
  );
}
