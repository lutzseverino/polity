import { msg } from "@lingui/core/macro";
import { Plural, Trans, useLingui } from "@lingui/react/macro";
import {
  createFileRoute,
  Link,
  notFound,
  Outlet,
  redirect,
  useMatches,
  useRouterState,
} from "@tanstack/react-router";

import { AppBadge } from "@/components/app/AppBadge";
import {
  AppNativeSelect,
  AppNativeSelectOption,
} from "@/components/app/AppNativeSelect";
import { AppPageLayout } from "@/components/app/AppPageLayout";
import { AppSeparator } from "@/components/app/AppSeparator";
import {
  AppTabs,
  AppTabsContent,
  AppTabsList,
  AppTabsTrigger,
} from "@/components/app/AppTabs";
import { AppText } from "@/components/app/AppText";
import {
  polityOptionsQueryOptions,
  polityQueryOptions,
  polityReferenceQueryOptions,
  usePolity,
  usePolityOptions,
} from "@/domains/polity";
import { isResourceNotFoundError } from "@/lib/resource-not-found";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/polities/$politySlug")({
  component: PolityWorkspaceRoute,
  loader: async ({ context, location, params }) => {
    const locale = context.getLocale();

    try {
      const referenceInput = {
        locale,
        polityReference: params.politySlug,
      };
      const reference = await context.queryClient.ensureQueryData(
        polityReferenceQueryOptions(referenceInput),
      );
      if (reference.slug !== params.politySlug) {
        context.queryClient.setQueryData(
          polityReferenceQueryOptions({
            locale,
            polityReference: reference.slug,
          }).queryKey,
          reference,
        );
        throw redirect({
          href: location.href.replace(
            `/polities/${params.politySlug}`,
            `/polities/${reference.slug}`,
          ),
          replace: true,
        });
      }

      const [, polity] = await Promise.all([
        context.queryClient.ensureQueryData(
          polityOptionsQueryOptions({ locale }),
        ),
        context.queryClient.ensureQueryData(
          polityQueryOptions({ locale, polityId: reference.id }),
        ),
      ]);

      return { polityId: reference.id, shellLabel: polity.name };
    } catch (error) {
      if (isResourceNotFoundError(error)) {
        throw notFound();
      }

      throw error;
    }
  },
  staticData: {
    shell: {
      back: { label: msg`All polities`, target: { to: "/polities" } },
      level: "workspace",
      section: "polities",
      target: { params: "politySlug", to: "/polities/$politySlug" },
    },
  },
});

function PolityWorkspaceRoute() {
  const { i18n, t } = useLingui();
  const { politySlug } = Route.useParams();
  const { polityId } = Route.useLoaderData();
  const { data: polities } = usePolityOptions({ locale: i18n.locale });
  const { data: polity } = usePolity({
    locale: i18n.locale,
    polityId,
  });
  const navigate = Route.useNavigate();
  const compactWorkspaceChrome = useMatches({
    select: (matches) =>
      matches.at(-1)?.staticData.shell?.compactWorkspaceChrome ?? "visible",
  });
  const hideCompactWorkspaceChrome = compactWorkspaceChrome === "hidden";
  const pathname = useRouterState({
    select: (state) => state.location.pathname,
  });
  const activeWorkspaceTab = pathname.includes("/motions")
    ? "motions"
    : pathname.endsWith("/government")
      ? "government"
      : pathname.endsWith("/record")
        ? "record"
        : "home";
  const workspaceNavigation = [
    {
      label: t`Home`,
      to: "/polities/$politySlug",
      value: "home",
    },
    {
      label: t`Motions`,
      to: "/polities/$politySlug/motions",
      value: "motions",
    },
    {
      label: t`Government`,
      to: "/polities/$politySlug/government",
      value: "government",
    },
    {
      label: t`Record`,
      to: "/polities/$politySlug/record",
      value: "record",
    },
  ] as const;

  return (
    <AppPageLayout measure="wide">
      <header
        className={cn(
          "space-y-4",
          hideCompactWorkspaceChrome && "hidden md:block",
        )}
      >
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="min-w-0">
            <div className="mb-2 flex flex-wrap items-center gap-2">
              {polity.readiness === "forming" ? (
                <AppBadge variant="outline">
                  <Trans>Forming</Trans>
                </AppBadge>
              ) : null}
              {polity.readiness === "unavailable" ? (
                <AppBadge variant="outline">
                  <Trans>Unavailable</Trans>
                </AppBadge>
              ) : null}
              <AppBadge variant="outline">
                {polity.visibility === "public" ? (
                  <Trans>Public</Trans>
                ) : (
                  <Trans>Private</Trans>
                )}
              </AppBadge>
              <AppText as="span" variant="caption">
                <Trans>Constitution v{polity.constitutionVersion}</Trans>
              </AppText>
            </div>
            <AppText
              as={hideCompactWorkspaceChrome ? "div" : "h1"}
              className="truncate"
              variant="pageTitle"
            >
              {polity.name}
            </AppText>
            <AppText className="mt-2" variant="supporting">
              <Plural
                value={polity.memberCount}
                one="# member"
                other="# members"
              />
            </AppText>
          </div>
          <div className="flex flex-col gap-1.5 text-xs font-medium text-muted-foreground">
            <label htmlFor="polity-switcher">
              <Trans>Switch polity</Trans>
            </label>
            <AppNativeSelect
              aria-label={t`Switch polity`}
              autoComplete="off"
              className="w-full sm:w-64"
              id="polity-switcher"
              name="polity-switcher"
              onChange={(event) => {
                void navigate({
                  params: { politySlug: event.currentTarget.value },
                  to: "/polities/$politySlug",
                });
              }}
              value={polity.slug}
            >
              {polities.map((option) => (
                <AppNativeSelectOption key={option.id} value={option.slug}>
                  {option.name}
                </AppNativeSelectOption>
              ))}
            </AppNativeSelect>
          </div>
        </div>
      </header>

      <AppTabs className="min-w-0 flex-col gap-0" value={activeWorkspaceTab}>
        <div
          className={cn(
            "-mx-4 sm:-mx-6 md:-mx-8",
            hideCompactWorkspaceChrome && "hidden md:block",
          )}
        >
          <nav
            aria-label={t`${polity.name} navigation`}
            className="no-scrollbar overflow-x-auto px-4 pb-4 sm:px-6 md:px-8"
          >
            <AppTabsList className="min-w-max justify-start">
              {workspaceNavigation.map((item) => (
                <AppTabsTrigger
                  className="flex-none px-3"
                  key={item.value}
                  nativeButton={false}
                  render={
                    <Link
                      params={{ politySlug }}
                      preload="intent"
                      to={item.to}
                    />
                  }
                  value={item.value}
                >
                  {item.label}
                </AppTabsTrigger>
              ))}
            </AppTabsList>
          </nav>
          <AppSeparator />
        </div>

        <AppTabsContent className="min-w-0 pt-6" value={activeWorkspaceTab}>
          <Outlet />
        </AppTabsContent>
      </AppTabs>
    </AppPageLayout>
  );
}
