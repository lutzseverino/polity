import type { MessageDescriptor } from "@lingui/core";
import { msg } from "@lingui/core/macro";
import { useLingui } from "@lingui/react";
import { useMatches } from "@tanstack/react-router";

export type ShellSection = "explore" | "home" | "inbox" | "me" | "polities";
export type ShellRouteLevel = "detail" | "root" | "task" | "workspace";

type ShellStaticPath = "/explore" | "/home" | "/inbox" | "/me" | "/polities";

export type ShellSectionTarget = Readonly<{ to: ShellStaticPath }>;

type ShellPolityPath =
  | "/polities/$polityId"
  | "/polities/$polityId/government"
  | "/polities/$polityId/motions"
  | "/polities/$polityId/record";

export type ShellLinkTarget =
  | Readonly<{ to: ShellStaticPath }>
  | Readonly<{ params: "polityId"; to: ShellPolityPath }>;

type ShellBackTarget = Readonly<{
  label: MessageDescriptor;
  target: ShellLinkTarget;
}>;

/**
 * Declarative shell contract for `staticData.shell` on a file route.
 *
 * Shell values inherit through the matched route chain. The nearest route that
 * declares a value wins, so parent routes can establish workspace defaults and
 * task/detail routes only need to specify their overrides. A loader may return
 * `{ shellLabel: string }` when the route label comes from fetched data.
 */
type ShellRouteData = Readonly<{
  /** Back destination shown by the compact top bar. */
  back?: ShellBackTarget;
  /** Short compact title used in place of the route's label. */
  compactLabel?: MessageDescriptor;
  /** Whether the compact bottom navigation remains present on this route. */
  compactNavigation?: "hidden" | "visible";
  /** Whether workspace-owned headers and tabs remain present in compact mode. */
  compactWorkspaceChrome?: "hidden" | "visible";
  /** Static route label. Prefer loader `shellLabel` for fetched entities. */
  label?: MessageDescriptor;
  /** Semantic depth used by the shell and available for future presentation rules. */
  level?: ShellRouteLevel;
  /** Primary application section used for navigation state and the breadcrumb root. */
  section?: ShellSection;
  /** Whether the global action launcher is available from the top bar. */
  showPrimaryAction?: boolean;
  /** Destination used when this route is an intermediate breadcrumb. */
  target?: ShellLinkTarget;
}>;

declare module "@tanstack/react-router" {
  interface StaticDataRouteOption {
    shell?: ShellRouteData;
  }
}

type ShellContextMatch = Readonly<{
  loaderData?: unknown;
  params?: unknown;
  shell?: ShellRouteData;
}>;

export type ResolvedShellBreadcrumb = Readonly<{
  label: string;
  params: Readonly<Record<string, string>>;
  target?: ShellLinkTarget;
}>;

type ResolvedShellBackTarget = Readonly<{
  label: string;
  params: Readonly<Record<string, string>>;
  target: ShellLinkTarget;
}>;

export type ResolvedShellContext = Readonly<{
  back?: ResolvedShellBackTarget;
  breadcrumbs: readonly ResolvedShellBreadcrumb[];
  compactNavigation: "hidden" | "visible";
  level: ShellRouteLevel;
  polityId?: string;
  section: ShellSection;
  showPrimaryAction: boolean;
  title: string;
}>;

type ShellSectionDefinition = Readonly<{
  label: MessageDescriptor;
  target: ShellSectionTarget;
}>;

export const shellSectionDefinitions: Readonly<
  Record<ShellSection, ShellSectionDefinition>
