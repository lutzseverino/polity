import { Plural, Trans, useLingui } from "@lingui/react/macro";
import {
  BookOpenCheck,
  CheckCircle2,
  FilePenLine,
  Gavel,
  Landmark,
  LogOut,
  type LucideIcon,
  Plus,
  Scale,
  ShieldAlert,
  UserPlus,
  Users,
  Vote,
} from "lucide-react";
import { type ReactNode, useId } from "react";

import { AppBadge } from "@/components/app/AppBadge";
import { AppText } from "@/components/app/AppText";
import type {
  ActionAvailability,
  PolityActionAvailability,
} from "@/domains/polity";
import type {
  ActionDefinition,
  ActionId,
} from "@/features/launch-action/lib/action-definitions";
import type { ActionLauncherActionLinkProps } from "@/features/launch-action/lib/launch-action";
import { cn } from "@/lib/utils";

const actionIconById: Readonly<Record<ActionId, LucideIcon>> = {
  "amend-constitution": BookOpenCheck,
  "appeal-sanction": Scale,
  "certify-motion": CheckCircle2,
  "disband-polity": Landmark,
  "invite-member": UserPlus,
  "propose-resolution": FilePenLine,
  "propose-sanction": ShieldAlert,
  "request-review": Gavel,
  "resign-membership": LogOut,
  "review-office-term": Users,
  "start-election": Vote,
};

function ActionOption({
  action,
  availability,
  onSelect,
  polityId,
  renderActionLink,
}: Readonly<{
  action: ActionDefinition;
  availability: ActionAvailability;
  onSelect?: () => void;
  polityId: string;
  renderActionLink: (props: ActionLauncherActionLinkProps) => ReactNode;
}>) {
  const { i18n } = useLingui();
  const Icon = actionIconById[action.id];
  const content = (
    <>
      <span
        className={cn(
          "flex size-10 shrink-0 items-center justify-center rounded-xl border bg-background shadow-xs",
          !availability.available && "bg-muted/40 text-muted-foreground",
        )}
      >
        <Icon aria-hidden="true" className="size-4.5" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
          <AppText as="span" variant="strong">
            {i18n._(action.goal)}
          </AppText>
          {!availability.available ? (
            <AppBadge variant="outline">
              <Trans>Unavailable</Trans>
            </AppBadge>
          ) : null}
        </span>
        <AppText as="span" className="mt-0.5 block" variant="caption">
          <span className="font-medium text-foreground/80">
            {i18n._(action.label)}
          </span>
          {" · "}
          {availability.available
            ? i18n._(action.description)
            : (availability.reasonMessage ?? i18n._(action.description))}
        </AppText>
      </span>
    </>
  );

  if (!availability.available) {
    return (
      <div
        aria-disabled="true"
        className="flex min-w-0 items-start gap-3 rounded-xl border border-dashed bg-muted/20 p-3.5"
      >
        {content}
      </div>
    );
  }

  return renderActionLink({
    actionId: action.id,
    children: content,
    className:
      "flex items-start gap-3 border bg-background p-3.5 transition-colors hover:border-foreground/25 hover:bg-muted/40",
    onSelect,
    polityId,
  });
}

