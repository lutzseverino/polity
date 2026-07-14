import { Trans, useLingui } from "@lingui/react/macro";
import { Link, Outlet, ScrollRestoration } from "@tanstack/react-router";
import {
  Compass,
  Home,
  Inbox,
  Landmark,
  type LucideIcon,
  UserRound,
} from "lucide-react";

import { AppAvatar, AppAvatarFallback } from "@/components/app/AppAvatar";
import { AppText } from "@/components/app/AppText";
import { InboxPreview, useInboxItems } from "@/domains/inbox";
import { usePolityOptions } from "@/domains/polity";
import { ActionLauncher } from "@/features/launch-action";
import { cn } from "@/lib/utils";

type NavigationItem = Readonly<{
  icon: LucideIcon;
  label: string;
  to: "/explore" | "/home" | "/inbox" | "/me" | "/polities";
}>;

function NavigationLink({ icon: Icon, label, to }: NavigationItem) {
  return (
    <Link
      activeOptions={{ exact: to !== "/polities" }}
      activeProps={{ className: "bg-muted text-foreground" }}
      className={cn(
        "flex min-w-0 items-center rounded-lg text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
        "flex-col gap-1 px-2 py-2 md:flex-row md:gap-3 md:px-3",
      )}
      to={to}
    >
      <Icon aria-hidden="true" className="size-5 shrink-0" />
      <span className="truncate text-[0.6875rem] md:text-sm">{label}</span>
    </Link>
  );
}

function AccountLink() {
  const { t } = useLingui();

  return (
    <Link
      aria-label={t`Open profile`}
      className="flex h-9 min-w-0 items-center gap-2 rounded-lg px-2.5 transition-colors hover:bg-muted focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
      to="/me"
    >
      <AppAvatar>
        <AppAvatarFallback>LS</AppAvatarFallback>
      </AppAvatar>
      <AppText className="truncate" variant="strong">
        Lutz Severino
      </AppText>
    </Link>
  );
}

export function AppShell() {
  const { i18n, t } = useLingui();
  const { data: inboxItems } = useInboxItems({ locale: i18n.locale });
  const { data: polityOptions } = usePolityOptions({ locale: i18n.locale });
  const desktopNavigationItems: readonly NavigationItem[] = [
    { icon: Home, label: t`Home`, to: "/home" },
    { icon: Compass, label: t`Explore`, to: "/explore" },
    { icon: Landmark, label: t`Polities`, to: "/polities" },
  ];
  const mobileNavigationItems: readonly NavigationItem[] = [
    ...desktopNavigationItems,
    { icon: Inbox, label: t`Inbox`, to: "/inbox" },
    { icon: UserRound, label: t`Me`, to: "/me" },
  ];

  return (
    <div className="min-h-svh bg-muted/30">
      <a
        className="fixed top-2 left-2 z-50 -translate-y-20 rounded-lg bg-background px-3 py-2 text-sm font-medium shadow-md focus-visible:translate-y-0 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        href="#main-content"
      >
        <Trans>Skip to main content</Trans>
      </a>

      <header className="sticky top-0 z-30 flex h-14 items-center border-b bg-background/95 px-4 backdrop-blur md:hidden">
        <AppText as="span" variant="subsectionTitle">
          decreos
        </AppText>
      </header>

      <aside className="fixed inset-y-0 left-0 z-30 hidden w-60 flex-col border-r bg-background md:flex">
        <AppText
          as="div"
          className="flex h-16 items-center px-5"
          variant="sectionTitle"
        >
          decreos
        </AppText>
        <nav
          aria-label={t`Primary Navigation`}
          className="flex flex-1 flex-col gap-1 px-3"
        >
          {desktopNavigationItems.map((item) => (
            <NavigationLink key={item.to} {...item} />
          ))}
        </nav>
      </aside>

      <header className="sticky top-0 z-20 ml-60 hidden h-16 items-center justify-end border-b bg-background/95 px-8 backdrop-blur md:flex">
        <div className="flex items-center gap-1">
          <ActionLauncher polities={polityOptions} />
          <span aria-hidden="true" className="mx-1.5 h-6 w-px bg-border" />
          <InboxPreview items={inboxItems} />
          <AccountLink />
        </div>
      </header>

      <main
        className="pb-24 focus:outline-none md:ml-60 md:pb-8"
        id="main-content"
        tabIndex={-1}
      >
        <div className="mx-auto w-full max-w-7xl px-4 py-5 sm:px-6 md:px-8 md:py-8">
          <Outlet />
        </div>
      </main>

      <nav
        aria-label={t`Primary Navigation`}
        className="fixed inset-x-0 bottom-0 z-30 grid grid-cols-5 border-t bg-background/95 px-[max(0.5rem,env(safe-area-inset-left))] pb-[env(safe-area-inset-bottom)] backdrop-blur md:hidden"
      >
        {mobileNavigationItems.map((item) => (
          <NavigationLink key={item.to} {...item} />
        ))}
      </nav>
      <ScrollRestoration />
    </div>
  );
}
