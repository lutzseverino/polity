import { useLingui } from "@lingui/react/macro";
import { Link } from "@tanstack/react-router";
import {
  Compass,
  Home,
  Inbox,
  Landmark,
  type LucideIcon,
  UserRound,
} from "lucide-react";

import type { ShellLayout } from "@/app/shell/shell-layout";
import {
  type ShellSection,
  type ShellSectionTarget,
  shellSectionDefinitions,
} from "@/app/shell/shell-route-context";
import { AppAvatar, AppAvatarFallback } from "@/components/app/AppAvatar";
import { AppText } from "@/components/app/AppText";
import { cn } from "@/lib/utils";

type NavigationDestination = Readonly<{
  group: "account" | "primary";
  icon: LucideIcon;
  matchDescendants?: boolean;
  section: ShellSection;
}>;

type NavigationItem = NavigationDestination &
  ShellSectionTarget &
  Readonly<{
    ariaLabel?: string;
    badge?: number;
    label: string;
  }>;

type ShellNavigationProps = Readonly<{
  layout: ShellLayout;
  openInboxTaskCount: number;
}>;

const applicationName = "decreos";

const navigationDestinations: readonly NavigationDestination[] = [
  { group: "primary", icon: Home, section: "home" },
  {
    group: "primary",
    icon: Compass,
    section: "explore",
  },
  {
    group: "primary",
    icon: Landmark,
    matchDescendants: true,
    section: "polities",
  },
  { group: "primary", icon: Inbox, section: "inbox" },
  { group: "account", icon: UserRound, section: "me" },
];

const navigationLinkClassName: Readonly<Record<ShellLayout, string>> = {
  compact: "flex-col gap-1 px-2 py-2",
  expanded: "gap-3 px-3 py-2",
  medium: "min-h-14 flex-col gap-1 px-1.5 py-2",
};

function NavigationLink({
  ariaLabel,
  badge,
  icon: Icon,
  label,
  layout,
  matchDescendants = false,
  to,
}: NavigationItem & Readonly<{ layout: ShellLayout }>) {
  return (
    <Link
      activeOptions={{ exact: !matchDescendants }}
      activeProps={{ className: "bg-muted text-foreground" }}
      aria-label={ariaLabel}
      className={cn(
        "focus-indicator relative flex min-w-0 items-center rounded-lg text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground",
        navigationLinkClassName[layout],
      )}
      to={to}
    >
      <span className="relative shrink-0">
        <Icon aria-hidden="true" className="size-5" />
        {badge ? (
          <span
            aria-hidden="true"
            className="absolute -top-2 -right-2 flex min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[0.625rem] leading-4 font-semibold text-primary-foreground"
          >
            {badge}
          </span>
        ) : null}
      </span>
      <span
        className={cn(
          "truncate",
          layout === "expanded" ? "text-sm" : "text-[0.6875rem]",
        )}
      >
        {label}
      </span>
    </Link>
  );
}

function AccountLink({
  ariaLabel,
  displayLabel,
  layout,
  to,
}: Readonly<{
  ariaLabel: string;
  displayLabel: string;
  layout: Exclude<ShellLayout, "compact">;
  to: ShellSectionTarget["to"];
}>) {
  return (
    <Link
      activeOptions={{ exact: true }}
      activeProps={{ className: "bg-muted text-foreground" }}
      aria-label={ariaLabel}
      className={cn(
        "focus-indicator flex h-11 min-w-0 items-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground",
        layout === "expanded" ? "gap-3 px-2" : "justify-center",
      )}
      to={to}
    >
      <AppAvatar>
        <AppAvatarFallback>
          <UserRound aria-hidden="true" className="size-4" />
        </AppAvatarFallback>
      </AppAvatar>
      {layout === "expanded" ? (
        <AppText as="span" className="truncate" variant="strong">
          {displayLabel}
        </AppText>
      ) : null}
    </Link>
  );
}

function AppBrand({ compact = false }: Readonly<{ compact?: boolean }>) {
  return (
    <AppText
      as="div"
      className={cn(
        "flex h-16 items-center font-semibold",
        compact ? "justify-center text-lg" : "px-5 text-lg",
      )}
      variant="sectionTitle"
    >
      {compact ? (
        <>
          <span className="sr-only">{applicationName}</span>
          <span aria-hidden="true">{applicationName.charAt(0)}</span>
        </>
      ) : (
        applicationName
      )}
    </AppText>
  );
}

export function ShellNavigation({
  layout,
  openInboxTaskCount,
}: ShellNavigationProps) {
  const { i18n, t } = useLingui();
  const items = navigationDestinations.map((destination): NavigationItem => {
    const definition = shellSectionDefinitions[destination.section];
    const label = i18n._(definition.label);
    const badge =
      destination.section === "inbox" && openInboxTaskCount > 0
        ? openInboxTaskCount
        : undefined;

    return {
      ...destination,
      ...definition.target,
      ariaLabel: badge
        ? t`${label}, ${openInboxTaskCount} items need action`
        : undefined,
      badge,
      label,
    };
  });
  const primaryItems = items.filter((item) => item.group === "primary");
  const accountItem = items.find((item) => item.group === "account");

  if (layout === "compact") {
    return (
      <nav
        aria-label={t`Primary Navigation`}
        className="fixed inset-x-0 bottom-0 z-30 grid grid-cols-5 border-t bg-background/95 pt-1 pr-[max(0.5rem,env(safe-area-inset-right))] pb-[env(safe-area-inset-bottom)] pl-[max(0.5rem,env(safe-area-inset-left))] backdrop-blur"
      >
        {items.map((item) => (
          <NavigationLink key={item.to} layout={layout} {...item} />
        ))}
      </nav>
    );
  }

  return (
    <aside
      className={cn(
        "fixed inset-y-0 left-0 z-30 flex flex-col border-r bg-background pt-[env(safe-area-inset-top)]",
        layout === "expanded" ? "w-60" : "w-20",
      )}
    >
      <AppBrand compact={layout === "medium"} />
      <nav
        aria-label={t`Primary Navigation`}
        className={cn(
          "flex flex-1 flex-col gap-1",
          layout === "expanded" ? "px-3" : "px-2",
        )}
      >
        {primaryItems.map((item) => (
          <NavigationLink key={item.to} layout={layout} {...item} />
        ))}
      </nav>
      <div
        className={cn(
          "pb-[max(0.75rem,env(safe-area-inset-bottom))]",
          layout === "expanded" ? "px-3" : "px-2",
        )}
      >
        {accountItem ? (
          <AccountLink
            ariaLabel={t`Open account`}
            displayLabel={t`Account`}
            layout={layout}
            to={accountItem.to}
          />
        ) : null}
      </div>
    </aside>
  );
}
