import { Trans, useLingui } from "@lingui/react/macro";
import { Outlet, useRouter, useSearch } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { ShellNavigation } from "@/app/shell/ShellNavigation";
import { ShellTopBar } from "@/app/shell/ShellTopBar";
import {
  shellContentInsetClassName,
  shouldHideShellPageHeader,
  useShellLayout,
} from "@/app/shell/shell-layout";
import { useShellRouteContext } from "@/app/shell/shell-route-context";
import { AppLinkButton } from "@/components/app/AppButton";
import { AppDialog, AppDialogContent } from "@/components/app/AppDialog";
import { countOpenInboxTasks, useInboxItems } from "@/domains/inbox";
import { usePolityOptions } from "@/domains/polity";
import {
  InvitationResponse,
  invitationResponseDescriptionId,
  invitationResponseTitleId,
} from "@/features/accept-invitation";
import { cn } from "@/lib/utils";

export function AppShell() {
  const { i18n } = useLingui();
  const router = useRouter();
  const { task } = useSearch({ from: "__root__" });
  const [displayedTask, setDisplayedTask] = useState(task);
  const layout = useShellLayout();
  const shellContext = useShellRouteContext();
  const { data: inboxItems } = useInboxItems({ locale: i18n.locale });
  const { data: polityOptions } = usePolityOptions({ locale: i18n.locale });
  const openInboxTaskCount = countOpenInboxTasks(inboxItems);
  const showCompactNavigation =
    layout !== "compact" || shellContext.compactNavigation === "visible";
  const hidePageHeader = shouldHideShellPageHeader(layout, shellContext.level);

  useEffect(() => {
    if (task) {
      setDisplayedTask(task);
    }
  }, [task]);

  return (
    <div className="min-h-svh bg-muted/30">
      <a
        className="focus-indicator fixed top-[max(0.5rem,env(safe-area-inset-top))] left-[max(0.5rem,env(safe-area-inset-left))] z-50 -translate-y-24 rounded-lg bg-background px-3 py-2 text-sm font-medium shadow-md focus-visible:translate-y-0"
        href="#main-content"
      >
        <Trans>Skip to main content</Trans>
      </a>

      {showCompactNavigation ? (
        <ShellNavigation
          layout={layout}
          openInboxTaskCount={openInboxTaskCount}
        />
      ) : null}
      <ShellTopBar
        context={shellContext}
        inboxItems={inboxItems}
        layout={layout}
        polities={polityOptions}
      />

      <main
        className={cn(
          "pb-8 focus:outline-none",
          shellContentInsetClassName[layout],
          hidePageHeader && "[&_[data-slot=page-header]]:sr-only",
          layout === "compact" &&
            showCompactNavigation &&
            "pb-[calc(5rem+env(safe-area-inset-bottom))]",
        )}
        data-shell-layout={layout}
        data-shell-level={shellContext.level}
        id="main-content"
        tabIndex={-1}
      >
        <div className="mx-auto w-full max-w-7xl px-4 py-5 sm:px-6 md:px-8 md:py-8">
          <Outlet />
        </div>
      </main>

      {displayedTask ? (
        <AppDialog
          onOpenChange={(open) => {
            if (!open) {
              router.history.back();
            }
          }}
          onOpenChangeComplete={(open) => {
            if (!open && task === undefined) {
              setDisplayedTask(undefined);
            }
          }}
          open={task !== undefined}
        >
          <AppDialogContent
            aria-describedby={invitationResponseDescriptionId}
            aria-labelledby={invitationResponseTitleId}
            className="max-h-[calc(100dvh-1rem)] max-w-none overflow-hidden p-0 sm:max-h-[calc(100dvh-2rem)] sm:max-w-xl"
          >
            <InvitationResponse
              descriptionId={invitationResponseDescriptionId}
              headingLevel="h2"
              invitationId={displayedTask.invitationId}
              locale={i18n.locale}
              onDismiss={() => router.history.back()}
              renderPolitiesLink={(label) => (
                <AppLinkButton
                  className="min-h-11 w-full sm:min-h-9 sm:w-auto"
                  search={{ task: undefined }}
                  size="lg"
                  to="/polities"
                >
                  {label}
                </AppLinkButton>
              )}
              showDismissAfterAccept
              titleId={invitationResponseTitleId}
            />
          </AppDialogContent>
        </AppDialog>
      ) : null}
    </div>
  );
}