> = {
  explore: { label: msg`Explore`, target: { to: "/explore" } },
  home: { label: msg`Home`, target: { to: "/home" } },
  inbox: { label: msg`Inbox`, target: { to: "/inbox" } },
  me: { label: msg`Me`, target: { to: "/me" } },
  polities: { label: msg`Polities`, target: { to: "/polities" } },
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readShellLabel(loaderData: unknown) {
  if (!isRecord(loaderData)) {
    return undefined;
  }

  return typeof loaderData.shellLabel === "string"
    ? loaderData.shellLabel
    : undefined;
}

function readStringParams(value: unknown) {
  if (!isRecord(value)) {
    return {};
  }

  return Object.fromEntries(
    Object.entries(value).filter(
      (entry): entry is [string, string] => typeof entry[1] === "string",
    ),
  );
}

function appendBreadcrumb(
  breadcrumbs: ResolvedShellBreadcrumb[],
  breadcrumb: ResolvedShellBreadcrumb,
) {
  if (breadcrumbs.at(-1)?.label !== breadcrumb.label) {
    breadcrumbs.push(breadcrumb);
  }
}

function findLastMatching<T>(
  items: readonly T[],
  predicate: (item: T) => boolean,
) {
  for (let index = items.length - 1; index >= 0; index -= 1) {
    const item = items[index];

    if (item && predicate(item)) {
      return item;
    }
  }

  return undefined;
}

export function resolveShellContext(
  matches: readonly ShellContextMatch[],
  translate: (message: MessageDescriptor) => string,
): ResolvedShellContext {
  const routeContexts = matches.flatMap((match) =>
    match.shell
      ? [
          {
            label: readShellLabel(match.loaderData),
            params: readStringParams(match.params),
            shell: match.shell,
          },
        ]
      : [],
  );
  const section =
    findLastMatching(routeContexts, (context) => Boolean(context.shell.section))
      ?.shell.section ?? "polities";
  const sectionDefinition = shellSectionDefinitions[section];
  const breadcrumbs: ResolvedShellBreadcrumb[] = [];

  appendBreadcrumb(breadcrumbs, {
    label: translate(sectionDefinition.label),
    params: {},
    target: sectionDefinition.target,
  });

  for (const context of routeContexts) {
    const label =
      context.label ??
      (context.shell.label ? translate(context.shell.label) : undefined);

    if (label) {
      appendBreadcrumb(breadcrumbs, {
        label,
        params: context.params,
        target: context.shell.target,
      });
    }
  }

  const titleContext = findLastMatching(
    routeContexts,
    (context) =>
      Boolean(context.shell.compactLabel) ||
      Boolean(context.label) ||
      Boolean(context.shell.label),
  );
  const title = titleContext?.shell.compactLabel
    ? translate(titleContext.shell.compactLabel)
    : (titleContext?.label ??
      (titleContext?.shell.label
        ? translate(titleContext.shell.label)
        : undefined) ??
      translate(sectionDefinition.label));
  const backContext = findLastMatching(routeContexts, (context) =>
    Boolean(context.shell.back),
  );
  const visibilityContext = findLastMatching(
    routeContexts,
    (context) => context.shell.compactNavigation !== undefined,
  );
  const actionContext = findLastMatching(
    routeContexts,
    (context) => context.shell.showPrimaryAction !== undefined,
  );
  const levelContext = findLastMatching(routeContexts, (context) =>
    Boolean(context.shell.level),
  );
  const polityContext = findLastMatching(routeContexts, (context) =>
    Boolean(context.params.polityId),
  );
  const back = backContext?.shell.back;

  return {
    back: back
      ? {
          label: translate(back.label),
          params: backContext.params,
          target: back.target,
        }
      : undefined,
    breadcrumbs,
    compactNavigation: visibilityContext?.shell.compactNavigation ?? "visible",
    level: levelContext?.shell.level ?? "root",
    polityId: polityContext?.params.polityId,
    section,
    showPrimaryAction: actionContext?.shell.showPrimaryAction ?? true,
    title,
  };
}

export function useShellRouteContext() {
  const { i18n } = useLingui();
  const matches = useMatches({
    select: (matches) =>
      matches.map((match) => ({
        loaderData: match.loaderData,
        params: match.params,
        shell: match.staticData.shell,
      })),
  });

  return resolveShellContext(matches, (message) => i18n._(message));
}
