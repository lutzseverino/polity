import { ArrowDown, ArrowRight, Check, Plus, Users } from "lucide-react";
import type { ComponentProps, FormEvent, ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { AppButton } from "@/components/app/app-button";
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

import type { LandingOnboarding } from "./LandingPage";
import { Emblem, Shards, Starburst } from "./LandingPage.motifs";

type Article = {
  body: string;
  no: string;
  title: string;
};

type RegisterEntry = {
  detail: string;
  entry: string;
};

export function Eyebrow({
  children,
  className,
  ...props
}: ComponentProps<"span"> & {
  children: ReactNode;
}) {
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

export function LandingHero({ onboarding }: { onboarding: LandingOnboarding }) {
  const isNaming = onboarding.step === null;
  const { t } = useTranslation("landing");

  return (
    <section className="relative isolate overflow-hidden border-b" data-hero>
      <div className="mx-auto grid max-w-7xl gap-0 lg:min-h-[calc(100dvh-3.5rem)] lg:grid-cols-[1.05fr_0.95fr]">
        {/* Left — the founding */}
        <div className="flex flex-col justify-center gap-8 border-border px-4 py-16 md:px-8 md:py-20 lg:border-r lg:py-24">
          <div
            className={cn(
              "flex flex-col gap-6 transition-all duration-500 ease-out motion-reduce:transition-none",
              isNaming
                ? "max-h-[44rem] opacity-100"
                : "pointer-events-none -translate-y-2 max-h-0 overflow-hidden opacity-0",
            )}
          >
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

          <div data-hero-fade>
            <FoundingFlow onboarding={onboarding} />
          </div>

          {isNaming ? (
            <Button
              aria-label={t("hero.cta")}
              asChild
              className="w-fit gap-2 font-mono text-[0.7rem] leading-none tracking-[0.2em] uppercase"
              data-hero-fade
              size="sm"
              variant="ghost"
            >
              <a href="#instrument">
                {t("hero.cta")}
                <ArrowDown aria-hidden="true" data-bob />
              </a>
            </Button>
          ) : null}
        </div>

        {/* Right — the first entry in the official record */}
        <div className="relative flex min-h-[26rem] items-center justify-center overflow-hidden bg-secondary px-6 py-16 text-secondary-foreground lg:min-h-0">
          <div
            aria-hidden="true"
            className="pointer-events-none absolute inset-0 grid place-items-center overflow-hidden"
          >
            <Starburst className="aspect-square w-[155%]" data-hero-burst />
          </div>

          <Emblem
            className="absolute top-6 right-6 size-20 text-secondary-foreground md:size-28"
            data-hero-stamp
          />

          <figure
            className="relative w-full max-w-[20rem] border-[3px] border-secondary bg-secondary text-secondary-foreground"
            data-hero-plate
          >
            <figcaption className="flex items-center justify-between border-b border-secondary-foreground/20 px-3 py-2 font-mono text-[0.58rem] leading-none tracking-[0.24em] text-secondary-foreground/85 uppercase">
              <span data-hero-mark>{t("hero.plate.official")}</span>
              <span data-hero-mark>{t("hero.plate.number")}</span>
              <span data-hero-mark>{t("hero.plate.record")}</span>
            </figcaption>
            <div className="relative aspect-[16/9] overflow-hidden">
              <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_42%,color-mix(in_oklch,var(--secondary-foreground)_34%,transparent),transparent_68%)]" />
              <div className="absolute inset-0 text-secondary-foreground opacity-70 texture-halftone [--dot:4px]" />
              <div className="absolute inset-0 text-secondary-foreground opacity-25 texture-halftone [--dot:11px]" />
            </div>
            <div
              className="border-t border-secondary-foreground/20 px-3 py-2 text-center font-mono text-[0.58rem] leading-none tracking-[0.34em] text-secondary-foreground/85 uppercase"
              data-hero-mark
            >
              {t("hero.plate.founded")}
            </div>
          </figure>
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

function FoundingFlow({ onboarding }: { onboarding: LandingOnboarding }) {
  const { t } = useTranslation("landing");

  return (
    <div
      className="w-full max-w-md transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] motion-reduce:transition-none"
    >
      {onboarding.step === null ? (
        <NameStep onboarding={onboarding} />
      ) : (
        <div className="border bg-card text-left">
          <div className="flex items-center justify-between border-b bg-secondary px-4 py-2.5 text-secondary-foreground">
            <span className="font-mono text-[0.7rem] leading-none tracking-[0.24em] uppercase">
              {t("onboarding.panel.title")}
            </span>
            <span className="font-mono text-[0.7rem] leading-none tracking-[0.24em] text-secondary-foreground/60 uppercase">
              {t("onboarding.panel.number")}
            </span>
          </div>
          <div className="grid gap-5 p-5">
            <FlowStepper step={onboarding.step} />
            {onboarding.step === "visibility" ? (
              <VisibilityStep onboarding={onboarding} />
            ) : null}
            {onboarding.step === "invites" ? (
              <InviteStep onboarding={onboarding} />
            ) : null}
            {onboarding.step === "ready" ? (
              <ReadyStep onboarding={onboarding} />
            ) : null}
          </div>
        </div>
      )}
    </div>
  );
}

function FlowStepper({ step }: { step: NonNullable<LandingOnboarding["step"]> }) {
  const { t } = useTranslation("landing");
  const flowSteps = [
    { key: "name", label: t("onboarding.flow.name") },
    { key: "visibility", label: t("onboarding.flow.visibility") },
    { key: "invites", label: t("onboarding.flow.invites") },
  ] as const;
  const currentIndex =
    step === "visibility" ? 1 : step === "invites" ? 2 : 3;

  return (
    <ol className="grid grid-cols-3 gap-2">
      {flowSteps.map(({ key, label }, index) => {
        const isComplete = currentIndex > index;
        const isCurrent = currentIndex === index;

        return (
          <li className="flex flex-col gap-1.5" key={key}>
            <div
              className={cn(
                "h-1 transition-colors",
                isComplete || isCurrent ? "bg-primary" : "bg-muted",
              )}
            />
            <span
              className={cn(
                "font-mono text-[0.65rem] leading-none tracking-[0.18em] uppercase",
                isCurrent ? "text-foreground" : "text-muted-foreground",
              )}
            >
              {`0${index + 1} · ${label}`}
            </span>
          </li>
        );
      })}
    </ol>
  );
}

function NameStep({ onboarding }: { onboarding: LandingOnboarding }) {
  const { t } = useTranslation("landing");

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

function VisibilityStep({ onboarding }: { onboarding: LandingOnboarding }) {
  const { t } = useTranslation("landing");

  return (
    <div className="grid gap-5">
      <div className="flex flex-col gap-1">
        <h2 className="font-display text-2xl">
          {t("onboarding.visibility.title")}
        </h2>
        <p className="text-sm leading-6 text-muted-foreground">
          {t("onboarding.visibility.description")}
        </p>
      </div>

      <FieldGroup>
        <Field>
          <FieldTitle className="type-mono-label" id="visibility-label">
            {t("onboarding.visibility.label")}
          </FieldTitle>
          <AppToggleGroup
            aria-labelledby="visibility-label"
            className="grid w-full grid-cols-1 gap-3 sm:grid-cols-2"
            onValueChange={onboarding.updateVisibility}
            type="single"
            value={onboarding.visibility}
          >
            <AppToggleGroupItem treatment="choice" value="private">
              <span className="grid gap-1">
                <span className="font-display text-lg">
                  {t("onboarding.visibility.options.private.label")}
                </span>
                <span
                  className="text-xs leading-5 text-muted-foreground"
                  data-slot="choice-copy"
                >
                  {t("onboarding.visibility.options.private.copy")}
                </span>
              </span>
            </AppToggleGroupItem>
            <AppToggleGroupItem treatment="choice" value="public">
              <span className="grid gap-1">
                <span className="font-display text-lg">
                  {t("onboarding.visibility.options.public.label")}
                </span>
                <span
                  className="text-xs leading-5 text-muted-foreground"
                  data-slot="choice-copy"
                >
                  {t("onboarding.visibility.options.public.copy")}
                </span>
              </span>
            </AppToggleGroupItem>
          </AppToggleGroup>
          <FieldDescription>{onboarding.visibilityCopy}</FieldDescription>
        </Field>
      </FieldGroup>

      <AppButton onClick={onboarding.continueToInvites} type="button">
        {t("onboarding.visibility.continue")}
        <ArrowRight data-icon="inline-end" />
      </AppButton>
    </div>
  );
}

function InviteStep({ onboarding }: { onboarding: LandingOnboarding }) {
  const { t } = useTranslation("landing");

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onboarding.addInvite();
  }

  return (
    <div className="grid gap-5">
      <div className="flex flex-col gap-1">
        <h2 className="font-display text-2xl">
          {t("onboarding.invites.title")}
        </h2>
        <p className="text-sm leading-6 text-muted-foreground">
          {t("onboarding.invites.description")}
        </p>
      </div>

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

        <div className="grid grid-cols-2 gap-2">
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

      <div aria-live="polite" className="flex min-h-10 flex-wrap gap-2">
        {onboarding.invites.length ? (
          onboarding.invites.map((email) => (
            <span
              className="inline-flex h-8 items-center gap-1.5 border px-3 font-mono text-xs"
              key={email}
            >
              <Users aria-hidden="true" className="size-3 text-primary" />
              {email}
            </span>
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

function ReadyStep({ onboarding }: { onboarding: LandingOnboarding }) {
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

      <div className="grid gap-px border bg-border">
        <SummaryRow
          label={t("onboarding.ready.access")}
          value={t(
            `onboarding.visibility.options.${onboarding.visibility}.label`,
          )}
        />
        <SummaryRow
          label={t("onboarding.ready.members")}
          value={
            onboarding.invites.length
              ? onboarding.invites.join(", ")
              : t("onboarding.ready.enrolLater")
          }
        />
      </div>

      <AppButton type="button">
        {t("onboarding.ready.enter")}
        <ArrowRight data-icon="inline-end" />
      </AppButton>
    </div>
  );
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="grid grid-cols-[6rem_1fr] gap-3 bg-card px-4 py-3 text-sm">
      <span className="font-mono text-[0.7rem] leading-none tracking-[0.16em] text-muted-foreground uppercase">
        {label}
      </span>
      <span className="truncate font-medium">{value}</span>
    </div>
  );
}
