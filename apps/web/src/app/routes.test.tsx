import { createMemoryHistory, RouterProvider } from "@tanstack/react-router";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";

import { AppProviders } from "@/app/providers/AppProviders";
import { createAppRouter } from "@/app/router";

function createTestRouter(initialEntry: string) {
  return createAppRouter(
    createMemoryHistory({ initialEntries: [initialEntry] }),
  );
}

function renderRouter(router: ReturnType<typeof createTestRouter>) {
  return render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  );
}

describe("first governing journey", () => {
  it("lets an eligible member understand and record an official vote", async () => {
    const user = userEvent.setup();
    const router = createTestRouter(
      "/polities/thursday-assembly/motions/shared-dinner",
    );

    renderRouter(router);

    expect(
      await screen.findByRole("heading", { name: "Shared Thursday Dinner" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Official vote")).toBeInTheDocument();
    expect(
      screen.getByText(/reactions and comments never count/i),
    ).toBeInTheDocument();

    await user.click(
      screen.getByRole("button", { name: /yes: support the motion/i }),
    );
    await user.click(screen.getByRole("button", { name: "Record vote" }));

    expect(screen.getByText("Your vote was recorded")).toBeInTheDocument();
    expect(screen.getByText(/you voted yes/i)).toBeInTheDocument();
  });

  it("opens the polity directory as the default application destination", async () => {
    const router = createTestRouter("/");

    renderRouter(router);

    expect(
      await screen.findByRole("heading", { name: "Your Polities", level: 1 }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /the thursday assembly/i }),
    ).toHaveAttribute("href", "/polities/thursday-assembly");
    expect(
      screen.getAllByRole("navigation", { name: "Primary Navigation" }),
    ).toHaveLength(2);
  });

  it("scales invitations separately from existing memberships", async () => {
    const user = userEvent.setup();
    const router = createTestRouter("/polities");

    renderRouter(router);

    expect(
      await screen.findByRole("region", { name: "Invitations" }),
    ).toBeInTheDocument();
    const politySection = screen.getByRole("region", { name: "Your Polities" });
    expect(politySection).toBeInTheDocument();
    expect(
      within(politySection).getByRole("link", { name: /found a polity/i }),
    ).toHaveAttribute("href", "/polities/new");
    expect(politySection.querySelector('[data-slot="empty"]')).toBeVisible();
    expect(
      screen.getByRole("link", { name: /sunday supper club/i }),
    ).toHaveAttribute("href", "/polities/invitations/invitation-supper-club");
    expect(screen.getByText("Garden Cooperative")).toBeInTheDocument();
    expect(screen.getByText("Local Book Circle")).toBeInTheDocument();
    expect(screen.queryByText("Nothing Needs You")).not.toBeInTheDocument();
    expect(
      screen.queryByText("Government is operating normally."),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText(
        "One more standing member is needed for full government.",
      ),
    ).not.toBeInTheDocument();
    expect(
      screen.getAllByRole("link", { name: "View 1 More Invitation" }),
    ).toHaveLength(2);
    expect(screen.queryByText("Cabin Council")).not.toBeInTheDocument();

    await user.click(
      screen.getAllByRole("link", { name: "View 1 More Invitation" })[0],
    );

    expect(router.state.location.searchStr).toBe("?invitations=all");
    expect(screen.getByText("Cabin Council")).toBeInTheDocument();
  });

  it("keeps invitation review and acceptance under polities", async () => {
    const user = userEvent.setup();
    const router = createTestRouter("/polities");

    renderRouter(router);

    await user.click(
      await screen.findByRole("link", { name: /sunday supper club/i }),
    );

    expect(router.state.location.pathname).toBe(
      "/polities/invitations/invitation-supper-club",
    );
    expect(
      await screen.findByRole("heading", { name: "Sunday Supper Club" }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Accept Invitation" }));

    expect(screen.getByText("Invitation Accepted")).toBeInTheDocument();
  });

  it("keeps inbox categories in the URL and separates updates", async () => {
    const user = userEvent.setup();
    const router = createTestRouter("/inbox");

    renderRouter(router);

    expect(
      await screen.findByRole("heading", { name: "Inbox" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Invitation to Sunday Supper Club"),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", {
        name: /invitation to sunday supper club/i,
      }),
    ).toHaveAttribute("href", "/polities/invitations/invitation-supper-club");

    await user.click(screen.getByRole("button", { name: /updates/i }));

    expect(router.state.location.searchStr).toBe("?category=updates");
    expect(
      screen.getByText("Autumn Cabin Budget Was Adopted"),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("Invitation to Sunday Supper Club"),
    ).not.toBeInTheDocument();
  });

  it("uses text to discover a structured action handoff", async () => {
    const user = userEvent.setup();
    const router = createTestRouter("/home");

    renderRouter(router);

    const actionInput = await screen.findByRole("textbox", {
      name: "Find an Action",
    });
    await user.type(actionInput, "invite");
    await user.click(screen.getByRole("link", { name: /invite a member/i }));

    expect(router.state.location.pathname).toBe("/actions/new");
    expect(router.state.location.searchStr).toBe(
      "?action=invite-member&polity=thursday-assembly",
    );
    expect(
      await screen.findByRole("heading", { name: "Invite a Member" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        /never become a binding government action automatically/i,
      ),
    ).toBeInTheDocument();
  });

  it("keeps candidacy consent distinct from voting", async () => {
    const user = userEvent.setup();
    const router = createTestRouter(
      "/polities/thursday-assembly/motions/tribune-election",
    );

    renderRouter(router);

    expect(
      await screen.findByText("Respond to your nomination"),
    ).toBeInTheDocument();
    expect(screen.queryByText("How do you vote?")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Accept nomination" }));

    expect(screen.getByText("Response recorded")).toBeInTheDocument();
    expect(
      screen.getByText(/you accepted the nomination/i),
    ).toBeInTheDocument();
  });

  it("shows a completed motion as a certified result with an official-record path", async () => {
    const router = createTestRouter(
      "/polities/thursday-assembly/motions/autumn-cabin-budget",
    );

    renderRouter(router);

    expect(await screen.findByText("Certified result")).toBeInTheDocument();
    expect(screen.getAllByText("Adopted")).not.toHaveLength(0);
    expect(
      screen.getByRole("link", { name: "Official record No. 41" }),
    ).toHaveAttribute("href", "/polities/thursday-assembly/record");
  });
});
