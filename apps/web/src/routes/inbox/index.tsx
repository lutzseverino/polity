import { Trans, useLingui } from "@lingui/react/macro";
import { createFileRoute } from "@tanstack/react-router";

import { AppBadge } from "@/components/app/AppBadge";
import { AppButton } from "@/components/app/AppButton";
import { AppPageHeader } from "@/components/app/AppPageHeader";
import { AppText } from "@/components/app/AppText";
import {
  type InboxCategory,
  InboxItemLink,
  useInboxItems,
} from "@/domains/inbox";

type InboxSearch = Readonly<{
  category?: InboxCategory;
}>;

export const Route = createFileRoute("/inbox/")({
  component: InboxRoute,
  validateSearch: (search): InboxSearch => ({
    category: search.category === "updates" ? "updates" : undefined,
  }),
});

function InboxRoute() {
  const { i18n, t } = useLingui();
  const categories: readonly Readonly<{
    label: string;
    value: InboxCategory;
  }>[] = [
    { label: t`Needs Action`, value: "needs-action" },
    { label: t`Updates`, value: "updates" },
  ];
  const navigate = Route.useNavigate();
  const { category } = Route.useSearch();
  const activeCategory = category ?? "needs-action";
  const { data: items } = useInboxItems({ locale: i18n.locale });
  const visibleItems = items.filter((item) => item.category === activeCategory);

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <AppPageHeader
        description={
          <Trans>
            Find everything that needs your attention here. Each item opens
            where that decision belongs.
          </Trans>
        }
        eyebrow={<Trans>Your Attention</Trans>}
        title={<Trans>Inbox</Trans>}
      />

      <fieldset className="flex gap-2">
        <legend className="sr-only">
          <Trans>Inbox Categories</Trans>
        </legend>
        {categories.map((category) => {
          const count = items.filter(
            (item) => item.category === category.value,
          ).length;
          const isActive = activeCategory === category.value;

          return (
            <AppButton
              aria-pressed={isActive}
              key={category.value}
              onClick={() => {
                void navigate({ search: { category: category.value } });
              }}
              variant={isActive ? "default" : "outline"}
            >
              {category.label}
              <AppBadge variant={isActive ? "secondary" : "outline"}>
                {count}
              </AppBadge>
            </AppButton>
          );
        })}
      </fieldset>

      <section aria-live="polite" aria-labelledby="inbox-section-heading">
        <AppText
          as="h2"
          className="mb-3"
          id="inbox-section-heading"
          variant="sectionTitle"
        >
          {activeCategory === "needs-action" ? (
            <Trans>Needs Action</Trans>
          ) : (
            <Trans>Updates</Trans>
          )}
        </AppText>
        {visibleItems.length > 0 ? (
          <div className="space-y-3">
            {visibleItems.map((item) => (
              <InboxItemLink item={item} key={item.id} />
            ))}
          </div>
        ) : (
          <div className="rounded-xl border border-dashed p-8 text-center">
            <AppText variant="strong">
              <Trans>Nothing Here Right Now</Trans>
            </AppText>
            <AppText className="mt-1" variant="supporting">
              <Trans>
                New items will appear here when they need your attention.
              </Trans>
            </AppText>
          </div>
        )}
      </section>
    </div>
  );
}
