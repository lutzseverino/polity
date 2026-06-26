import {
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  Check,
  Landmark,
  Plus,
  Users,
} from "lucide-react";
import type { ComponentProps, FormEvent, MouseEvent, ReactNode } from "react";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { AppBadge, BadgeRemoveButton } from "@/components/app/app-badge";
import { AppButton } from "@/components/app/app-button";
import {
  AppCard,
  AppCardContent,
  AppCardHeader,
} from "@/components/app/app-card";
import { AppInput } from "@/components/app/app-input";
import { AppLink } from "@/components/app/app-link";
import {
  AppToggleGroup,
  AppToggleGroupItem,
} from "@/components/app/app-toggle-group";
import { Button } from "@/components/ui/button";
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldTitle,
} from "@/components/ui/field";
import { cn } from "@/lib/utils";
import { Emblem, Shards, Starburst } from "./motifs";
import { useFoundingTransition, useStepTransition } from "./motion";
import type { LandingOnboarding } from "./onboarding";
import { OnboardingStepper, StepIntro, SummaryGrid } from "./onboarding-flow";
import { OfficialRecordPlate } from "./record-plate";

type Article = {
  body: string;
  no: string;
  title: string;
};

type RegisterEntry = {
  detail: string;
  entry: string;
};

type EyebrowProps = Readonly<
  ComponentProps<"span"> & {
    children: ReactNode;
  }
>;

type LandingHeroProps = Readonly<{
  onboarding: LandingOnboarding;
}>;

type OnboardingStepProps = Readonly<{
  onboarding: LandingOnboarding;
}>;

type StepActionsProps = Readonly<{
  children: ReactNode;
  onboarding: LandingOnboarding;
}>;

const visibilityChoices = ["public", "private"] as const;
const paceChoices = ["fast", "standard", "deliberate"] as const;

function Eyebrow({ children, className, ...props }: EyebrowProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-2.5 font-mono text-[0.7rem] font-medium leading-none tracking-[0.28em] uppercase",
        className,
      )}
      {...props}
    >
      <span aria-hidden="true" className="block size-2 bg-primary" />
      <span className="block pt-px">{children}</span>
    </span>
  );
}

