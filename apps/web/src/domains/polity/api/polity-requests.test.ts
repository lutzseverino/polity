import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";

import {
  getPolity,
  getPolityActions,
  getPolityMotion,
  listPolities,
} from "@/domains/polity/api/polity-requests";
import { apiMockServer } from "@/test/mocks/server";

describe("polity requests", () => {
  it("returns zero-based pages with stable metadata", async () => {
    const firstPage = await listPolities({
      acceptedLanguage: "en",
      page: 0,
      size: 2,
    });
    const secondPage = await listPolities({
      acceptedLanguage: "en",
      page: 1,
      size: 2,
    });

    expect(firstPage.content).toHaveLength(2);
    expect(firstPage.page).toEqual({
      number: 0,
      size: 2,
      totalElements: 3,
      totalPages: 2,
    });
    expect(secondPage.content).toHaveLength(1);
    expect(secondPage.page).toEqual({
      number: 1,
      size: 2,
      totalElements: 3,
      totalPages: 2,
    });
  });

  it("paginates the filtered result set", async () => {
    const result = await listPolities({
      acceptedLanguage: "en",
      page: 0,
      query: "  assembly  ",
      size: 1,
    });

    expect(result.content.map(({ name }) => name)).toEqual([
      "The Thursday Assembly",
    ]);
    expect(result.page).toEqual({
      number: 0,
      size: 1,
      totalElements: 1,
      totalPages: 1,
    });
  });

  it("returns backend-shaped availability for the selected polity", async () => {
    const actions = await getPolityActions("neighbourhood-table", {
      acceptedLanguage: "en",
    });

    expect(actions.inviteMembers.available).toBe(true);
    expect(actions.introduceMotion).toMatchObject({
      available: false,
      reason: "procedure_electorate_below_minimum",
    });
    expect(actions.introduceMotion.reasonMessage).toBeTruthy();
  });

  it("composes a workspace from polity, government, action, motion, and record resources", async () => {
    const polity = await getPolity("thursday-assembly", {
      acceptedLanguage: "en",
    });

    expect(polity).toMatchObject({
      memberCount: 8,
      name: "The Thursday Assembly",
      readiness: "ready",
    });
    expect(polity.motions.map(({ id }) => id)).toEqual([
      "shared-dinner",
      "tribune-election",
      "autumn-cabin-budget",
    ]);
    expect(polity.recentActivity[2]).toMatchObject({
      label: "Official record No. 41",
    });
  });

  it("projects motion procedure and certified record data from their owning resources", async () => {
    const motion = await getPolityMotion(
      "thursday-assembly",
      "autumn-cabin-budget",
      { acceptedLanguage: "en" },
    );

    expect(motion.procedure).toEqual({
      electorate: "All 8 active members",
      name: "Ordinary resolution",
      notice: "24 hours",
      threshold: "Simple majority of votes cast",
    });
    expect(motion.result).toEqual({
      no: 2,
      outcome: "Adopted",
      recordEntry: 41,
      yes: 5,
    });
  });

  it("rejects malformed transport responses", async () => {
    apiMockServer.use(
      http.get("/api/v1/polities", () =>
        HttpResponse.json({ content: [], page: { number: "zero" } }),
      ),
    );

    await expect(listPolities({ acceptedLanguage: "en" })).rejects.toThrow(
      "Invalid polity page response.",
    );
  });
});
