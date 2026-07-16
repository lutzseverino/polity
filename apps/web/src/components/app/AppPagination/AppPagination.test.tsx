import { setupI18n } from "@lingui/core";
import { I18nProvider } from "@lingui/react";
import { fireEvent, render, screen } from "@testing-library/react";
import type { ComponentProps } from "react";
import { describe, expect, it, vi } from "vitest";

import { AppPagination } from "@/components/app/AppPagination";

const i18n = setupI18n({ locale: "en", messages: { en: {} } });

function renderPagination(
  props: Partial<ComponentProps<typeof AppPagination>> = {},
) {
  const onPageChange = vi.fn();

  render(
    <I18nProvider i18n={i18n}>
      <AppPagination
        onPageChange={onPageChange}
        page={2}
        totalPages={5}
        {...props}
      />
    </I18nProvider>,
  );

  return { onPageChange };
}

describe("AppPagination", () => {
  it("stays out of the document when there is only one page", () => {
    renderPagination({ totalPages: 1 });

    expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
  });

  it("exposes the current page and changes pages from either direction", () => {
    const { onPageChange } = renderPagination();

    expect(
      screen.getByRole("button", { name: "Go to page 2" }),
    ).toHaveAttribute("aria-current", "page");

    fireEvent.click(
      screen.getByRole("button", { name: "Go to previous page" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Go to next page" }));

    expect(onPageChange).toHaveBeenNthCalledWith(1, 1);
    expect(onPageChange).toHaveBeenNthCalledWith(2, 3);
  });

  it("uses a bounded page window for large result sets", () => {
    renderPagination({ page: 10, totalPages: 20 });

    expect(screen.getByRole("button", { name: "Go to page 1" })).toBeVisible();
    expect(screen.getByRole("button", { name: "Go to page 9" })).toBeVisible();
    expect(screen.getByRole("button", { name: "Go to page 10" })).toBeVisible();
    expect(screen.getByRole("button", { name: "Go to page 11" })).toBeVisible();
    expect(screen.getByRole("button", { name: "Go to page 20" })).toBeVisible();
  });
});
