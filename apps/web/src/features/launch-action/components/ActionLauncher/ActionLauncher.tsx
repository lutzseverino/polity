import { Plural, Trans, useLingui } from "@lingui/react/macro";
import { Plus, Search } from "lucide-react";
import { useEffect, useId, useState } from "react";

import { AppButton, AppLinkButton } from "@/components/app/AppButton";
import {
  AppCard,
  AppCardContent,
  AppCardDescription,
  AppCardHeader,
  AppCardTitle,
} from "@/components/app/AppCard";
import {
  AppDialog,
  AppDialogContent,
  AppDialogDescription,
  AppDialogTitle,
  AppDialogTrigger,
} from "@/components/app/AppDialog";
import { AppInput } from "@/components/app/AppInput";
import {
  AppNativeSelect,
  AppNativeSelectOption,
} from "@/components/app/AppNativeSelect";
import { AppText } from "@/components/app/AppText";
import type { PolityActionAvailability } from "@/domains/polity";
import { usePolityActions } from "@/domains/polity";
import { ActionResults } from "@/features/launch-action/components/ActionLauncher/ActionResults";
import { filterActionDefinitions } from "@/features/launch-action/lib/action-definitions";
import type {
  ActionLauncherVariant,
  PolityOption,
} from "@/features/launch-action/lib/launch-action";
import { cn } from "@/lib/utils";

type ActionLauncherProps = Readonly<{
  defaultPolityId?: string;
  polities: readonly PolityOption[];
  triggerPresentation?: "icon" | "labelled" | "prompt";
  variant?: ActionLauncherVariant;
}>;

function getInitialPolityId(
  defaultPolityId: string | undefined,
  polities: readonly PolityOption[],
) {
  return polities.some((polity) => polity.id === defaultPolityId)
    ? (defaultPolityId ?? "")
    : (polities[0]?.id ?? "");
}

function ReadinessSummary({
  actions,
  availableCount,
  polityName,
}: Readonly<{
  actions: PolityActionAvailability;
  availableCount: number;
  polityName: string;
}>) {
  const healthy = actions.readiness.status === "ready";

  return (
    <div className="flex items-start gap-3 rounded-xl bg-muted/55 px-3.5 py-3">
      <span
        aria-hidden="true"
        className={cn(
          "mt-1.5 size-2.5 shrink-0 rounded-full ring-4",
          healthy
            ? "bg-emerald-500 ring-emerald-500/10"
            : "bg-amber-500 ring-amber-500/10",
        )}
      />
      <div className="min-w-0 flex-1">
        <AppText variant="strong">
          <Plural value={availableCount} one="# action" other="# actions" />{" "}
          <Trans>currently allowed in {polityName}</Trans>
        </AppText>
        <AppText className="mt-0.5" variant="caption">
          {actions.readiness.statusMessage}
        </AppText>
      </div>
    </div>
  );
}

