import { describe, expect, it } from "vitest";

import {
  getPolityActions,
  listPolities,
} from "@/domains/polity/api/polity-requests";

describe("polity requests", () => {
  it("returns zero-based pages with stable metadata", async () => {
    const firstPage = await listPolities({ page: 0, size: 2 });
    const secondPage = await listPolities({ page: 1, size: 2 });

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
    const actions = await getPolityActions("neighbourhood-table");

    expect(actions.inviteMembers.available).toBe(true);
    expect(actions.introduceMotion).toMatchObject({
      available: false,
      reason: "procedure_electorate_below_minimum",
    });
    expect(actions.introduceMotion.reasonMessage).toBeTruthy();
  });
});
