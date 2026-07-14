import { createMemoryHistory, RouterProvider } from "@tanstack/react-router";
import { render, screen, waitFor, within } from "@testing-library/react";
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
    expect(
      screen.getByRole("navigation", { name: "Current location" }),
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
      await screen.findByRole("heading", {
        name: "Polities",
        level: 1,
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /the thursday assembly/i }),
    ).toHaveAttribute("href", "/polities/thursday-assembly");
    expect(
      screen.getByRole("navigation", { name: "Primary Navigation" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("navigation", { name: "Current location" }),
    ).not.toBeInTheDocument();
    const accountLink = screen.getByRole("link", { name: "Open account" });
    expect(accountLink).toHaveAttribute("href", "/me");
    expect(accountLink.querySelector('[data-slot="avatar"]')).not.toBeNull();
    expect(within(accountLink).getByText("Account")).toBeInTheDocument();
  });

  it("keeps the polity directory focused on existing memberships", async () => {
    const router = createTestRouter("/polities");

    renderRouter(router);

    expect(
      await screen.findByRole("heading", {
        name: "Polities",
        level: 1,
      }),
    ).toBeInTheDocument();
    const politySection = screen.getByRole("region", { name: "Polities" });
    expect(politySection).toBeInTheDocument();
    const foundPolityLink = within(politySection).getByRole("link", {
      name: /found a polity/i,
    });
    expect(foundPolityLink).toHaveAttribute("href", "/polities/new");
    const separators = politySection.querySelectorAll(
      '[data-slot="separator"]',
    );
    expect(separators).toHaveLength(2);
    expect(separators[0]).toHaveClass("hidden", "md:block");
    expect(separators[1]).toHaveClass("md:hidden");
    expect(foundPolityLink.nextElementSibling).toBe(separators[1]);
    expect(politySection.querySelector('[data-slot="empty"]')).toBeVisible();
    expect(screen.queryByRole("region", { name: "Invitations" })).toBeNull();
    expect(screen.queryByText("Sunday Supper Club")).toBeNull();
    expect(screen.getByText("The Thursday Assembly")).toBeInTheDocument();
  });

  it("resolves an invitation from Inbox as an open task", async () => {
    const user = userEvent.setup();
    const router = createTestRouter("/inbox");

    renderRouter(router);

    expect(
      await screen.findByRole("link", {
        name: "Inbox, 6 items need action",
      }),
    ).toBeInTheDocument();
    await user.click(
      screen.getByRole("link", {
        name: /invitation to join sunday supper club/i,
      }),
    );

    expect(router.state.location.pathname).toBe("/inbox");
    expect(router.state.location.maskedLocation?.pathname).toBe(
      "/polities/invitations/invitation-supper-club",
    );
    expect(router.history.location.pathname).toBe(
      "/polities/invitations/invitation-supper-club",
    );
    const invitationDialog = await screen.findByRole("dialog");
    expect(
      within(invitationDialog).getByRole("heading", {
        name: "Join Sunday Supper Club?",
        level: 2,
      }),
    ).toBeInTheDocument();

    await user.click(
      within(invitationDialog).getByRole("button", {
        name: "Join polity",
      }),
    );

    expect(
      within(invitationDialog).getByText("You joined Sunday Supper Club"),
    ).toBeInTheDocument();
    await user.click(
      within(invitationDialog).getByRole("button", { name: "Done" }),
    );
    await waitFor(() => {
      expect(screen.queryByRole("dialog")).toBeNull();
    });
    expect(
      screen.queryByText("Invitation to join Sunday Supper Club"),
    ).toBeNull();
    expect(
      screen.getByRole("link", { name: "Inbox, 5 items need action" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", {
        name: "Open Inbox, 5 items need action",
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /Needs Action\s*5/ }),
    ).toBeInTheDocument();
  });

  it("dismisses an invitation back to the exact Inbox state", async () => {
    const user = userEvent.setup();
    const router = createTestRouter("/inbox");

    renderRouter(router);

    await user.click(
      await screen.findByRole("link", {
        name: /invitation to join cabin council/i,
      }),
    );
    const invitationDialog = await screen.findByRole("dialog");
    await user.click(
      within(invitationDialog).getByRole("button", { name: "Not Now" }),
    );
    await waitFor(() => {
      expect(router.state.location.search.task).toBeUndefined();
    });

    expect(router.state.location.pathname).toBe("/inbox");
    expect(
      screen.getByText("Invitation to join Cabin Council"),
    ).toBeInTheDocument();
  });

  it("renders a direct invitation URL as the standalone fallback", async () => {
    const router = createTestRouter(
      "/polities/invitations/invitation-supper-club",
    );

    renderRouter(router);

    expect(
      await screen.findByRole("heading", {
        name: "Join Sunday Supper Club?",
        level: 1,
      }),
    ).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("keeps inbox categories in the URL and separates updates", async () => {
    const user = userEvent.setup();
    const router = createTestRouter("/inbox");

    renderRouter(router);

    expect(
      await screen.findByRole("heading", { name: "Inbox" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Invitation to join Sunday Supper Club"),
    ).toBeInTheDocument();
    const invitationLink = screen.getByRole("link", {
      name: /invitation to join sunday supper club/i,
    });
    expect(invitationLink).toHaveAttribute(
      "href",
      "/polities/invitations/invitation-supper-club",
    );

    await user.click(invitationLink);

    const invitationDialog = await screen.findByRole("dialog");
    expect(router.state.location.pathname).toBe("/inbox");
    await user.click(
      within(invitationDialog).getByRole("button", { name: "Not Now" }),
    );
    await waitFor(() => {
      expect(router.state.location.search.task).toBeUndefined();
    });
    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });

    expect(router.state.location.pathname).toBe("/inbox");

    await user.click(screen.getByRole("button", { name: /updates/i }));

    expect(router.state.location.searchStr).toBe("?category=updates");
    expect(
      screen.getByText("Autumn Cabin Budget Was Adopted"),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("Invitation to join Sunday Supper Club"),
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