function ActionLauncherContent({
  defaultPolityId,
  onSelect,
  polities,
  presentation,
}: Readonly<{
  defaultPolityId?: string;
  onSelect?: () => void;
  polities: readonly PolityOption[];
  presentation: "dialog" | "surface";
}>) {
  const { i18n, t } = useLingui();
  const inputId = useId();
  const politySelectId = useId();
  const [polityId, setPolityId] = useState(() =>
    getInitialPolityId(defaultPolityId, polities),
  );
  const [query, setQuery] = useState("");
  const {
    data: availability,
    error,
    isPending,
    refetch,
  } = usePolityActions({
    locale: i18n.locale,
    polityId,
  });
  useEffect(() => {
    const nextPolityId = getInitialPolityId(defaultPolityId, polities);

    if (nextPolityId) {
      setPolityId(nextPolityId);
    }
  }, [defaultPolityId, polities]);

  const actions = filterActionDefinitions(query, (message) => i18n._(message));
  const allActions = filterActionDefinitions("", (message) => i18n._(message));
  const polityName =
    polities.find((polity) => polity.id === polityId)?.name ?? "";
  const availableCount = availability
    ? allActions.filter(
        (action) => availability[action.availabilityKey].available,
      ).length
    : 0;

  return (
    <div className="space-y-4">
      <div className="grid gap-3 sm:grid-cols-[minmax(0,1.45fr)_minmax(12rem,0.8fr)]">
        <div className="min-w-0 space-y-1.5">
          <label
            className="text-xs font-medium text-muted-foreground"
            htmlFor={inputId}
          >
            <Trans>What do you want to do?</Trans>
          </label>
          <div className="relative min-w-0">
            <Search
              aria-hidden="true"
              className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
            />
            <AppInput
              autoComplete="off"
              autoFocus={presentation === "dialog"}
              className="min-w-0 text-ellipsis pl-9"
              id={inputId}
              name="action-query"
              onChange={(event) => setQuery(event.currentTarget.value)}
              placeholder={t`Try “change our rules” or “invite someone”…`}
              value={query}
            />
          </div>
        </div>
        <div className="min-w-0 space-y-1.5">
          <label
            className="text-xs font-medium text-muted-foreground"
            htmlFor={politySelectId}
          >
            <Trans>In this polity</Trans>
          </label>
          <AppNativeSelect
            autoComplete="off"
            className="w-full min-w-0 [&_select]:overflow-hidden [&_select]:text-ellipsis [&_select]:whitespace-nowrap"
            id={politySelectId}
            name="polity"
            onChange={(event) => setPolityId(event.currentTarget.value)}
            value={polityId}
          >
            {polities.map((polity) => (
              <AppNativeSelectOption key={polity.id} value={polity.id}>
                {polity.name}
              </AppNativeSelectOption>
            ))}
          </AppNativeSelect>
        </div>
      </div>

      {isPending ? (
        <div
          className="flex min-h-36 items-center justify-center rounded-xl border border-dashed"
          role="status"
        >
          <AppText variant="supporting">
            <Trans>Checking what you can do…</Trans>
          </AppText>
        </div>
      ) : error || !availability ? (
        <div
          className="flex min-h-36 flex-col items-center justify-center rounded-xl border border-dashed px-5 text-center"
          role="alert"
        >
          <AppText variant="strong">
            <Trans>We couldn’t check your actions</Trans>
          </AppText>
          <AppText className="mt-1" variant="caption">
            <Trans>
              Your polity is unchanged. Try checking its permissions again.
            </Trans>
          </AppText>
          <AppButton
            className="mt-3"
            onClick={() => void refetch()}
            size="sm"
            variant="outline"
          >
            <Trans>Try again</Trans>
          </AppButton>
        </div>
      ) : (
        <>
          <ReadinessSummary
            actions={availability}
            availableCount={availableCount}
            polityName={polityName}
          />

          <ActionResults
            actions={actions}
            availability={availability}
            onSelect={onSelect}
            polityId={polityId}
            presentation={presentation}
            query={query.trim()}
          />
        </>
      )}
    </div>
  );
}

function EmptyLauncher() {
  return (
    <div className="rounded-xl border border-dashed px-4 py-8 text-center">
      <AppText variant="strong">
        <Trans>Join or found a polity first</Trans>
      </AppText>
      <AppText className="mt-1" variant="caption">
        <Trans>Actions become available inside a polity.</Trans>
      </AppText>
      <div className="mt-4 flex flex-col justify-center gap-2 sm:flex-row">
        <AppLinkButton to="/explore" variant="outline">
          <Trans>Explore polities</Trans>
        </AppLinkButton>
        <AppLinkButton to="/polities/new">
          <Trans>Found a polity</Trans>
        </AppLinkButton>
      </div>
    </div>
  );
}

