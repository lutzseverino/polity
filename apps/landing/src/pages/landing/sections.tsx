import {
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  Check,
  Heart,
  Landmark,
  MessageSquare,
  Plus,
  Users,
} from "lucide-react";
import type { ComponentProps, FormEvent, MouseEvent, ReactNode } from "react";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { AppBadge, BadgeRemoveButton } from "@/components/app/app-badge";
import { AppButton } from "@/components/app/app-button";
import { AppCard, AppCardCluster } from "@/components/app/app-card";
import {
  AppField,
  AppFieldDescription,
  AppFieldError,
  AppFieldGroup,
  AppFieldLabel,
  AppFieldTitle,
} from "@/components/app/app-field";
import { AppInput } from "@/components/app/app-input";
import { AppLink } from "@/components/app/app-link";
import {
  AppToggleChoiceContent,
  AppToggleGroup,
  AppToggleGroupItem,
} from "@/components/app/app-toggle-group";
import { SectionHeading } from "@/components/app/section-heading";
import {
  TerminalDot,
  TerminalPanel,
  TerminalPanelHeader,
} from "@/components/app/terminal-panel";
import { TerminalRule } from "@/components/app/terminal-rule";
import { cn } from "@/lib/utils";
import { AsciiSeal } from "./ascii";
import { useStepTransition } from "./motion";
import type { LandingOnboarding } from "./onboarding";
import { OnboardingStepper, StepIntro, SummaryGrid } from "./onboarding-flow";
import { SealField } from "./seal-field";

type Article = {
  body: string;
  no: string;
  title: string;
};

