import { msg } from "@lingui/core/macro";
import { Plural, Trans, useLingui } from "@lingui/react/macro";
import {
  createFileRoute,
  Link,
  notFound,
  Outlet,
  useMatches,
} from "@tanstack/react-router";

import { AppBadge } from "@/components/app/AppBadge";
import {
  AppNativeSelect,
  AppNativeSelectOption,
} from "@/components/app/AppNativeSelect";
import { AppText } from "@/components/app/AppText";
import {
  politiesQueryOptions,
  polityQueryOptions,
  ReadinessBadge,
  usePolities,
  usePolity,
} from "@/domains/polity";
import { isResourceNotFoundError } from "@/lib/resource-not-found";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/polities/$polityId")({
  component: PolityWorkspaceRoute,
  loader: async ({ context, params }) => {
    const input = { locale: context.getLocale(), polityId: params.polityId };

    try {
      const [, polity] = await Promise.all([
        context.queryClient.ensureQueryData(politiesQueryOptions(input)),
        context.queryClient.ensureQueryData(polityQueryOptions(input)),
      ]);

      return { shellLabel: polity.name };
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
      target: { params: "polityId", to: "/polities/$polityId" },
    },
  },
});

function PolityWorkspaceRoute() {
  const { i18n, t } = useLingui();
  const { polityId } = Route.useParams();
  const { data: polities } = usePolities({ locale: i18n.locale });
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
  const workspaceNavigation = [
    { exact: true, label: t`Overview`, to: "/polities/$polityId" },
    { exact: false, label: t`Motions`, to: "/polities/$polityId/motions" },
    {
      exact: false,
      label: t`Government`,
      to: "/polities/$polityId/government",
    },
    { exact: false, label: t`Record`, to: "/polities/$polityId/record" },
  ] as const;

  return (
    <div className="space-y-6">
      <header
        className={cn(
          "space-y-4",
          hideCompactWorkspaceChrome && "hidden md:block",
        )}
      >
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="min-w-0">
            <div className="mb-2 flex flex-wrap items-center gap-2">
              <ReadinessBadge readiness={polity.readiness} />
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
              className="sr-only md:not-sr-only md:truncate"
              variant="pageTitle"
            >
              {polity.name}
            </AppText>
            <AppText className="mt-2" variant="supporting">
              {polity.role} ·{" "}
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
                  params: { polityId: event.currentTarget.value },
                  to: "/polities/$polityId",
                });
              }}
              value={polity.id}
            >
              {polities.map((option) => (
                <AppNativeSelectOption key={option.id} value={option.id}>
                  {option.name}
                </AppNativeSelectOption>
              ))}
            </AppNativeSelect>
          </div>
        </div>
      </header>

      <nav
        aria-label={t`${polity.name} navigation`}
        className={cn(
          "no-scrollbar -mx-4 overflow-x-auto border-b px-4 sm:-mx-6 sm:px-6 md:mx-0 md:px-0",
          hideCompactWorkspaceChrome && "hidden md:block",
        )}
      >
        <div className="flex min-w-max gap-1">
          {workspaceNavigation.map((item) => (
            <Link
              activeOptions={{ exact: item.exact }}
              activeProps={{ className: "border-foreground text-foreground" }}
              className="border-b-2 border-transparent px-3 py-2.5 text-sm font-medium text-muted-foreground hover:text-foreground focus-visible:rounded-t focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              key={item.label}
              params={{ polityId: polity.id }}
              to={item.to}
            >
              {item.label}
            </Link>
          ))}
        </div>
      </nav>

      <Outlet />
    </div>
  );
}
