import { msg } from "@lingui/core/macro";
import { Plural, Trans, useLingui } from "@lingui/react/macro";
import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowRight, Landmark, Plus, Search, SearchX, X } from "lucide-react";
import { useEffect, useState } from "react";

import { AppButton } from "@/components/app/AppButton";
import {
  AppEmpty,
  AppEmptyContent,
  AppEmptyDescription,
  AppEmptyHeader,
  AppEmptyMedia,
  AppEmptyTitle,
} from "@/components/app/AppEmpty";
import { AppInput } from "@/components/app/AppInput";
import { AppPageHeader } from "@/components/app/AppPageHeader";
import { AppPageLayout } from "@/components/app/AppPageLayout";
import { AppPagination } from "@/components/app/AppPagination";
import { AppSeparator } from "@/components/app/AppSeparator";
import { AppText } from "@/components/app/AppText";
import { countOpenInboxTasksForPolity, useInboxItems } from "@/domains/inbox";
import {
  PolityCard,
  politiesQueryOptions,
  usePolities,
} from "@/domains/polity";
import { cn } from "@/lib/utils";

const maximumPolitySearchLength = 120;
const polityDirectoryPageSize = 12;
const politySearchDebounceMs = 300;

type PolityDirectorySearch = Readonly<{
  page?: number;
  query?: string;
}>;

function normalizeDirectoryQuery(query: unknown) {
  if (typeof query !== "string") {
    return undefined;
  }

  const normalized = query.trim().slice(0, maximumPolitySearchLength);

  return normalized || undefined;
}

function normalizeDirectoryPage(page: unknown) {
  return typeof page === "number" && Number.isSafeInteger(page) && page > 1
    ? page
    : undefined;
}

export const Route = createFileRoute("/polities/")({
  component: PolityDirectoryRoute,
  validateSearch: (search): PolityDirectorySearch => ({
    page: normalizeDirectoryPage(search.page),
    query: normalizeDirectoryQuery(search.query),
  }),
  loaderDeps: ({ search }) => ({
    page: search.page ?? 1,
    query: search.query,
  }),
  loader: ({ context, deps }) => {
    const locale = context.getLocale();

    return context.queryClient.ensureQueryData(
      politiesQueryOptions({
        locale,
        page: deps.page - 1,
        query: deps.query,
        size: polityDirectoryPageSize,
      }),
    );
  },
  staticData: {
    shell: {
      label: msg`Polities`,
      level: "root",
      section: "polities",
      target: { to: "/polities" },
    },
  },
});