function CompactActionLauncher({
  defaultPolityId,
  polities,
  triggerPresentation,
}: Omit<ActionLauncherProps, "variant">) {
  const { t } = useLingui();
  const titleId = useId();
  const descriptionId = useId();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const handleShortcut = (event: KeyboardEvent) => {
      if (event.repeat) {
        return;
      }

      if (
        event.key.toLocaleLowerCase() === "k" &&
        (event.metaKey || event.ctrlKey)
      ) {
        event.preventDefault();
        setOpen((current) => !current);
      }
    };

    document.addEventListener("keydown", handleShortcut);
    return () => document.removeEventListener("keydown", handleShortcut);
  }, []);

  return (
    <AppDialog onOpenChange={setOpen} open={open}>
      <AppDialogTrigger
        render={
          <AppButton
            aria-keyshortcuts="Control+K Meta+K"
            aria-label={
              triggerPresentation === "icon" ? t`Start an action` : undefined
            }
            className={cn(
              triggerPresentation === "prompt" &&
                "h-auto w-full justify-start gap-3 whitespace-normal rounded-xl p-4 text-left shadow-xs hover:border-foreground/25 hover:bg-muted/40 sm:p-5",
            )}
            size={triggerPresentation === "icon" ? "icon-lg" : "lg"}
            variant={triggerPresentation === "prompt" ? "outline" : "default"}
          />
        }
      >
        {triggerPresentation === "prompt" ? (
          <>
            <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary text-primary-foreground">
              <Plus aria-hidden="true" className="size-4.5" />
            </span>
            <span className="min-w-0 flex-1">
              <AppText as="span" className="block" variant="strong">
                <Trans>Start an action</Trans>
              </AppText>
              <AppText as="span" className="mt-0.5 block" variant="caption">
                <Trans>
                  Describe the outcome you want. The constitution will guide the
                  rest.
                </Trans>
              </AppText>
            </span>
            <kbd className="hidden rounded border bg-background px-1.5 py-1 text-[0.625rem] leading-none font-medium text-muted-foreground sm:block">
              ⌘K
            </kbd>
          </>
        ) : (
          <>
            <Plus
              aria-hidden="true"
              data-icon={
                triggerPresentation === "icon" ? undefined : "inline-start"
              }
            />
            {triggerPresentation === "labelled" ? (
              <>
                <Trans>Actions</Trans>
                <kbd className="ml-1 hidden rounded border border-primary-foreground/25 px-1.5 py-0.5 text-[0.625rem] leading-none font-medium opacity-75 xl:inline">
                  ⌘K
                </kbd>
              </>
            ) : null}
          </>
        )}
      </AppDialogTrigger>

      <AppDialogContent
        aria-describedby={descriptionId}
        aria-labelledby={titleId}
        className="max-h-[calc(100dvh-1rem)] max-w-none overflow-hidden p-0 sm:max-h-[calc(100dvh-2rem)] sm:max-w-xl"
      >
        <div className="flex max-h-[calc(100dvh-1rem)] min-h-0 flex-col sm:max-h-[calc(100dvh-2rem)]">
          <header className="border-b px-5 py-4 pr-16 sm:px-6 sm:py-5 sm:pr-16">
            <AppDialogTitle
              render={<AppText as="h2" id={titleId} variant="sectionTitle" />}
            >
              <Trans>Make something happen</Trans>
            </AppDialogTitle>
            <AppDialogDescription
              render={
                <AppText
                  className="mt-1"
                  id={descriptionId}
                  variant="supporting"
                />
              }
            >
              <Trans>
                Start with your goal. The constitution determines the formal
                action and whether you are authorized to begin.
              </Trans>
            </AppDialogDescription>
          </header>
          <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-4 sm:p-6">
            {polities.length > 0 ? (
              <ActionLauncherContent
                defaultPolityId={defaultPolityId}
                onSelect={() => setOpen(false)}
                polities={polities}
                presentation="dialog"
              />
            ) : (
              <EmptyLauncher />
            )}
          </div>
        </div>
      </AppDialogContent>
    </AppDialog>
  );
}

export function ActionLauncher({
  defaultPolityId,
  polities,
  triggerPresentation = "labelled",
  variant = "compact",
}: ActionLauncherProps) {
  if (variant === "surface") {
    return (
      <AppCard>
        <AppCardHeader>
          <AppCardTitle>
            <Trans>Make something happen</Trans>
          </AppCardTitle>
          <AppCardDescription>
            <Trans>
              Choose the outcome in plain language. The constitution handles the
              mechanics and shows what is possible now.
            </Trans>
          </AppCardDescription>
        </AppCardHeader>
        <AppCardContent>
          {polities.length > 0 ? (
            <ActionLauncherContent
              defaultPolityId={defaultPolityId}
              polities={polities}
              presentation="surface"
            />
          ) : (
            <EmptyLauncher />
          )}
        </AppCardContent>
      </AppCard>
    );
  }

  return (
    <CompactActionLauncher
      defaultPolityId={defaultPolityId}
      polities={polities}
      triggerPresentation={triggerPresentation}
    />
  );
}
