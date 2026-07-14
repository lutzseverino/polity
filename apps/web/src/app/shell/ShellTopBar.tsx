import { useLingui } from "@lingui/react/macro";
import { Link } from "@tanstack/react-router";
import { ArrowLeft, ChevronRight } from "lucide-react";
import type { ReactNode } from "react";

import {
  type ShellLayout,
  shellContentInsetClassName,
} from "@/app/shell/shell-layout";
import type {
  ResolvedShellBreadcrumb,
  ResolvedShellContext,
  ShellLinkTarget,
} from "@/app/shell/shell-route-context";
import { AppText } from "@/components/app/AppText";
import { type InboxItem, InboxPreview } from "@/domains/inbox";
import { InvitationTaskLink } from "@/features/accept-invitation";
import { ActionLauncher, type PolityOption } from "@/features/launch-action";
import { cn } from "@/lib/utils";

type ShellTopBarProps = Readonly<{
  context: ResolvedShellContext;
  inboxItems: readonly InboxItem[];
  layout: ShellLayout;
  polities: readonly PolityOption[];
}>;

type ShellInternalLinkProps = Readonly<{
  ariaLabel?: string;
  children: ReactNode;
  className: string;
  params: Readonly<Record<string, string>>;
  target: ShellLinkTarget;
}>;

function ShellInternalLink({
  ariaLabel,
  children,
  className,
  params,
  target,
}: ShellInternalLinkProps) {
  if (!("params" in target)) {
    return (
      <Link aria-label={ariaLabel} className={className} to={target.to}>
        {children}
      </Link>
    );
  }

  const polityId = params.polityId;

  if (!polityId) {
    return <span className={className}>{children}</span>;
  }

  switch (target.to) {
    case "/polities/$polityId":
      return (
        <Link
          aria-label={ariaLabel}
          className={className}
          params={{ polityId }}
          to="/polities/$polityId"
        >
          {children}
        </Link>
      );
    case "/polities/$polityId/government":
      return (
        <Link
          aria-label={ariaLabel}
          className={className}
          params={{ polityId }}
          to="/polities/$polityId/government"
        >
          {children}
        </Link>
      );
    case "/polities/$polityId/motions":
      return (
        <Link
          aria-label={ariaLabel}
          className={className}
          params={{ polityId }}
          to="/polities/$polityId/motions"
        >
          {children}
        </Link>
      );
    case "/polities/$polityId/record":
      return (
        <Link
          aria-label={ariaLabel}
          className={className}
          params={{ polityId }}
          to="/polities/$polityId/record"
        >
          {children}
        </Link>
      );
  }
}

function ShellBreadcrumbs({
  breadcrumbs,
}: Readonly<{ breadcrumbs: readonly ResolvedShellBreadcrumb[] }>) {
  const { t } = useLingui();

  return (
    <nav aria-label={t`Current location`} className="min-w-0">
      <ol className="flex min-w-0 items-center gap-1.5 text-sm">
        {breadcrumbs.map((breadcrumb, index) => {
          const isCurrent = index === breadcrumbs.length - 1;
          const content = isCurrent ? (
            <AppText
              aria-current="page"
              as="span"
              className="block max-w-72 truncate"
              variant="strong"
            >
              {breadcrumb.label}
            </AppText>
          ) : breadcrumb.target ? (
            <ShellInternalLink
              className="block max-w-56 truncate rounded text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              params={breadcrumb.params}
              target={breadcrumb.target}
            >
              {breadcrumb.label}
            </ShellInternalLink>
          ) : (
            <AppText
              as="span"
              className="block max-w-56 truncate"
              variant="supporting"
            >
              {breadcrumb.label}
            </AppText>
          );

          return (
            <li
              className={cn(
                "flex min-w-0 items-center gap-1.5",
                isCurrent && "flex-1",
              )}
              key={breadcrumb.label}
            >
              {index > 0 ? (
                <ChevronRight
                  aria-hidden="true"
                  className="size-3.5 shrink-0 text-muted-foreground"
                />
              ) : null}
              {content}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}

export function ShellTopBar({
  context,
  inboxItems,
  layout,
  polities,
}: ShellTopBarProps) {
  const isCompact = layout === "compact";
  const showDesktopBreadcrumbs = !isCompact && context.level !== "root";

  return (
    <header
      className={cn(
        "sticky top-0 z-20 flex items-center border-b bg-background/95 pt-[env(safe-area-inset-top)] pr-[max(1rem,env(safe-area-inset-right))] pl-[max(1rem,env(safe-area-inset-left))] backdrop-blur",
        isCompact
          ? "h-[calc(3.5rem+env(safe-area-inset-top))]"
          : "h-[calc(4rem+env(safe-area-inset-top))]",
        shellContentInsetClassName[layout],
      )}
    >
      <div
        className={cn(
          "flex min-w-0 flex-1 items-center gap-4",
          isCompact || showDesktopBreadcrumbs
            ? "justify-between"
            : "justify-end",
        )}
      >
        {isCompact ? (
          <div className="flex min-w-0 items-center gap-1.5">
            {context.back ? (
              <ShellInternalLink
                ariaLabel={context.back.label}
                className="inline-flex size-10 shrink-0 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                params={context.back.params}
                target={context.back.target}
              >
                <ArrowLeft aria-hidden="true" className="size-5" />
              </ShellInternalLink>
            ) : null}
            <AppText as="span" className="truncate" variant="subsectionTitle">
              {context.title}
            </AppText>
          </div>
        ) : showDesktopBreadcrumbs ? (
          <ShellBreadcrumbs breadcrumbs={context.breadcrumbs} />
        ) : null}

        <div className="flex shrink-0 items-center gap-1">
          {context.showPrimaryAction ? (
            <ActionLauncher
              polities={polities}
              triggerPresentation={layout === "expanded" ? "labelled" : "icon"}
            />
          ) : null}
          {!isCompact ? (
            <>
              {context.showPrimaryAction ? (
                <span
                  aria-hidden="true"
                  className="mx-1.5 h-6 w-px bg-border"
                />
              ) : null}
              <InboxPreview
                items={inboxItems}
                renderInvitationLink={({
                  children,
                  className,
                  invitationId,
                }) => (
                  <InvitationTaskLink
                    className={className}
                    invitationId={invitationId}
                  >
                    {children}
                  </InvitationTaskLink>
                )}
              />
            </>
          ) : null}
        </div>
      </div>
    </header>
  );
}