function PolityDirectoryRoute() {
  const { i18n, t } = useLingui();
  const navigate = Route.useNavigate();
  const { page = 1, query } = Route.useSearch();
  const [searchValue, setSearchValue] = useState(query ?? "");
  const { data: polityPage } = usePolities({
    locale: i18n.locale,
    page: page - 1,
    query,
    size: polityDirectoryPageSize,
  });
  const { data: inboxItems } = useInboxItems({ locale: i18n.locale });
  const hasQuery = Boolean(query);
  const { content: polities, page: pageMetadata } = polityPage;

  useEffect(() => {
    setSearchValue(query ?? "");
  }, [query]);

  useEffect(() => {
    const normalizedSearchValue = normalizeDirectoryQuery(searchValue);

    if (normalizedSearchValue === query) {
      return;
    }

    const timeout = window.setTimeout(() => {
      navigate({
        replace: true,
        search: {
          page: undefined,
          query: normalizedSearchValue,
        },
      }).catch(() => undefined);
    }, politySearchDebounceMs);

    return () => window.clearTimeout(timeout);
  }, [navigate, query, searchValue]);

  useEffect(() => {
    if (pageMetadata.totalPages > 0 && page > pageMetadata.totalPages) {
      navigate({
        replace: true,
        search: {
          page:
            pageMetadata.totalPages === 1 ? undefined : pageMetadata.totalPages,
          query,
        },
      }).catch(() => undefined);
    }
  }, [navigate, page, pageMetadata.totalPages, query]);

  function clearSearch() {
    setSearchValue("");
    navigate({
      replace: true,
      search: { page: undefined, query: undefined },
    }).catch(() => undefined);
  }

  function changePage(nextPage: number) {
    navigate({
      search: {
        page: nextPage === 1 ? undefined : nextPage,
        query,
      },
    })
      .then(() => {
        document.getElementById("polity-results-heading")?.focus();
      })
      .catch(() => undefined);
  }

  return (
    <AppPageLayout measure="wide">
      <AppPageHeader title={<Trans>Polities</Trans>} />

      <div className="-mx-4 hidden sm:-mx-6 md:-mx-8 md:block">
        <AppSeparator aria-hidden="true" />
      </div>

      <section
        aria-labelledby="polity-results-heading"
        className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3"
      >
        <Link className="focus-indicator group rounded-xl" to="/polities/new">
          <AppEmpty className="h-full flex-row justify-start gap-3 border bg-transparent p-4 text-left transition-colors group-hover:border-foreground/30 group-hover:bg-muted/50">
            <AppEmptyMedia
              className="mb-0 size-10 rounded-full border border-dashed bg-transparent"
              variant="icon"
            >
              <Plus aria-hidden="true" className="size-5" />
            </AppEmptyMedia>
            <AppEmptyHeader className="min-w-0 max-w-none items-start gap-1 text-left">
              <AppEmptyTitle className="text-base font-semibold">
                <Trans>Found a polity</Trans>
              </AppEmptyTitle>
              <AppEmptyDescription>
                <Trans>
                  Create a new space and establish how it will govern itself.
                </Trans>
              </AppEmptyDescription>
            </AppEmptyHeader>
          </AppEmpty>
        </Link>

        <AppSeparator
          aria-hidden="true"
          className="md:hidden"
          variant="gradient"
        />

        <div className="flex flex-col gap-3 md:order-first md:flex-row md:items-center md:justify-between lg:col-span-2 xl:col-span-3">
          <search className="w-full max-w-md">
            <form
              onSubmit={(event) => {
                event.preventDefault();
                navigate({
                  replace: true,
                  search: {
                    page: undefined,
                    query: normalizeDirectoryQuery(searchValue),
                  },
                }).catch(() => undefined);
              }}
            >
              <label className="sr-only" htmlFor="polity-search">
                <Trans>Search polities</Trans>
              </label>
              <div className="relative">
                <Search
                  aria-hidden="true"
                  className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
                />
                <AppInput
                  className="h-11 appearance-none pr-11 pl-9 md:h-8 md:pr-9 [&::-webkit-search-cancel-button]:appearance-none"
                  id="polity-search"
                  maxLength={maximumPolitySearchLength}
                  onChange={(event) =>
                    setSearchValue(event.currentTarget.value)
                  }
                  placeholder={t`Search polities…`}
                  type="search"
                  value={searchValue}
                />
                {searchValue ? (
                  <AppButton
                    aria-label={t`Clear polity search`}
                    className="absolute inset-y-0 right-0 my-auto size-11 md:size-8"
                    onClick={clearSearch}
                    size="icon"
                    type="button"
                    variant="ghost"
                  >
                    <X aria-hidden="true" />
                  </AppButton>
                ) : null}
              </div>
            </form>
          </search>

          <AppText
            aria-live="polite"
            as="h2"
            className="shrink-0 self-end text-right md:self-auto md:text-left"
            id="polity-results-heading"
            tabIndex={-1}
            variant="supporting"
          >
            <Plural
              value={pageMetadata.totalElements}
              one="# polity"
              other="# polities"
            />
          </AppText>
        </div>

        {polities.map((polity) => {
          const openTaskCount = countOpenInboxTasksForPolity(
            inboxItems,
            polity.slug,
          );

          return (
            <Link
              className="focus-indicator group min-w-0 rounded-xl"
              key={polity.id}
              params={{ politySlug: polity.slug }}
              to="/polities/$politySlug"
            >
              <PolityCard>
                <PolityCard.Header>
                  <PolityCard.Identity>
                    <PolityCard.Title className="wrap-break-word">
                      {polity.name}
                    </PolityCard.Title>
                    <PolityCard.Description className="flex flex-wrap items-center gap-x-2 gap-y-1">
                      <span>{polity.institutionName}</span>
                      <span aria-hidden="true">·</span>
                      <span>
                        {polity.visibility === "public" ? (
                          <Trans>Public</Trans>
                        ) : (
                          <Trans>Private</Trans>
                        )}
                      </span>
                    </PolityCard.Description>
                  </PolityCard.Identity>
                </PolityCard.Header>
                <PolityCard.Footer
                  className={cn(
                    "mt-auto justify-between py-3 transition-colors",
                    openTaskCount > 0
                      ? "bg-muted/40 group-hover:bg-muted/70"
                      : "bg-transparent",
                  )}
                >
                  {openTaskCount > 0 ? (
                    <>
                      <AppText as="span" variant="strong">
                        <Plural
                          value={openTaskCount}
                          one="# action needs you"
                          other="# actions need you"
                        />
                      </AppText>
                      <ArrowRight
                        aria-hidden="true"
                        className="size-4 shrink-0 text-muted-foreground transition-colors group-hover:text-foreground"
                      />
                    </>
                  ) : (
                    <AppText
                      as="span"
                      className="font-normal text-muted-foreground"
                      variant="strong"
                    >
                      <Trans>Nothing to respond to</Trans>
                    </AppText>
                  )}
                </PolityCard.Footer>
              </PolityCard>
            </Link>
          );
        })}

        {polities.length === 0 ? (
          <AppEmpty className="border py-12 lg:col-span-1 xl:col-span-2">
            <AppEmptyMedia variant="icon">
              {hasQuery ? (
                <SearchX aria-hidden="true" />
              ) : (
                <Landmark aria-hidden="true" />
              )}
            </AppEmptyMedia>
            <AppEmptyHeader>
              <AppEmptyTitle aria-level={3} role="heading">
                {hasQuery ? (
                  <Trans>No matching polities</Trans>
                ) : (
                  <Trans>No polities yet</Trans>
                )}
              </AppEmptyTitle>
              <AppEmptyDescription>
                {hasQuery ? (
                  <Trans>No polities match “{query}”.</Trans>
                ) : (
                  <Trans>Polities you join will appear here.</Trans>
                )}
              </AppEmptyDescription>
            </AppEmptyHeader>
            {hasQuery ? (
              <AppEmptyContent className="sm:flex-row sm:justify-center">
                <AppButton onClick={clearSearch} type="button">
                  <Trans>Clear search</Trans>
                </AppButton>
              </AppEmptyContent>
            ) : null}
          </AppEmpty>
        ) : null}

        <AppPagination
          className="lg:col-span-2 xl:col-span-3"
          onPageChange={changePage}
          page={pageMetadata.number + 1}
          totalPages={pageMetadata.totalPages}
        />
      </section>
    </AppPageLayout>
  );
}
