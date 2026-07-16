import { Trans, useLingui } from "@lingui/react/macro";
import { ChevronLeft, ChevronRight, Ellipsis } from "lucide-react";

import { AppButton } from "@/components/app/AppButton";
import { cn } from "@/lib/utils";

type AppPaginationProps = Readonly<{
  className?: string;
  onPageChange: (page: number) => void;
  page: number;
  totalPages: number;
}>;

type PageItem = number | "ellipsis-start" | "ellipsis-end";

function getPageItems(page: number, totalPages: number): readonly PageItem[] {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }

  const middlePages = [page - 1, page, page + 1].filter(
    (pageNumber) => pageNumber > 1 && pageNumber < totalPages,
  );
  const pages = [...new Set([1, ...middlePages, totalPages])];
  const items: PageItem[] = [];

  for (const [index, pageNumber] of pages.entries()) {
    const previousPage = pages[index - 1];

    if (previousPage !== undefined && pageNumber - previousPage > 1) {
      items.push(index === 1 ? "ellipsis-start" : "ellipsis-end");
    }

    items.push(pageNumber);
  }

  return items;
}

export function AppPagination({
  className,
  onPageChange,
  page,
  totalPages,
}: AppPaginationProps) {
  const { t } = useLingui();

  if (totalPages <= 1) {
    return null;
  }

  const currentPage = Math.min(Math.max(page, 1), totalPages);
  const pageItems = getPageItems(currentPage, totalPages);

  return (
    <nav
      aria-label={t`Pagination`}
      className={cn(
        "flex items-center justify-between gap-3 md:justify-end",
        className,
      )}
    >
      <AppButton
        aria-label={t`Go to previous page`}
        className="h-11 px-3 md:h-8"
        disabled={currentPage === 1}
        onClick={() => onPageChange(currentPage - 1)}
        type="button"
        variant="outline"
      >
        <ChevronLeft aria-hidden="true" />
        <Trans>Previous</Trans>
      </AppButton>

      <span className="text-sm text-muted-foreground md:hidden">
        <Trans>
          Page {currentPage} of {totalPages}
        </Trans>
      </span>

      <div className="hidden items-center gap-1 md:flex">
        {pageItems.map((item) =>
          typeof item === "number" ? (
            <AppButton
              aria-current={item === currentPage ? "page" : undefined}
              aria-label={t`Go to page ${item}`}
              className="size-8 p-0 aria-current-page:bg-primary aria-current-page:text-primary-foreground"
              key={item}
              onClick={() => onPageChange(item)}
              type="button"
              variant={item === currentPage ? "default" : "ghost"}
            >
              {item}
            </AppButton>
          ) : (
            <span
              aria-hidden="true"
              className="flex size-8 items-center justify-center text-muted-foreground"
              key={item}
            >
              <Ellipsis className="size-4" />
            </span>
          ),
        )}
      </div>

      <AppButton
        aria-label={t`Go to next page`}
        className="h-11 px-3 md:h-8"
        disabled={currentPage === totalPages}
        onClick={() => onPageChange(currentPage + 1)}
        type="button"
        variant="outline"
      >
        <Trans>Next</Trans>
        <ChevronRight aria-hidden="true" />
      </AppButton>
    </nav>
  );
}