type RegisterEntry = {
  detail: string;
  entry: string;
  kind: string;
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

/** Mono status tag — a bracketed record marker, the terminal's eyebrow. */
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
  const scrollToInstrument = (event: MouseEvent<HTMLButtonElement>) => {
    event.preventDefault();
    const prefersReducedMotion = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    ).matches;
    document.getElementById("instrument")?.scrollIntoView({
      block: "start",
      behavior: prefersReducedMotion ? "auto" : "smooth",
    });
  };

  return (
    <section className="relative isolate overflow-hidden border-b" data-hero>
      <div className="relative mx-auto grid max-w-7xl gap-0 lg:min-h-[calc(100dvh-3rem-1px)] lg:grid-cols-[1.05fr_0.95fr]">
        {/* Left — the proclamation and the founding command line. */}
        <div className="relative z-0 flex flex-col justify-center gap-8 px-4 py-16 md:px-8 md:py-20 lg:border-r lg:py-12">
          <div className="flex flex-col gap-6">
            <Eyebrow data-boot>{t("hero.eyebrow")}</Eyebrow>
            <h1 className="font-display text-[clamp(2.9rem,7.4vw,5.5rem)] leading-[0.9]">
              <span className="-mt-[0.2em] block overflow-hidden pt-[0.2em] pb-[0.06em]">
                <span className="block" data-boot-line>
                  {t("hero.titleLine1")}
                </span>
              </span>
              <span className="-mt-[0.2em] block overflow-hidden pt-[0.2em] pb-[0.06em]">
                <span className="block" data-boot-line>
                  {t("hero.titleLine2")}
                </span>
              </span>
              <span className="-mt-[0.2em] block overflow-hidden pt-[0.2em] pb-[0.06em]">
                <span className="block text-primary" data-boot-line>
                  {t("hero.titleEmphasis")}
                </span>
              </span>
            </h1>
            <p
              className="max-w-prose text-base leading-7 text-muted-foreground md:text-lg"
              data-boot
            >
              {t("hero.body")}
            </p>
          </div>

          <div className="w-full max-w-md" data-boot>
            <NameStep onboarding={onboarding} />
          </div>

          <AppButton
            aria-label={t("hero.cta")}
            className="w-fit gap-2 font-mono text-[0.7rem] leading-none tracking-[0.2em] text-muted-foreground uppercase hover:text-foreground"
            data-boot
            onClick={scrollToInstrument}
            size="sm"
            treatment="plain"
            type="button"
            variant="ghost"
          >
            {t("hero.cta")}
            <ArrowDown aria-hidden="true" data-bob />
          </AppButton>
        </div>

        {/* Right — the console: the seal field powers on, then yields to the
            flow. The field is struck full-bleed across the pane; the console
            crossfades in over it once a name is entered. */}
        <div className="relative flex min-h-[28rem] items-center justify-center border-t px-4 py-10 md:px-8 lg:min-h-0 lg:border-t-0 lg:py-12">
          <div className="pointer-events-none absolute inset-0 texture-scanlines opacity-40" />

          {/* Seal field — the resting state while you name the polity. */}
          <SealField
            active={isNaming}
            className={cn(
              "absolute inset-0 transition-opacity duration-500",
              isNaming
                ? "opacity-100"
                : "pointer-events-none opacity-0 duration-300",
            )}
          />

          {/* Console layer — the founding flow, once a name is entered. */}
          <div
            aria-hidden={isNaming}
            className={cn(
              "relative w-full max-w-md transition-opacity duration-500",
              isNaming
                ? "pointer-events-none opacity-0 duration-300"
                : "opacity-100 delay-150",
            )}
            inert={isNaming ? true : undefined}
          >
            <OnboardingConsole onboarding={onboarding} />
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
      <div className="flex flex-col gap-6 border-b pb-10" data-reveal>
        <TerminalRule label={t("method.eyebrow")} tail="§ 01" />
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
          <h2 className="font-display text-[clamp(2.25rem,5vw,4.25rem)] leading-[0.86]">
            {t("method.titleLine1")}
            <br />
            {t("method.titleLine2")}
          </h2>
          <p className="max-w-sm leading-7 text-muted-foreground md:text-right">
            {t("method.body")}
          </p>
        </div>
      </div>

      <AppCardCluster className="mt-10 md:grid-cols-12" data-reveal-group>
        <AppCard
          as="article"
          className="flex min-h-72 flex-col justify-between gap-8 md:col-span-5"
          data-reveal-item
          padding="lg"
        >
          <span className="block size-3 bg-primary" />
          <h3 className="font-display text-[clamp(2rem,3.6vw,3.25rem)] leading-[0.9]">
            {t("method.featureTitle")}
          </h3>
        </AppCard>

        <AppCard
          as="article"
          className="relative flex min-h-72 flex-col justify-between gap-8 overflow-hidden md:col-span-7"
          data-reveal-item
          padding="lg"
          tone="primary"
        >
          <AsciiSeal
            className="pointer-events-none absolute -top-16 -right-14 text-[0.6rem] text-primary-foreground/20"
            monochrome
          />
          <span className="relative block size-3 bg-primary-foreground" />
          <p className="relative max-w-[16ch] font-display text-[clamp(2rem,3.6vw,3.25rem)] leading-[0.9]">
            {t("method.featureBody")}
          </p>
        </AppCard>

        {articles.map(({ body, no, title }) => (
          <AppCard
            as="article"
            className="group flex min-h-56 flex-col justify-between gap-6 transition-colors hover:bg-primary hover:text-primary-foreground md:col-span-3"
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
          </AppCard>
        ))}
      </AppCardCluster>
    </section>
  );
}

export function RecordSection() {
  const { t } = useTranslation("landing");
  const register = t("record.entries", {
    returnObjects: true,
  }) as RegisterEntry[];

  return (
    <section className="border-y bg-card" id="record">
      <div className="mx-auto grid max-w-7xl gap-0 lg:grid-cols-[1fr_1.1fr]">
        <div className="flex flex-col justify-center px-4 py-20 md:px-8 md:py-28 lg:border-r">
          <SectionHeading
            eyebrow={t("record.eyebrow")}
            index="§ 02"
            lead={t("record.body")}
          >
            {t("record.title")}
          </SectionHeading>
        </div>

        {/* The official record, rendered as a printed ledger. */}
        <div
          className="flex flex-col justify-center px-4 py-14 md:px-8 md:py-20"
          data-reveal-group
        >
          <TerminalPanel className="bg-background">
            <TerminalPanelHeader
              className="px-4"
              end={t("record.selectedEntries")}
              start={t("record.ledger")}
            />
            <ol>
              {register.map(({ detail, entry, kind }) => (
                <li
                  className="grid grid-cols-[3.25rem_1fr_auto] items-baseline gap-4 border-b border-border/60 px-4 py-3.5 font-mono text-sm last:border-b-0"
                  data-ledger-line
                  data-reveal-item
                  key={entry}
                >
                  <span className="text-primary tabular-nums">{entry}</span>
                  <span className="text-foreground">{detail}</span>
                  <span className="text-[0.6rem] tracking-[0.2em] text-muted-foreground/70 uppercase">
                    {kind}
                  </span>
                </li>
              ))}
            </ol>
            <div className="flex items-center gap-2 px-4 py-3 font-mono text-[0.7rem] text-muted-foreground">
              <span className="text-primary">$</span>
              <span>{t("record.append")}</span>
              <span aria-hidden="true" className="terminal-caret" />
            </div>
          </TerminalPanel>
        </div>
      </div>
    </section>
  );
}