export function ActionResults({
  actions,
  availability,
  onSelect,
  polityId,
  presentation,
  query,
  renderActionLink,
}: Readonly<{
  actions: readonly ActionDefinition[];
  availability: PolityActionAvailability;
  onSelect?: () => void;
  polityId: string;
  presentation: "dialog" | "surface";
  query: string;
  renderActionLink: (props: ActionLauncherActionLinkProps) => ReactNode;
}>) {
  const availableHeadingId = useId();
  const unavailableHeadingId = useId();
  const availableActions = actions.filter(
    (action) =>
      availability[action.availabilityKey].available &&
      (query || !action.deemphasized),
  );
  const secondaryActions = query
    ? []
    : actions.filter(
        (action) =>
          availability[action.availabilityKey].available && action.deemphasized,
      );
  const unavailableActions = actions.filter(
    (action) => !availability[action.availabilityKey].available,
  );
  const availableMatchCount = availableActions.length + secondaryActions.length;
  const resultStatus = query ? (
    <span className="sr-only" role="status">
      <Plural
        value={actions.length}
        one="# matching action."
        other="# matching actions."
      />{" "}
      <Plural
        value={availableMatchCount}
        one="# is allowed."
        other="# are allowed."
      />
    </span>
  ) : null;
  const gridClassName = cn(
    "grid gap-2",
    presentation === "surface" && "sm:grid-cols-2",
  );

  if (actions.length === 0) {
    return (
      <>
        {resultStatus}
        <div className="rounded-xl border border-dashed px-4 py-8 text-center">
          <AppText variant="strong">
            <Trans>No matching actions</Trans>
          </AppText>
          <AppText className="mt-1" variant="caption">
            <Trans>
              Try describing the outcome, such as “invite someone,” “change our
              rules,” or “remove an officer.”
            </Trans>
          </AppText>
        </div>
      </>
    );
  }

  return (
    <div className="space-y-4">
      {resultStatus}
      {availableActions.length > 0 ? (
        <section aria-labelledby={availableHeadingId}>
          <div className="mb-2 flex items-center justify-between gap-3">
            <AppText as="h3" id={availableHeadingId} variant="captionStrong">
              {query ? (
                <Trans>Best matches</Trans>
              ) : (
                <Trans>Allowed actions</Trans>
              )}
            </AppText>
            <AppText as="span" variant="caption">
              {availableActions.length}
            </AppText>
          </div>
          <div className={gridClassName}>
            {availableActions.map((action) => (
              <ActionOption
                action={action}
                availability={availability[action.availabilityKey]}
                key={action.id}
                onSelect={onSelect}
                polityId={polityId}
                renderActionLink={renderActionLink}
              />
            ))}
          </div>
        </section>
      ) : null}

      {secondaryActions.length > 0 ? (
        <details className="group rounded-xl border bg-background">
          <summary className="focus-indicator flex cursor-pointer list-none items-center justify-between gap-3 rounded-xl px-3.5 py-3 text-sm font-medium marker:hidden">
            <span>
              <Trans>Membership and closure</Trans>
            </span>
            <Plus
              aria-hidden="true"
              className="size-4 text-muted-foreground transition-transform group-open:rotate-45"
            />
          </summary>
          <div className={cn(gridClassName, "border-t p-2")}>
            {secondaryActions.map((action) => (
              <ActionOption
                action={action}
                availability={availability[action.availabilityKey]}
                key={action.id}
                onSelect={onSelect}
                polityId={polityId}
                renderActionLink={renderActionLink}
              />
            ))}
          </div>
        </details>
      ) : null}

      {unavailableActions.length > 0 ? (
        query ? (
          <section aria-labelledby={unavailableHeadingId}>
            <AppText
              as="h3"
              className="mb-2"
              id={unavailableHeadingId}
              variant="captionStrong"
            >
              <Trans>Not available here</Trans>
            </AppText>
            <div className={gridClassName}>
              {unavailableActions.map((action) => (
                <ActionOption
                  action={action}
                  availability={availability[action.availabilityKey]}
                  key={action.id}
                  polityId={polityId}
                  renderActionLink={renderActionLink}
                />
              ))}
            </div>
          </section>
        ) : (
          <details className="group rounded-xl border bg-background">
            <summary className="focus-indicator flex cursor-pointer list-none items-center justify-between gap-3 rounded-xl px-3.5 py-3 text-sm font-medium marker:hidden">
              <span>
                <Plural
                  value={unavailableActions.length}
                  one="# more action exists, but is unavailable here"
                  other="# more actions exist, but are unavailable here"
                />
              </span>
              <Plus
                aria-hidden="true"
                className="size-4 text-muted-foreground transition-transform group-open:rotate-45"
              />
            </summary>
            <div className={cn(gridClassName, "border-t p-2")}>
              {unavailableActions.map((action) => (
                <ActionOption
                  action={action}
                  availability={availability[action.availabilityKey]}
                  key={action.id}
                  polityId={polityId}
                  renderActionLink={renderActionLink}
                />
              ))}
            </div>
          </details>
        )
      ) : null}
    </div>
  );
}