export function LandingHero({ onboarding }: LandingHeroProps) {
  const isNaming = onboarding.step === null;
  const { t } = useTranslation("landing");
  const { sectionRef, capture } = useFoundingTransition(!isNaming);
  const scrollToInstrument = (event: MouseEvent<HTMLButtonElement>) => {
    event.preventDefault();
    window.history.replaceState(
      null,
      "",
      `${window.location.pathname}${window.location.search}`,
    );
    const prefersReducedMotion = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    ).matches;
    document.getElementById("instrument")?.scrollIntoView({
      block: "start",
      behavior: prefersReducedMotion ? "auto" : "smooth",
    });
  };

  // Snapshot the poster geometry the instant before a boundary crossing so the
  // section-level visual can animate from the layout the visitor was seeing.
  const flow = useMemo<LandingOnboarding>(() => {
    const crossFoundingBoundary = (transition: () => void) => {
      capture();
      transition();
    };

    return {
      ...onboarding,
      startGovernment: () => crossFoundingBoundary(onboarding.startGovernment),
      goBack: () => {
        if (onboarding.step === "government") {
          crossFoundingBoundary(onboarding.goBack);
          return;
        }
        onboarding.goBack();
      },
      goToStep: (step) => {
        if (step === null) {
          crossFoundingBoundary(() => onboarding.goToStep(step));
          return;
        }
        onboarding.goToStep(step);
      },
    };
  }, [onboarding, capture]);

  return (
    <section
      className="relative isolate overflow-hidden border-b"
      data-founding-state={isNaming ? "naming" : "founding"}
      data-hero
      ref={sectionRef}
    >
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 z-10 overflow-visible bg-secondary will-change-[clip-path]"
        data-founding-visual
      >
        <div
          className="absolute inset-0 z-[1] grid place-items-center overflow-visible will-change-transform"
          data-hero-burst-shell
        >
          <Starburst
            className="aspect-square h-[150%] w-auto lg:h-[130%]"
            data-hero-burst
          />
        </div>
      </div>

      <div className="relative mx-auto grid max-w-7xl gap-0 lg:h-[100dvh] lg:grid-cols-[1.05fr_0.95fr] lg:overflow-hidden">
        {/* Left — stays mounted under the poster, so the ground covers/reveals it. */}
        <div
          aria-hidden={isNaming ? undefined : true}
          className={cn(
            "relative z-0 col-start-1 row-start-1 flex flex-col justify-center gap-8 border-border px-4 py-16 md:px-8 md:py-20 lg:border-r lg:py-24",
            !isNaming && "pointer-events-none",
          )}
          data-hero-title-layer
          inert={isNaming ? undefined : true}
        >
          <div className="flex flex-col gap-6">
            <Eyebrow data-hero-fade>{t("hero.eyebrow")}</Eyebrow>
            <h1 className="font-display text-[clamp(2.75rem,7vw,5.25rem)] leading-[0.92]">
              <span className="block overflow-hidden pb-[0.06em]">
                <span className="block" data-hero-line>
                  {t("hero.titleLine1")}
                </span>
              </span>{" "}
              <span className="block overflow-hidden pb-[0.06em]">
                <span className="block" data-hero-line>
                  {t("hero.titleLine2")}{" "}
                  <span className="text-primary">
                    {t("hero.titleEmphasis")}
                  </span>
                </span>
              </span>
            </h1>
            <p
              className="max-w-prose text-base leading-7 text-muted-foreground md:text-lg"
              data-hero-fade
            >
              {t("hero.body")}
            </p>
          </div>

          <div className="w-full max-w-md" data-hero-fade>
            <NameStep onboarding={flow} />
          </div>

          <Button
            aria-label={t("hero.cta")}
            className="w-fit gap-2 font-mono text-[0.7rem] leading-none tracking-[0.2em] uppercase"
            data-hero-fade
            onClick={scrollToInstrument}
            size="sm"
            type="button"
            variant="ghost"
          >
            {t("hero.cta")}
            <ArrowDown aria-hidden="true" data-bob />
          </Button>
        </div>

        {/* Right — the record stage; unfolds into the founding instrument */}
        <div
          className={cn(
            "relative z-20 flex min-h-[26rem] items-center justify-center px-6 py-16 text-secondary-foreground lg:min-h-0",
            isNaming ? "lg:py-16" : "lg:py-10",
            isNaming
              ? "lg:col-start-2 lg:row-start-1"
              : "col-start-1 row-start-1 lg:col-span-2 lg:col-start-1 lg:row-start-1",
          )}
          data-hero-stage
        >
          <Emblem
            className="absolute right-6 bottom-6 z-[2] size-20 text-secondary-foreground md:size-28"
            data-hero-stamp
          />

          <div className="relative z-[2] grid w-full place-items-center">
            <div
              className="col-start-1 row-start-1 w-full max-w-[20rem]"
              data-record-plate-surface
            >
              <OfficialRecordPlate />
            </div>
            <div
              aria-hidden={isNaming ? true : undefined}
              className="absolute top-1/2 left-1/2 w-full max-w-md -translate-x-1/2 -translate-y-1/2"
              data-onboarding-surface
            >
              <OnboardingPanel onboarding={flow} />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

export function MethodSection() {
  const { t } = useTranslation("landing");
  const articles = t("method.articles", {
    returnObjects: true,
  }) as Article[];

  return (
    <section
      className="mx-auto max-w-7xl px-4 py-20 md:px-8 md:py-28"
      id="instrument"
    >
      <div className="flex flex-col gap-5 border-b pb-10" data-reveal>
        <Eyebrow>{t("method.eyebrow")}</Eyebrow>
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
          <h2 className="font-display text-[clamp(2.25rem,5vw,4.25rem)] leading-[0.88]">
            {t("method.titleLine1")}
            <br />
            {t("method.titleLine2")}
          </h2>
          <p className="max-w-sm leading-7 text-muted-foreground md:text-right">
            {t("method.body")}
          </p>
        </div>
      </div>

      <div
        className="mt-10 grid gap-px border bg-border md:grid-cols-12"
        data-reveal-group
      >
        <article
          className="flex min-h-72 flex-col justify-between gap-8 bg-card p-7 md:col-span-5 md:p-9"
          data-reveal-item
        >
          <span className="block size-3 bg-primary" />
          <h3 className="font-display text-[clamp(2rem,3.6vw,3.25rem)] leading-[0.9]">
            {t("method.featureTitle")}
          </h3>
        </article>

        <article
          className="relative flex min-h-72 flex-col justify-between gap-8 overflow-hidden bg-primary p-7 text-primary-foreground md:col-span-7 md:p-9"
          data-reveal-item
        >
          <Shards className="absolute top-0 right-0 size-56 text-primary-foreground/15" />
          <span className="relative block size-3 bg-primary-foreground" />
          <p className="relative max-w-[16ch] font-display text-[clamp(2rem,3.6vw,3.25rem)] leading-[0.9]">
            {t("method.featureBody")}
          </p>
        </article>

        {articles.map(({ body, no, title }) => (
          <article
            className="group flex min-h-56 flex-col justify-between gap-6 bg-card p-6 transition-colors hover:bg-primary hover:text-primary-foreground md:col-span-3"
            data-reveal-item
            key={no}
          >
            <span className="font-display text-5xl leading-none text-primary transition-colors group-hover:text-primary-foreground">
              {no}
            </span>
            <div className="flex flex-col gap-2">
              <h3 className="font-display text-xl">{title}</h3>
              <p className="text-sm leading-6 text-muted-foreground transition-colors group-hover:text-primary-foreground/85">
                {body}
              </p>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

export function RecordSection() {
  const { t } = useTranslation("landing");
  const register = t("record.entries", {
    returnObjects: true,
  }) as RegisterEntry[];

  return (
    <section
      className="border-y bg-secondary text-secondary-foreground"
      id="record"
    >
      <div className="mx-auto grid max-w-7xl gap-0 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="relative flex flex-col justify-center gap-7 overflow-hidden px-4 py-20 md:px-8 md:py-28 lg:border-r lg:border-border/30">
          <Starburst
            className="absolute -top-72 -right-72 size-[36rem] text-primary"
            data-scrub
            inner={0.7}
          />
          <Eyebrow className="relative text-secondary-foreground" data-reveal>
            {t("record.eyebrow")}
          </Eyebrow>
          <h2
            className="relative max-w-[14ch] font-display text-[clamp(2.25rem,5vw,4.5rem)] leading-[0.86]"
            data-reveal
          >
            {t("record.title")}
          </h2>
          <p
            className="relative max-w-prose leading-7 text-secondary-foreground/70"
            data-reveal
          >
            {t("record.body")}
          </p>
        </div>

        <div
          className="flex flex-col justify-center gap-px bg-border/30 px-4 py-10 md:px-8 md:py-16"
          data-reveal-group
        >
          <p
            className="mb-3 px-1 font-mono text-[0.7rem] leading-none tracking-[0.28em] text-secondary-foreground/60 uppercase"
            data-reveal-item
          >
            {t("record.selectedEntries")}
          </p>
          {register.map(({ detail, entry }) => (
            <div
              className="flex items-baseline gap-4 bg-secondary py-4 font-mono text-sm"
              data-reveal-item
              key={entry}
            >
              <span className="text-primary tabular-nums">No. {entry}</span>
              <span className="text-secondary-foreground/85">{detail}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

export function Colophon() {
  const { t } = useTranslation("landing");

  return (
    <footer className="mx-auto max-w-7xl px-4 py-14 md:px-8">
      <div className="flex flex-col gap-10 md:flex-row md:items-end md:justify-between">
        <div className="flex items-center gap-5">
          <Emblem className="size-16 text-foreground" />
          <div className="flex flex-col gap-1">
            <span className="font-display text-2xl leading-none">decreos</span>
            <span className="max-w-[26ch] font-mono text-[0.7rem] tracking-[0.16em] text-muted-foreground uppercase">
              {t("colophon.tagline")}
            </span>
          </div>
        </div>
        <dl className="grid grid-cols-2 gap-x-10 gap-y-3 font-mono text-xs text-muted-foreground uppercase sm:grid-cols-3">
          <div className="flex flex-col gap-1">
            <dt className="tracking-[0.2em] opacity-60">
              {t("colophon.edition")}
            </dt>
            <dd className="text-foreground">{t("colophon.editionValue")}</dd>
          </div>
          <div className="flex flex-col gap-1">
            <dt className="tracking-[0.2em] opacity-60">
              {t("colophon.established")}
            </dt>
            <dd className="text-foreground">
              {t("colophon.establishedValue")}
            </dd>
          </div>
          <div className="flex flex-col gap-1">
            <dt className="tracking-[0.2em] opacity-60">
              {t("colophon.source")}
            </dt>
            <dd>
              <AppLink
                href="https://github.com/lutzseverino/polity"
                rel="noreferrer"
                target="_blank"
              >
                {t("colophon.sourceLink")}
              </AppLink>
            </dd>
          </div>
        </dl>
      </div>
    </footer>
  );
}

function useDisplayedOnboardingStep(step: LandingOnboarding["step"]) {
  const [lastStep, setLastStep] = useState(step);

  useEffect(() => {
    if (step !== null) {
      setLastStep(step);
    }
  }, [step]);

  return step ?? lastStep;
}

function OnboardingPanel({ onboarding }: OnboardingStepProps) {
  const { t } = useTranslation("landing");
  const displayStep = useDisplayedOnboardingStep(onboarding.step);
  const { stepRef } = useStepTransition(displayStep);

  return (
    <AppCard className="w-full bg-card text-left text-foreground">
      <AppCardHeader className="flex flex-row items-center justify-between bg-secondary text-secondary-foreground">
        <span className="font-mono text-[0.7rem] leading-none tracking-[0.24em] uppercase">
          {t("onboarding.panel.title")}
        </span>
        <span className="font-mono text-[0.7rem] leading-none tracking-[0.24em] text-secondary-foreground/60 uppercase">
          {t("onboarding.panel.number")}
        </span>
      </AppCardHeader>
      <AppCardContent className="grid gap-5">
        <OnboardingStepper
          onStepSelect={onboarding.goToStep}
          step={displayStep}
        />
        <div className="grid" data-onboarding-step={displayStep} ref={stepRef}>
          {displayStep === "government" ? (
            <GovernmentStep onboarding={onboarding} />
          ) : null}
          {displayStep === "visibility" ? (
            <VisibilityStep onboarding={onboarding} />
          ) : null}
          {displayStep === "invites" ? (
            <InviteStep onboarding={onboarding} />
          ) : null}
          {displayStep === "ready" ? (
            <ReadyStep onboarding={onboarding} />
          ) : null}
        </div>
      </AppCardContent>
    </AppCard>
  );
}

function GovernmentStep({ onboarding }: OnboardingStepProps) {
  const { t } = useTranslation("landing");

  return (
    <div className="grid gap-5">
      <StepIntro
        description={t("onboarding.government.description")}
        title={t("onboarding.government.title")}
      />

      <FieldGroup>
        <Field>
          <FieldTitle className="type-mono-label" id="setup-label">
            {t("onboarding.government.preset.label")}
          </FieldTitle>
          <div className="grid border-2 border-primary bg-primary p-4 text-primary-foreground">
            <div className="flex items-start gap-3">
              <Landmark
                aria-hidden="true"
                className="mt-0.5"
                data-icon="inline-start"
              />
              <span className="grid gap-1">
                <span className="font-display text-xl">
                  {t(
                    "onboarding.government.preset.options.standardRepublic.label",
                  )}
                </span>
                <span className="text-xs leading-5 text-primary-foreground/82">
                  {t(
                    "onboarding.government.preset.options.standardRepublic.copy",
                  )}
                </span>
              </span>
            </div>
          </div>
          <FieldDescription>
            {t(
              "onboarding.government.preset.options.standardRepublic.description",
            )}
          </FieldDescription>
        </Field>

        <Field>
          <FieldTitle className="type-mono-label" id="pace-label">
            {t("onboarding.government.pace.label")}
          </FieldTitle>
          <AppToggleGroup
            aria-labelledby="pace-label"
            className="grid w-full grid-cols-3 gap-3"
            onValueChange={onboarding.updatePace}
            type="single"
            value={onboarding.pace}
          >
            {paceChoices.map((choice) => (
              <AppToggleGroupItem
                className="justify-center text-center"
                key={choice}
                treatment="choice"
                value={choice}
              >
                <span className="grid gap-1">
                  <span className="font-display text-lg">
                    {t(`onboarding.government.pace.options.${choice}.label`)}
                  </span>
                  <span
                    className="text-xs leading-5 text-muted-foreground"
                    data-slot="choice-copy"
                  >
                    {t(`onboarding.government.pace.options.${choice}.copy`)}
                  </span>
                </span>
              </AppToggleGroupItem>
            ))}
          </AppToggleGroup>
          <FieldDescription>{onboarding.paceCopy}</FieldDescription>
        </Field>
      </FieldGroup>

      <StepActions onboarding={onboarding}>
        <AppButton onClick={onboarding.continueToVisibility} type="button">
          {t("onboarding.government.continue")}
          <ArrowRight data-icon="inline-end" />
        </AppButton>
      </StepActions>
    </div>
  );
}

function NameStep({ onboarding }: OnboardingStepProps) {
  const { t } = useTranslation("landing");
  const examples = useMemo(() => {
    const translatedExamples: unknown = t("onboarding.name.examples", {
      returnObjects: true,
    });

    return Array.isArray(translatedExamples)
      ? translatedExamples.filter(
          (item): item is string => typeof item === "string",
        )
      : [];
  }, [t]);
  const [exampleIndex, setExampleIndex] = useState(0);
  const example =
    examples.length > 0 ? examples[exampleIndex % examples.length] : "";

  useEffect(() => {
    if (examples.length < 2) {
      return;
    }

    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
    if (reduceMotion.matches) {
      return;
    }

    const interval = window.setInterval(() => {
      setExampleIndex((current) => (current + 1) % examples.length);
    }, 4800);

    return () => window.clearInterval(interval);
  }, [examples.length]);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onboarding.startGovernment();
  }

  return (
    <form className="grid gap-3" onSubmit={submit}>
      <FieldGroup>
        <Field data-invalid={onboarding.nameError ? true : undefined}>
          <FieldLabel className="type-mono-label" htmlFor="polity-name">
            {t("onboarding.name.label")}
          </FieldLabel>
          <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
            <AppInput
              animatedPlaceholder={example}
              aria-invalid={onboarding.nameError ? true : undefined}
              autoComplete="organization"
              id="polity-name"
              maxLength={120}
              onChange={(event) => onboarding.setPolityName(event.target.value)}
              placeholder={t("onboarding.name.placeholder")}
              value={onboarding.polityName}
            />
            <AppButton disabled={!onboarding.canStart} type="submit">
              {t("onboarding.name.submit")}
              <ArrowRight data-icon="inline-end" />
            </AppButton>
          </div>
          <FieldDescription className="font-mono text-[0.7rem] tracking-[0.1em]">
            {t("onboarding.name.description")}
          </FieldDescription>
          {onboarding.nameError ? (
            <FieldError>{onboarding.nameError}</FieldError>
          ) : null}
        </Field>
      </FieldGroup>
    </form>
  );
}

function VisibilityStep({ onboarding }: OnboardingStepProps) {
  const { t } = useTranslation("landing");

  return (
    <div className="grid gap-5">
      <StepIntro
        description={t("onboarding.visibility.description")}
        title={t("onboarding.visibility.title")}
      />

      <FieldGroup>
        <Field>
          <FieldTitle className="type-mono-label" id="visibility-label">
            {t("onboarding.visibility.label")}
          </FieldTitle>
          <AppToggleGroup
            aria-labelledby="visibility-label"
            className="grid w-full grid-cols-2 gap-3"
            onValueChange={onboarding.updateVisibility}
            type="single"
            value={onboarding.visibility}
          >
            {visibilityChoices.map((choice) => (
              <AppToggleGroupItem
                key={choice}
                treatment="choice"
                value={choice}
              >
                <span className="grid gap-1">
                  <span className="font-display text-lg">
                    {t(`onboarding.visibility.options.${choice}.label`)}
                  </span>
                  <span
                    className="text-xs leading-5 text-muted-foreground"
                    data-slot="choice-copy"
                  >
                    {t(`onboarding.visibility.options.${choice}.copy`)}
                  </span>
                </span>
              </AppToggleGroupItem>
            ))}
          </AppToggleGroup>
          <FieldDescription>{onboarding.visibilityCopy}</FieldDescription>
        </Field>
      </FieldGroup>

      <StepActions onboarding={onboarding}>
        <AppButton onClick={onboarding.continueToInvites} type="button">
          {t("onboarding.visibility.continue")}
          <ArrowRight data-icon="inline-end" />
        </AppButton>
      </StepActions>
    </div>
  );
}

function InviteStep({ onboarding }: OnboardingStepProps) {
  const { t } = useTranslation("landing");

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onboarding.addInvite();
  }

  return (
    <div className="grid gap-5">
      <StepIntro
        description={t("onboarding.invites.description")}
        title={t("onboarding.invites.title")}
      />

      <form className="grid gap-4" onSubmit={submit}>
        <FieldGroup>
          <Field data-invalid={onboarding.inviteError ? true : undefined}>
            <FieldLabel className="type-mono-label" htmlFor="invite-email">
              {t("onboarding.invites.label")}
            </FieldLabel>
            <AppInput
              aria-invalid={onboarding.inviteError ? true : undefined}
              autoComplete="email"
              id="invite-email"
              inputMode="email"
              onChange={(event) =>
                onboarding.setInviteEmail(event.target.value)
              }
              placeholder={t("onboarding.invites.placeholder")}
              type="email"
              value={onboarding.inviteEmail}
            />
            {onboarding.inviteError ? (
              <FieldError>{onboarding.inviteError}</FieldError>
            ) : null}
          </Field>
        </FieldGroup>

        <div className="grid grid-cols-[auto_1fr_1fr] gap-2">
          <BackStepButton onboarding={onboarding} />
          <AppButton type="submit" variant="secondary">
            <Plus data-icon="inline-start" />
            {t("onboarding.invites.enrol")}
          </AppButton>
          <AppButton onClick={onboarding.finishSetup} type="button">
            {t("onboarding.invites.finish")}
            <ArrowRight data-icon="inline-end" />
          </AppButton>
        </div>
      </form>

      <div aria-live="polite" className="flex flex-wrap gap-2">
        {onboarding.invites.length ? (
          onboarding.invites.map((email) => (
            <AppBadge key={email} treatment="removable">
              <Users aria-hidden="true" data-icon="inline-start" />
              <span className="min-w-0 truncate">{email}</span>
              <BadgeRemoveButton
                aria-label={t("onboarding.invites.remove", { email })}
                onClick={() => onboarding.removeInvite(email)}
              />
            </AppBadge>
          ))
        ) : (
          <p className="font-mono text-xs text-muted-foreground">
            {t("onboarding.invites.empty")}
          </p>
        )}
      </div>
    </div>
  );
}

function StepActions({ children, onboarding }: StepActionsProps) {
  return (
    <div className="grid grid-cols-[auto_1fr] gap-2">
      <BackStepButton onboarding={onboarding} />
      {children}
    </div>
  );
}

function BackStepButton({ onboarding }: OnboardingStepProps) {
  const { t } = useTranslation("landing");

  return (
    <AppButton
      aria-label={t("onboarding.back")}
      className="w-11 px-0"
      onClick={onboarding.goBack}
      type="button"
      variant="secondary"
    >
      <ArrowLeft aria-hidden="true" />
    </AppButton>
  );
}

function ReadyStep({ onboarding }: OnboardingStepProps) {
  const { t } = useTranslation("landing");

  return (
    <div className="grid gap-5">
      <div className="flex flex-col gap-3">
        <span className="inline-flex size-9 items-center justify-center bg-primary text-primary-foreground">
          <Check aria-hidden="true" className="size-5" />
        </span>
        <h2 className="font-display text-2xl leading-[0.95]">
          {t("onboarding.ready.title", { name: onboarding.displayName })}
        </h2>
        <p className="text-sm leading-6 text-muted-foreground">
          {t("onboarding.ready.description")}
        </p>
      </div>

      <SummaryGrid
        rows={[
          {
            label: t("onboarding.ready.government"),
            value: t(
              `onboarding.government.preset.options.${onboarding.setupPresetCopyKey}.label`,
            ),
          },
          {
            label: t("onboarding.ready.pace"),
            value: t(
              `onboarding.government.pace.options.${onboarding.pace}.label`,
            ),
          },
          {
            label: t("onboarding.ready.access"),
            value: t(
              `onboarding.visibility.options.${onboarding.visibility}.label`,
            ),
          },
          {
            label: t("onboarding.ready.members"),
            value: onboarding.invites.length
              ? onboarding.invites.join(", ")
              : t("onboarding.ready.enrolLater"),
          },
        ]}
      />

      <AppButton type="button">
        {t("onboarding.ready.enter")}
        <ArrowRight data-icon="inline-end" />
      </AppButton>
    </div>
  );
}