export function SocialSection() {
  const { t } = useTranslation("landing");

  return (
    <section className="mx-auto max-w-7xl px-4 py-20 md:px-8 md:py-28">
      <div className="grid gap-12 lg:grid-cols-[0.95fr_1.05fr] lg:items-center lg:gap-16">
        <SectionHeading
          eyebrow={t("social.eyebrow")}
          index="§ 03"
          lead={t("social.body")}
        >
          {t("social.titleLine1")}
          <br />
          <span className="text-primary">{t("social.titleLine2")}</span>
        </SectionHeading>

        {/* One motion carrying the vote that decides it and the social layer
            beside it. */}
        <figure className="flex flex-col gap-0" data-reveal>
          <TerminalPanel>
            <TerminalPanelHeader
              className="px-4 py-3"
              end={
                <>
                  <TerminalDot pulse />
                  {t("social.motion.status")}
                </>
              }
              muted={false}
              start={
                <span className="text-foreground">
                  {t("social.motion.label")}
                </span>
              }
            />
            <div className="px-4 pt-6 pb-5">
              <p className="text-lg leading-7">{t("social.motion.text")}</p>
              <p className="mt-2 font-mono text-[0.62rem] tracking-[0.18em] text-muted-foreground/70 uppercase">
                {t("social.motion.author")}
              </p>
            </div>
            {/* The vote - what actually decides the motion. */}
            <div className="flex flex-wrap items-center gap-x-4 gap-y-2 border-t border-primary/40 bg-primary/10 px-4 py-3 font-mono text-xs">
              <span className="text-[0.58rem] tracking-[0.2em] text-primary uppercase">
                {t("social.motion.vote")}
              </span>
              <span className="text-foreground tabular-nums">
                {t("social.motion.tally")}
              </span>
              <span className="ml-auto inline-flex items-center gap-1.5 text-[0.62rem] text-muted-foreground uppercase">
                <Check aria-hidden="true" className="size-3.5 text-primary" />
                {t("social.motion.quorum")}
              </span>
            </div>
            {/* The social layer - conversation around the motion. */}
            <div className="flex flex-wrap items-center gap-x-4 gap-y-2 border-t px-4 py-3 font-mono text-xs text-muted-foreground">
              <span className="text-[0.58rem] tracking-[0.2em] text-muted-foreground/70 uppercase">
                {t("social.motion.discussion")}
              </span>
              <span className="ml-auto flex items-center gap-5">
                <span className="inline-flex items-center gap-1.5">
                  <Heart aria-hidden="true" className="size-3.5" />
                  {t("social.motion.likes")}
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <MessageSquare aria-hidden="true" className="size-3.5" />
                  {t("social.motion.comments")}
                </span>
              </span>
            </div>
          </TerminalPanel>
          <figcaption className="flex items-center gap-3 border border-t-0 bg-background px-4 py-2.5 font-mono text-[0.66rem] leading-none tracking-[0.16em] text-muted-foreground uppercase">
            <span aria-hidden="true" className="text-primary">
              ▸
            </span>
            {t("social.caption")}
          </figcaption>
        </figure>
      </div>
    </section>
  );
}

