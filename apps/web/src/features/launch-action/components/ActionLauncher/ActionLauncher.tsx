import { Trans, useLingui } from "@lingui/react/macro";
import { Link } from "@tanstack/react-router";
import {
  ArrowUpRight,
  BookOpenCheck,
  CirclePlus,
  FilePenLine,
  type LucideIcon,
  Plus,
  Scale,
  Search,
  UserPlus,
  Vote,
} from "lucide-react";
import { useId, useState } from "react";

import { AppButton } from "@/components/app/AppButton";
import {
  AppCard,
  AppCardContent,
  AppCardDescription,
  AppCardHeader,
  AppCardTitle,
} from "@/components/app/AppCard";
import { AppInput } from "@/components/app/AppInput";
import {
  AppNativeSelect,
  AppNativeSelectOption,
} from "@/components/app/AppNativeSelect";
import {
  AppPopover,
  AppPopoverContent,
  AppPopoverTrigger,
} from "@/components/app/AppPopover";
import { AppText } from "@/components/app/AppText";
import {
  type ActionDefinition,
  filterActionDefinitions,
} from "@/features/launch-action/lib/action-definitions";
import type {
  ActionLauncherVariant,
  PolityOption,
} from "@/features/launch-action/lib/launch-action";
import { cn } from "@/lib/utils";

type ActionLauncherProps = Readonly<{
  defaultPolityId?: string;
  polities: readonly PolityOption[];
  variant?: ActionLauncherVariant;
}>;

const actionIconById: Readonly<Record<string, LucideIcon>> = {
  "amend-constitution": BookOpenCheck,
  "invite-member": UserPlus,
  "propose-resolution": FilePenLine,
  "request-review": Scale,
  "start-election": Vote,
};

const actionCardClassName =
  "grid min-w-0 grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-x-3 rounded-xl border p-3.5 text-left transition-colors sm:min-h-36 sm:grid-cols-[1fr_auto] sm:grid-rows-[auto_1fr] sm:items-start";

function ActionOption({
  action,
  polityId,
}: Readonly<{ action: ActionDefinition; polityId: string }>) {
  const { i18n } = useLingui();
  const Icon = actionIconById[action.id] ?? CirclePlus;
  const unavailable = action.unavailableIn?.includes(polityId) ?? false;
  const content = (
    <>
      <span className="col-start-1 row-start-1 flex size-9 items-center justify-center rounded-lg border bg-background text-foreground shadow-xs">
        <Icon aria-hidden="true" className="size-4" />
      </span>
      {unavailable ? (
        <AppText
          as="span"
          className="col-start-3 row-start-1 sm:col-start-2"
          variant="captionStrong"
        >
          <Trans>Unavailable</Trans>
        </AppText>
      ) : (
        <ArrowUpRight
          aria-hidden="true"
          className="col-start-3 row-start-1 size-4 text-muted-foreground transition-colors group-hover:text-foreground sm:col-start-2"
        />
      )}
      <div className="col-start-2 row-start-1 min-w-0 sm:col-span-2 sm:col-start-1 sm:row-start-2 sm:self-end sm:pt-4">
        <AppText variant="strong">{i18n._(action.label)}</AppText>
        <AppText className="mt-1 line-clamp-2" variant="caption">
          {i18n._(
            unavailable && action.unavailableReason
              ? action.unavailableReason
              : action.description,
          )}
        </AppText>
      </div>
    </>
  );

  if (unavailable) {
    return (
      <div
        aria-disabled="true"
        className={cn(actionCardClassName, "bg-muted/30 opacity-65")}
      >
        {content}
      </div>
    );
  }

  return (
    <Link
      className={cn(
        actionCardClassName,
        "group hover:border-foreground/25 hover:bg-muted/60 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
      )}
      search={{ action: action.id, polity: polityId }}
      to="/actions/new"
    >
      {content}
    </Link>
  );
}

function ActionLauncherContent({
  defaultPolityId,
  polities,
}: Omit<ActionLauncherProps, "variant">) {
  const { i18n, t } = useLingui();
  const inputId = useId();
  const politySelectId = useId();
  const [polityId, setPolityId] = useState(
    defaultPolityId ?? polities[0]?.id ?? "",
  );
  const [query, setQuery] = useState("");
  const actions = filterActionDefinitions(query, (message) => i18n._(message));

  return (
    <div className="space-y-4">
      <div className="grid gap-3 sm:grid-cols-[minmax(0,1.5fr)_minmax(12rem,0.75fr)]">
        <div className="space-y-1.5">
          <label
            className="text-xs font-medium text-muted-foreground"
            htmlFor={inputId}
          >
            <Trans>Find an Action</Trans>
          </label>
          <div className="relative">
            <Search
              aria-hidden="true"
              className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
            />
            <AppInput
              autoComplete="off"
              className="pl-9"
              id={inputId}
              name="action-query"
              onChange={(event) => setQuery(event.currentTarget.value)}
              placeholder={t`Search by goal or action…`}
              value={query}
            />
          </div>
        </div>
        <div className="space-y-1.5">
          <label
            className="text-xs font-medium text-muted-foreground"
            htmlFor={politySelectId}
          >
            <Trans>Polity</Trans>
          </label>
          <AppNativeSelect
            autoComplete="off"
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

      <div aria-live="polite">
        {actions.length > 0 ? (
          <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
            {actions.map((action) => (
              <ActionOption
                action={action}
                key={action.id}
                polityId={polityId}
              />
            ))}
          </div>
        ) : (
          <div className="rounded-lg border border-dashed px-4 py-6 text-center">
            <AppText variant="strong">
              <Trans>No Matching Actions</Trans>
            </AppText>
            <AppText className="mt-1" variant="caption">
              <Trans>
                Try a broader phrase such as “vote,” “invite,” or “rules.”
              </Trans>
            </AppText>
          </div>
        )}
      </div>
    </div>
  );
}

export function ActionLauncher({
  defaultPolityId,
  polities,
  variant = "compact",
}: ActionLauncherProps) {
  if (variant === "surface") {
    return (
      <AppCard>
        <AppCardHeader>
          <AppCardTitle>
            <Trans>Start an Action</Trans>
          </AppCardTitle>
          <AppCardDescription>
            <Trans>
              Describe your intent, then review the official action before
              anything is submitted.
            </Trans>
          </AppCardDescription>
        </AppCardHeader>
        <AppCardContent>
          <ActionLauncherContent
            defaultPolityId={defaultPolityId}
            polities={polities}
          />
        </AppCardContent>
      </AppCard>
    );
  }

  return (
    <AppPopover>
      <AppPopoverTrigger render={<AppButton size="lg" />}>
        <Plus aria-hidden="true" data-icon="inline-start" />
        <Trans>New Action</Trans>
      </AppPopoverTrigger>
      <AppPopoverContent
        align="end"
        className="w-[min(52rem,calc(100vw-2rem))] overscroll-contain p-4"
        sideOffset={8}
      >
        <div className="mb-4">
          <AppText variant="subsectionTitle">
            <Trans>Choose an Action</Trans>
          </AppText>
          <AppText className="mt-1" variant="caption">
            <Trans>
              Start with your goal. You’ll review the formal action before
              anything is submitted.
            </Trans>
          </AppText>
        </div>
        <ActionLauncherContent
          defaultPolityId={defaultPolityId}
          polities={polities}
        />
      </AppPopoverContent>
    </AppPopover>
  );
}
