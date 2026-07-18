import { createMemoryHistory, RouterProvider } from "@tanstack/react-router";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";

import { AppProviders } from "@/app/providers/AppProviders";
import { createAppRouter } from "@/app/router";
import { shellSectionDefinitions } from "@/app/shell/shell-route-context";

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

const rootDestinationPaths = Object.values(shellSectionDefinitions).map(
  ({ target }) => target.to,
);

describe("first governing journey", () => {
  it.each([
    ["wide", "/polities"],
    ["standard", "/home"],
    ["focused", "/polities/new"],
    ["narrow", "/polities/invitations/invitation-supper-club"],
  ] as const)("uses the %s page measure at %s", async (measure, pathname) => {
    const router = createTestRouter(pathname);

    renderRouter(router);

    await waitFor(() => {
      expect(
        document.querySelector('[data-slot="page-layout"]'),
      ).toHaveAttribute("data-measure", measure);
    });
  });

  it.each(
    rootDestinationPaths,
  )("uses the compact shell title and wide content heading at %s", async (pathname) => {
    const router = createTestRouter(pathname);

    renderRouter(router);

    const heading = await screen.findByRole("heading", { level: 1 });

    expect(heading.closest("header")).toHaveAttribute(
      "data-slot",
      "page-header",
    );
    expect(heading.closest("main")).toHaveAttribute("data-shell-level", "root");
    expect(
      screen.queryByRole("navigation", { name: "Current location" }),
    ).not.toBeInTheDocument();
  });

  it("lets an eligible member understand and record an official vote", async () => {
    const user = userEvent.setup();
    const router = createTestRouter(
      "/polities/11111111-1111-4111-8111-111111111111/motions/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
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
    ).toHaveAttribute("href", "/polities/11111111-1111-4111-8111-111111111111");
    const primaryNavigation = screen.getByRole("navigation", {
      name: "Primary Navigation",
    });
    for (const { target } of Object.values(shellSectionDefinitions).filter(
      ({ target }) => target.to !== shellSectionDefinitions.me.target.to,
    )) {
      expect(
        primaryNavigation.querySelector(`a[href="${target.to}"]`),
      ).not.toBeNull();
    }
    expect(
      screen.queryByRole("navigation", { name: "Current location" }),
    ).not.toBeInTheDocument();
    const accountLink = screen.getByRole("link", { name: "Open account" });
    expect(accountLink).toHaveAttribute(
      "href",
      shellSectionDefinitions.me.target.to,
    );
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
    const politySection = screen.getByRole("region", { name: "3 polities" });
    expect(politySection).toBeInTheDocument();
    const foundPolityLink = screen.getByRole("link", {
      name: /found a polity/i,
    });
    expect(foundPolityLink).toHaveAttribute("href", "/polities/new");
    expect(
      within(politySection).getByRole("link", {
        name: /found a polity/i,
      }),
    ).toBe(foundPolityLink);
    expect(politySection.querySelector("a")).toBe(foundPolityLink);
    const pageLayout = document.querySelector<HTMLElement>(
      '[data-slot="page-layout"]',
    );
    const separators = pageLayout?.querySelectorAll<HTMLElement>(
      '[data-slot="separator"]',
    );
    expect(separators).toHaveLength(2);
    const pageSeparator = separators?.[0];
    const featureSeparator = separators?.[1];
    expect(pageSeparator).not.toHaveClass("data-horizontal:bg-linear-to-r");
    expect(pageSeparator?.parentElement).toHaveClass(
      "-mx-4",
      "hidden",
      "sm:-mx-6",
      "md:-mx-8",
      "md:block",
    );
    expect(pageSeparator?.parentElement?.nextElementSibling).toBe(
      politySection,
    );
    expect(featureSeparator).toHaveClass(
      "data-horizontal:bg-linear-to-r",
      "md:hidden",
    );
    expect(politySection).not.toContainElement(pageSeparator ?? null);
    expect(politySection).toContainElement(featureSeparator ?? null);
    expect(politySection.querySelectorAll('[data-slot="empty"]')).toHaveLength(
      1,
    );
    const searchLabel = document.querySelector('label[for="polity-search"]');
    expect(searchLabel).toHaveClass("sr-only");
    const search = screen.getByRole("searchbox", { name: "Search polities" });
    const searchControls = search.closest("search")?.parentElement;
    expect(politySection.firstElementChild).toBe(foundPolityLink);
    expect(foundPolityLink.nextElementSibling).toBe(featureSeparator);
    expect(featureSeparator?.nextElementSibling).toBe(searchControls);
    expect(foundPolityLink).not.toHaveClass("order-1");
    expect(foundPolityLink).not.toHaveClass("md:order-3");
    expect(featureSeparator).toHaveClass("md:hidden");
    expect(searchControls).toHaveClass("md:order-first");
    expect(foundPolityLink.compareDocumentPosition(search)).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    );
    expect(screen.queryByRole("region", { name: "Invitations" })).toBeNull();
    expect(screen.queryByText("Sunday Supper Club")).toBeNull();
    const thursdayAssemblyLink = screen.getByRole("link", {
      name: /the thursday assembly/i,
    });
    expect(
      within(thursdayAssemblyLink).getByText("Public"),
    ).toBeInTheDocument();
    expect(
      within(thursdayAssemblyLink).getByText("Assembly"),
    ).toBeInTheDocument();
    expect(
      within(thursdayAssemblyLink).getByText("Constitution v2"),
    ).toBeInTheDocument();
    expect(within(thursdayAssemblyLink).queryByText("Ready")).toBeNull();

    const neighbourhoodTableLink = screen.getByRole("link", {
      name: /neighbourhood table/i,
    });
    expect(
      within(neighbourhoodTableLink).getByText("Constitution v1"),
    ).toBeInTheDocument();
    expect(within(neighbourhoodTableLink).queryByText("Forming")).toBeNull();

    const weekendCouncilLink = screen.getByRole("link", {
      name: /weekend council/i,
    });
    expect(within(weekendCouncilLink).getByText("Council")).toBeInTheDocument();
    expect(within(weekendCouncilLink).queryByText("Ready")).toBeNull();
  });

  it("searches polities from a URL-backed query", async () => {
    const user = userEvent.setup();
    const router = createTestRouter("/polities?query=weekend");

    renderRouter(router);

    const search = await screen.findByRole("searchbox", {
      name: "Search polities",
    });
    expect(search).toHaveValue("weekend");
    expect(screen.getByText("Weekend Council")).toBeInTheDocument();
    expect(screen.queryByText("The Thursday Assembly")).not.toBeInTheDocument();

    await user.clear(search);

    expect(
      await screen.findByText("The Thursday Assembly"),
    ).toBeInTheDocument();
    await waitFor(() => {
      expect(router.state.location.search).toEqual({});
    });
  });

  it("offers a useful empty state for an unmatched polity search", async () => {
    const user = userEvent.setup();
    const router = createTestRouter("/polities?query=unknown");

    renderRouter(router);

    const emptyStateHeading = await screen.findByRole("heading", {
      name: "No matching polities",
    });
    expect(emptyStateHeading).toBeInTheDocument();
    expect(
      screen.getByText("No polities match “unknown”."),
    ).toBeInTheDocument();
    const emptyState = emptyStateHeading.closest('[data-slot="empty"]');
    expect(emptyState).not.toBeNull();
    expect(
      within(emptyState as HTMLElement).queryByRole("link", {
        name: /found a polity/i,
      }),
    ).not.toBeInTheDocument();
    const foundPolityLinks = screen.getAllByRole("link", {
      name: /found a polity/i,
    });
    expect(foundPolityLinks).toHaveLength(1);
    expect(foundPolityLinks[0]).toHaveAttribute("href", "/polities/new");
    expect(foundPolityLinks[0]?.parentElement?.firstElementChild).toBe(
      foundPolityLinks[0],
    );

    await user.click(screen.getByRole("button", { name: "Clear search" }));

    expect(
      await screen.findByText("The Thursday Assembly"),
    ).toBeInTheDocument();
    expect(router.state.location.search).toEqual({});
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
      "/polities/membership-invitations/invitation-supper-club",
    );
    expect(router.history.location.pathname).toBe(
      "/polities/membership-invitations/invitation-supper-club",
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

  it("does not stack the action launcher over an open invitation task", async () => {
    const user = userEvent.setup();
    const router = createTestRouter("/inbox");

    renderRouter(router);

    await user.click(
      await screen.findByRole("link", {
        name: /invitation to join sunday supper club/i,
      }),
    );
    const invitationDialog = await screen.findByRole("dialog", {
      name: "Join Sunday Supper Club?",
    });

    fireEvent.keyDown(document, { key: "k", metaKey: true });

    expect(screen.getAllByRole("dialog")).toEqual([invitationDialog]);
    expect(
      screen.queryByRole("dialog", { name: "Make something happen" }),
    ).not.toBeInTheDocument();
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

  it("renders a direct invitation URL as passwordless token onboarding", async () => {
    const router = createTestRouter(
      "/polities/invitations/invitation-supper-club",
    );

    renderRouter(router);

    expect(
      await screen.findByRole("heading", {
        name: "Join Sunday Supper Club",
        level: 1,
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sign up" })).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Join polity" }),
    ).not.toBeInTheDocument();
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
      "/polities/membership-invitations/invitation-supper-club",
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
      name: "What do you want to do?",
    });
    await user.type(actionInput, "invite");
    expect(screen.getByRole("status")).toHaveTextContent(
      "1 matching action. 1 is allowed.",
    );
    await user.click(screen.getByRole("link", { name: /invite a member/i }));

    expect(router.state.location.pathname).toBe("/actions/new");
    expect(router.state.location.searchStr).toBe(
      "?action=invite-member&polity=11111111-1111-4111-8111-111111111111",
    );
    const actionHeading = await screen.findByRole("heading", {
      name: "Invite a Member",
    });
    expect(actionHeading).toBeInTheDocument();
    expect(actionHeading.closest("main")).toHaveAttribute(
      "data-shell-level",
      "task",
    );
    expect(
      screen.getByText(
        /never become a binding government action automatically/i,
      ),
    ).toBeInTheDocument();
  });

  it("ignores repeated action-launcher shortcut events", async () => {
    const router = createTestRouter("/home");

    renderRouter(router);

    await screen.findByRole("link", { name: "Home" });
    const preventedShortcut = new KeyboardEvent("keydown", {
      bubbles: true,
      cancelable: true,
      key: "k",
      metaKey: true,
    });
    preventedShortcut.preventDefault();
    document.dispatchEvent(preventedShortcut);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

    fireEvent.keyDown(document, { key: "k", metaKey: true, repeat: true });
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

    fireEvent.keyDown(document, { key: "k", metaKey: true });
    expect(
      await screen.findByRole("dialog", { name: "Make something happen" }),
    ).toBeInTheDocument();

    fireEvent.keyDown(document, { key: "k", metaKey: true, repeat: true });
    expect(
      screen.getByRole("dialog", { name: "Make something happen" }),
    ).toBeInTheDocument();
  });

  it("scopes the polity-home action finder to the current polity", async () => {
    const user = userEvent.setup();
    const router = createTestRouter(
      "/polities/22222222-2222-4222-8222-222222222222",
    );

    renderRouter(router);

    const actionTrigger = await screen.findByRole("button", {
      name: /^start an action/i,
    });
    expect(
      screen.getByRole("link", { name: /finish forming the polity/i }),
    ).toHaveAttribute(
      "href",
      "/actions/new?action=invite-member&polity=22222222-2222-4222-8222-222222222222",
    );
    expect(
      screen.getAllByRole("button", { name: /^start an action/i }),
    ).toHaveLength(1);

    await user.click(actionTrigger);

    const dialog = await screen.findByRole("dialog", {
      name: "Make something happen",
    });
    const polityPicker = within(dialog).getByRole("combobox", {
      name: "In this polity",
    });
    const actionInput = within(dialog).getByRole("textbox", {
      name: "What do you want to do?",
    });

    expect(polityPicker).toHaveValue("22222222-2222-4222-8222-222222222222");
    expect(
      polityPicker.closest('[data-slot="native-select-wrapper"]'),
    ).toHaveClass(
      "w-full",
      "min-w-0",
      "[&_select]:overflow-hidden",
      "[&_select]:text-ellipsis",
      "[&_select]:whitespace-nowrap",
    );
    expect(actionInput).toHaveClass("min-w-0", "text-ellipsis");
    expect(
      within(dialog).getByText(
        "3 actions currently allowed in Neighbourhood Table",
      ),
    ).toBeInTheDocument();
    expect(
      within(dialog).getByRole("link", { name: /invite someone to join/i }),
    ).toHaveAttribute(
      "href",
      "/actions/new?action=invite-member&polity=22222222-2222-4222-8222-222222222222",
    );
  });

  it("presents the polity home as one actionable feed", async () => {
    const router = createTestRouter(
      "/polities/11111111-1111-4111-8111-111111111111",
    );

    renderRouter(router);

    const dinnerLink = await screen.findByRole("link", {
      name: /vote on shared thursday dinner/i,
    });
    const candidacyLink = screen.getByRole("link", {
      name: /respond to your nomination/i,
    });
    const workspaceNavigation = screen.getByRole("navigation", {
      name: "The Thursday Assembly navigation",
    });
    const workspaceTabs = within(workspaceNavigation).getAllByRole("tab");

    expect(workspaceTabs).toHaveLength(4);
    const homeTab = within(workspaceNavigation).getByRole("tab", {
      name: "Home",
    });
    expect(homeTab.tagName).toBe("A");
    expect(homeTab).toHaveAttribute("aria-selected", "true");
    expect(homeTab).toHaveAttribute(
      "href",
      "/polities/11111111-1111-4111-8111-111111111111",
    );
    expect(workspaceNavigation.nextElementSibling).toHaveAttribute(
      "data-slot",
      "separator",
    );
    expect(dinnerLink).toHaveAttribute(
      "href",
      "/polities/11111111-1111-4111-8111-111111111111/motions/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
    );
    expect(candidacyLink).toHaveAttribute(
      "href",
      "/polities/11111111-1111-4111-8111-111111111111/motions/bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2",
    );
    expect(dinnerLink).toHaveAttribute("data-slot", "link-surface");
    expect(
      dinnerLink.querySelector('[data-slot="link-surface-indicator"]'),
    ).toHaveAttribute("aria-hidden", "true");
    expect(
      dinnerLink.querySelector('[data-slot="link-surface-indicator"]'),
    ).toHaveClass(
      "group-hover/link-surface:text-foreground",
      "group-focus-visible/link-surface:text-foreground",
      "motion-safe:group-hover/link-surface:translate-x-0.5",
    );
    expect(dinnerLink.querySelector("a")).toBeNull();
    expect(
      screen
        .getAllByRole("link")
        .filter(
          (link) =>
            link.getAttribute("href") ===
            "/polities/11111111-1111-4111-8111-111111111111/motions/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
        ),
    ).toHaveLength(1);
    expect(screen.getByRole("heading", { name: "For you" })).toBeVisible();
    expect(screen.getByRole("heading", { name: "Latest" })).toBeVisible();
    expect(screen.queryByText("Active motions")).not.toBeInTheDocument();
    expect(screen.queryByText("Government status")).not.toBeInTheDocument();
    expect(screen.queryByText("Ready")).not.toBeInTheDocument();
  });

  it("uses calm polity-home empty states", async () => {
    const router = createTestRouter(
      "/polities/33333333-3333-4333-8333-333333333333",
    );

    renderRouter(router);

    expect(await screen.findByText("You’re all caught up")).toBeVisible();
    expect(screen.getByText("It’s quiet here for now")).toBeVisible();
    expect(screen.queryByText(/no actions need you/i)).not.toBeInTheDocument();
    expect(
      screen.queryByText(/there are no active motions/i),
    ).not.toBeInTheDocument();
  });

  it("keeps candidacy consent distinct from voting", async () => {
    const user = userEvent.setup();
    const router = createTestRouter(
      "/polities/11111111-1111-4111-8111-111111111111/motions/bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2",
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
      "/polities/11111111-1111-4111-8111-111111111111/motions/cccccccc-cccc-4ccc-8ccc-ccccccccccc3",
    );

    renderRouter(router);

    expect(await screen.findByText("Certified result")).toBeInTheDocument();
    expect(screen.getAllByText("Adopted")).not.toHaveLength(0);
    expect(
      screen.getByRole("link", { name: "Official record No. 41" }),
    ).toHaveAttribute(
      "href",
      "/polities/11111111-1111-4111-8111-111111111111/record",
    );
  });

  it("shows how a polity is governed without roadmap placeholder copy", async () => {
    const router = createTestRouter(
      "/polities/11111111-1111-4111-8111-111111111111/government",
    );

    renderRouter(router);

    expect(
      await screen.findByRole("heading", { name: "Government status" }),
    ).toBeVisible();
    expect(screen.getByText("Tribune")).toBeVisible();
    expect(screen.getByText("Ordinary resolution")).toBeVisible();
    expect(
      screen.getByText("A constitutional council republic."),
    ).toBeVisible();
    expect(
      screen.queryByText("Planned polity destination"),
    ).not.toBeInTheDocument();
  });

  it("shows numbered official evidence with links back to its motion", async () => {
    const router = createTestRouter(
      "/polities/11111111-1111-4111-8111-111111111111/record",
    );

    renderRouter(router);

    expect(
      await screen.findByRole("heading", { name: "Official record" }),
    ).toBeVisible();
    expect(
      screen.getByText("Shared Thursday Dinner opened for voting"),
    ).toBeVisible();
    expect(screen.getByText("Voting opened.")).toBeVisible();
    expect(
      screen.getAllByRole("link", { name: "View motion" })[0],
    ).toHaveAttribute(
      "href",
      "/polities/11111111-1111-4111-8111-111111111111/motions/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
    );
    expect(
      screen.queryByText("Planned polity destination"),
    ).not.toBeInTheDocument();
  });

  it("uses a calm empty state when a polity has no official activity", async () => {
    const router = createTestRouter(
      "/polities/33333333-3333-4333-8333-333333333333/record",
    );

    renderRouter(router);

    expect(await screen.findByText("No official activity yet")).toBeVisible();
    expect(
      screen.getByText("Formal decisions and changes will appear here."),
    ).toBeVisible();
  });
});
