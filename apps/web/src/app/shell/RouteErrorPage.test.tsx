import { setupI18n } from "@lingui/core";
import { I18nProvider } from "@lingui/react";
import { render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";

import { RouteErrorPage } from "@/app/shell/RouteErrorPage";

vi.mock("@/components/app/AppLinkButton", () => ({
  AppLinkButton: ({ children }: Readonly<{ children: ReactNode }>) => (
    <a href="/polities">{children}</a>
  ),
}));

const i18n = setupI18n({ locale: "en", messages: { en: {} } });

describe("route error page", () => {
  it("does not expose technical error details", () => {
    render(
      <I18nProvider i18n={i18n}>
        <RouteErrorPage
          error={new Error("Invalid membership invitation response.")}
          info={{ componentStack: "" }}
          reset={() => undefined}
        />
      </I18nProvider>,
    );

    expect(
      screen.getByText("Something went wrong while opening this page."),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("Invalid membership invitation response."),
    ).not.toBeInTheDocument();
  });
});