export function Colophon() {
  const { t } = useTranslation("landing");

  return (
    <footer className="relative overflow-hidden border-t">
      {/* The seal, struck oversized and bleeding off the bottom-left corner. */}
      <AsciiSeal
        aria-hidden="true"
        className="pointer-events-none absolute -bottom-24 -left-16 text-[1.15rem] sm:-left-20 sm:text-[1.5rem] lg:text-[1.85rem]"
      />
      <div className="relative mx-auto flex min-h-[20rem] max-w-7xl flex-col items-end justify-end gap-5 px-4 py-16 text-right md:min-h-[26rem] md:px-8">
        <h2 className="max-w-[13ch] font-display text-[clamp(2.5rem,6vw,5rem)] leading-[0.82]">
          {t("colophon.transmission")}
        </h2>
        <p className="font-mono text-[0.66rem] leading-none tracking-[0.24em] text-muted-foreground uppercase">
          decreos · {t("colophon.establishedValue")} ·{" "}
          <AppLink
            href="https://github.com/lutzseverino/polity"
            rel="noreferrer"
            target="_blank"
          >
            {t("colophon.sourceLink")}
          </AppLink>
        </p>
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

/** The founding flow, framed as a terminal window with a title bar. */
function OnboardingConsole({ onboarding }: OnboardingStepProps) {
  const { t } = useTranslation("landing");
  const displayStep = useDisplayedOnboardingStep(onboarding.step);
  const { stepRef } = useStepTransition(displayStep);

  return (
    <TerminalPanel className="w-full text-left text-foreground shadow-[0_0_0_1px_var(--border),0_24px_60px_-30px_oklch(0_0_0/0.7)]">
      <TerminalPanelHeader
        end={t("onboarding.panel.number")}
        start={
          <>
            <TerminalDot />
            {t("onboarding.panel.title")}
          </>
        }
      />
      <div className="grid gap-5 p-5">
        <OnboardingStepper
          onStepSelect={onboarding.goToStep}
          step={displayStep}
        />
        <div className="grid" data-onboarding-step={displayStep} ref={stepRef}>
          {displayStep === "invites" ? (
            <InviteStep onboarding={onboarding} />
          ) : null}
          {displayStep === "visibility" ? (
            <VisibilityStep onboarding={onboarding} />
          ) : null}
          {displayStep === "preset" ? (
            <PresetStep onboarding={onboarding} />
          ) : null}
          {displayStep === "pace" ? <PaceStep onboarding={onboarding} /> : null}
          {displayStep === "ready" ? (
            <ReadyStep onboarding={onboarding} />
          ) : null}
        </div>
      </div>
    </TerminalPanel>
  );
}

function PresetStep({ onboarding }: OnboardingStepProps) {
  const { t } = useTranslation("landing");

  return (
    <div className="grid gap-4">
      <StepIntro
        description={t("onboarding.preset.description")}
        title={t("onboarding.preset.title")}
      />

      <AppFieldGroup>
        <AppField>
          <AppFieldTitle className="type-mono-label" id="setup-label">
            {t("onboarding.preset.label")}
          </AppFieldTitle>
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
                    `onboarding.preset.options.${onboarding.setupPresetCopyKey}.label`,
                  )}
                </span>
                <span className="text-xs leading-5 text-primary-foreground/82">
                  {t(
                    `onboarding.preset.options.${onboarding.setupPresetCopyKey}.copy`,
                  )}
                </span>
              </span>
            </div>
          </div>
          <AppFieldDescription>
            {t(
              `onboarding.preset.options.${onboarding.setupPresetCopyKey}.description`,
            )}
          </AppFieldDescription>
        </AppField>
      </AppFieldGroup>

      <StepActions onboarding={onboarding}>
        <AppButton onClick={onboarding.continueToPace} type="button">
          {t("onboarding.preset.continue")}
          <ArrowRight data-icon="inline-end" />
        </AppButton>
      </StepActions>
    </div>
  );
}

function PaceStep({ onboarding }: OnboardingStepProps) {
  const { t } = useTranslation("landing");

  return (
    <div className="grid gap-4">
      <StepIntro
        description={t("onboarding.pace.description")}
        title={t("onboarding.pace.title")}
      />

      <AppFieldGroup>
        <AppField>
          <AppFieldTitle className="type-mono-label" id="pace-label">
            {t("onboarding.pace.label")}
          </AppFieldTitle>
          <AppToggleGroup
            aria-labelledby="pace-label"
            className="grid w-full grid-cols-1 gap-3 sm:grid-cols-3"
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
                <AppToggleChoiceContent
                  copy={t(`onboarding.pace.options.${choice}.copy`)}
                  label={t(`onboarding.pace.options.${choice}.label`)}
                />
              </AppToggleGroupItem>
            ))}
          </AppToggleGroup>
          <AppFieldDescription>{onboarding.paceCopy}</AppFieldDescription>
        </AppField>
      </AppFieldGroup>

      <StepActions onboarding={onboarding}>
        <AppButton onClick={onboarding.finishSetup} type="button">
          {t("onboarding.pace.finish")}
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
    onboarding.startInvites();
  }

  return (
    <form className="grid gap-3" onSubmit={submit}>
      <AppFieldGroup>
        <AppField data-invalid={onboarding.nameError ? true : undefined}>
          <AppFieldLabel
            className="flex items-center gap-1.5 font-mono text-[0.7rem] leading-none tracking-[0.16em] normal-case"
            htmlFor="polity-name"
          >
            <span className="text-primary">{t("hero.command.prompt")}</span>
          </AppFieldLabel>
          <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
            <AppInput
              animatedPlaceholder={example}
              aria-invalid={onboarding.nameError ? true : undefined}
              aria-label={t("onboarding.name.label")}
              autoComplete="organization"
              id="polity-name"
              maxLength={120}
              name="polityName"
              onChange={(event) => onboarding.setPolityName(event.target.value)}
              placeholder={t("onboarding.name.placeholder")}
              value={onboarding.polityName}
            />
            <AppButton disabled={!onboarding.canStart} type="submit">
              {t("onboarding.name.submit")}
              <ArrowRight data-icon="inline-end" />
            </AppButton>
          </div>
          <AppFieldDescription className="font-mono text-[0.7rem] tracking-[0.1em]">
            {t("onboarding.name.description")}
          </AppFieldDescription>
          {onboarding.nameError ? (
            <AppFieldError>{onboarding.nameError}</AppFieldError>
          ) : null}
        </AppField>
      </AppFieldGroup>
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

      <AppFieldGroup>
        <AppField>
          <AppFieldTitle className="type-mono-label" id="visibility-label">
            {t("onboarding.visibility.label")}
          </AppFieldTitle>
          <AppToggleGroup
            aria-labelledby="visibility-label"
            className="grid w-full grid-cols-1 gap-3 sm:grid-cols-2"
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
                <AppToggleChoiceContent
                  copy={t(`onboarding.visibility.options.${choice}.copy`)}
                  label={t(`onboarding.visibility.options.${choice}.label`)}
                />
              </AppToggleGroupItem>
            ))}
          </AppToggleGroup>
          <AppFieldDescription>{onboarding.visibilityCopy}</AppFieldDescription>
        </AppField>
      </AppFieldGroup>

      <StepActions onboarding={onboarding}>
        <AppButton onClick={onboarding.continueToPreset} type="button">
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
    <div className="grid gap-4">
      <StepIntro
        description={t("onboarding.invites.description")}
        title={t("onboarding.invites.title")}
      />

      <form className="grid gap-4" onSubmit={submit}>
        <AppFieldGroup>
          <AppField data-invalid={onboarding.inviteError ? true : undefined}>
            <AppFieldLabel className="type-mono-label" htmlFor="invite-email">
              {t("onboarding.invites.label")}
            </AppFieldLabel>
            <AppInput
              aria-invalid={onboarding.inviteError ? true : undefined}
              autoComplete="email"
              id="invite-email"
              inputMode="email"
              name="inviteEmail"
              onChange={(event) =>
                onboarding.setInviteEmail(event.target.value)
              }
              placeholder={t("onboarding.invites.placeholder")}
              spellCheck={false}
              type="email"
              value={onboarding.inviteEmail}
            />
            {onboarding.inviteError ? (
              <AppFieldError>{onboarding.inviteError}</AppFieldError>
            ) : null}
          </AppField>
        </AppFieldGroup>

        <div className="grid grid-cols-[auto_1fr] gap-2 sm:grid-cols-[auto_1fr_1fr]">
          <BackStepButton onboarding={onboarding} />
          <AppButton type="submit">
            <Plus data-icon="inline-start" />
            {t("onboarding.invites.enrol")}
          </AppButton>
          <AppButton
            className="col-span-2 sm:col-span-1"
            onClick={onboarding.continueToVisibility}
            type="button"
            variant="secondary"
          >
            {t(
              onboarding.invites.length
                ? "onboarding.invites.continue"
                : "onboarding.invites.skip",
            )}
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
              `onboarding.preset.options.${onboarding.setupPresetCopyKey}.label`,
            ),
          },
          {
            label: t("onboarding.ready.pace"),
            value: t(`onboarding.pace.options.${onboarding.pace}.label`),
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
