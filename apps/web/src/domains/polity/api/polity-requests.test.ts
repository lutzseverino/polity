import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";

import {
  getPolity,
  getPolityActions,
  getPolityGovernment,
  getPolityMotion,
  getPolityOfficialRecord,
  listAllPolities,
  listPolities,
  listPolityMotionResponses,
} from "@/domains/polity/api/polity-requests";
import { apiMockServer } from "@/test/mocks/server";

describe("polity requests", () => {
  const uuid = (index: number) =>
    `00000000-0000-4000-8000-${String(index).padStart(12, "0")}`;

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
    const actions = await getPolityActions(
      "22222222-2222-4222-8222-222222222222",
      {
        acceptedLanguage: "en",
      },
    );

    expect(actions.inviteMembers.available).toBe(true);
    expect(actions.introduceMotion).toMatchObject({
      available: false,
      reason: "procedure_electorate_below_minimum",
    });
    expect(actions.introduceMotion.reasonMessage).toBeTruthy();
  });

  it("composes a workspace from polity, government, action, motion, and record resources", async () => {
    const polity = await getPolity("11111111-1111-4111-8111-111111111111", {
      acceptedLanguage: "en",
    });

    expect(polity).toMatchObject({
      memberCount: 8,
      name: "The Thursday Assembly",
      readiness: "ready",
    });
    expect(polity.motions.map(({ id }) => id)).toEqual([
      "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
      "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2",
      "cccccccc-cccc-4ccc-8ccc-ccccccccccc3",
    ]);
    expect(polity.recentActivity[2]).toMatchObject({
      label: "Official record No. 41",
    });
  });

  it("projects motion procedure and certified record data from their owning resources", async () => {
    const motion = await getPolityMotion(
      "11111111-1111-4111-8111-111111111111",
      "cccccccc-cccc-4ccc-8ccc-ccccccccccc3",
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

  it("projects a government view from government and action resources", async () => {
    const government = await getPolityGovernment(
      "11111111-1111-4111-8111-111111111111",
      { acceptedLanguage: "en" },
    );

    expect(government).toMatchObject({
      constitution: { title: "Constitution", version: 2 },
      formation: { activeMemberCount: 8, complete: true },
      health: { status: "healthy" },
      readiness: { status: "ready" },
    });
    expect(government.institutions[0]).toMatchObject({
      kind: "assembly",
      name: "Assembly",
    });
    expect(government.offices[0]).toMatchObject({
      name: "Tribune",
      seatCount: 1,
    });
  });

  it("projects official-record evidence without dropping its owner fields", async () => {
    const entries = await getPolityOfficialRecord(
      "11111111-1111-4111-8111-111111111111",
      { acceptedLanguage: "en" },
    );

    expect(entries[0]).toMatchObject({
      actorName: "Mira Chen",
      body: "Voting opened.",
      constitutionVersion: 2,
      entryNumber: 43,
      motionId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
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

  it.each([
    { field: "number", value: -1 },
    { field: "size", value: 0 },
    { field: "totalElements", value: 1.5 },
    { field: "totalPages", value: Number.MAX_SAFE_INTEGER + 1 },
  ])("rejects invalid page metadata: $field=$value", async ({
    field,
    value,
  }) => {
    apiMockServer.use(
      http.get("/api/v1/polities", () =>
        HttpResponse.json({
          content: [],
          page: {
            number: 0,
            size: 50,
            totalElements: 0,
            totalPages: 0,
            [field]: value,
          },
        }),
      ),
    );

    await expect(listPolities({ acceptedLanguage: "en" })).rejects.toThrow(
      "Invalid polity page response.",
    );
  });

  it.each([
    {
      createdAt: "2026-01-01T00:00:00.000Z",
      id: "not-a-uuid",
      label: "UUID",
    },
    {
      createdAt: "2025-02-30T12:00:00Z",
      id: "11111111-1111-4111-8111-111111111111",
      label: "impossible RFC 3339 date-time",
    },
  ])("rejects an invalid $label before projection", async ({
    createdAt,
    id,
  }) => {
    apiMockServer.use(
      http.get("/api/v1/polities", () =>
        HttpResponse.json({
          content: [
            {
              constitutionVersion: 1,
              createdAt,
              id,
              institutionName: "Assembly",
              jurisdictionName: "Example",
              name: "Example",
              status: "active",
              visibility: "public",
            },
          ],
          page: {
            number: 0,
            size: 50,
            totalElements: 1,
            totalPages: 1,
          },
        }),
      ),
    );

    await expect(listPolities({ acceptedLanguage: "en" })).rejects.toThrow(
      "Invalid polity response.",
    );
  });

  it("loads every polity and motion page for aggregate consumers", async () => {
    const polityResponses = Array.from({ length: 101 }, (_, index) => ({
      constitutionVersion: 1,
      createdAt: "2026-01-01T00:00:00.000Z",
      id: uuid(index + 1),
      institutionName: "Assembly",
      jurisdictionName: `Polity ${index + 1}`,
      name: `Polity ${index + 1}`,
      status: "active",
      visibility: "public",
    }));
    const motionResponses = Array.from({ length: 101 }, (_, index) => ({
      actions: {
        castElectionBallot: { available: false },
        castVote: { available: true },
        requestCertification: { available: false },
        respondCandidacy: { available: false },
      },
      body: "Motion body",
      certificationOpensAt: "2026-01-03T00:00:00.000Z",
      constitutionVersion: 1,
      effectType: "adopt_resolution",
      id: uuid(index + 201),
      introducedByName: "Member",
      openedAt: "2026-01-01T00:00:00.000Z",
      procedureName: "Ordinary resolution",
      status: "voting",
      title: `Motion ${index + 1}`,
      votingClosesAt: "2026-01-02T00:00:00.000Z",
      votingOpensAt: "2026-01-01T00:00:00.000Z",
    }));
    const recordResponses = Array.from({ length: 101 }, (_, index) => ({
      actorName: "Member",
      body: "Record body",
      constitutionVersion: 1,
      entryNumber: index + 1,
      id: uuid(index + 401),
      occurredAt: "2026-01-01T00:00:00.000Z",
      title: `Record ${index + 1}`,
      type: "motion_introduced",
    }));
    apiMockServer.use(
      http.get("/api/v1/polities", ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get("page") ?? 0);
        const content = polityResponses.slice(page * 100, page * 100 + 100);
        return HttpResponse.json({
          content,
          page: {
            number: page,
            size: 100,
            totalElements: 101,
            totalPages: 2,
          },
        });
      }),
      http.get("/api/v1/polities/:polityId/motions", ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get("page") ?? 0);
        const content = motionResponses.slice(page * 100, page * 100 + 100);
        return HttpResponse.json({
          content,
          page: {
            number: page,
            size: 100,
            totalElements: 101,
            totalPages: 2,
          },
        });
      }),
      http.get("/api/v1/polities/:polityId/record", ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get("page") ?? 0);
        const content = recordResponses.slice(page * 100, page * 100 + 100);
        return HttpResponse.json({
          content,
          page: {
            number: page,
            size: 100,
            totalElements: 101,
            totalPages: 2,
          },
        });
      }),
    );

    await expect(
      listAllPolities({ acceptedLanguage: "en" }),
    ).resolves.toHaveLength(101);
    await expect(
      listPolityMotionResponses(uuid(1), { acceptedLanguage: "en" }),
    ).resolves.toHaveLength(101);
    await expect(
      getPolityOfficialRecord(uuid(1), { acceptedLanguage: "en" }),
    ).resolves.toHaveLength(101);
  });

  it("rejects a short later record page instead of silently truncating", async () => {
    const records = Array.from({ length: 100 }, (_, index) => ({
      actorName: "Member",
      body: "Record body",
      constitutionVersion: 1,
      entryNumber: index + 1,
      id: uuid(index + 501),
      occurredAt: "2026-01-01T00:00:00.000Z",
      title: `Record ${index + 1}`,
      type: "motion_introduced",
    }));
    apiMockServer.use(
      http.get("/api/v1/polities/:polityId/record", ({ request }) => {
        const pageNumber = Number(
          new URL(request.url).searchParams.get("page") ?? 0,
        );
        return HttpResponse.json({
          content: pageNumber === 0 ? records : [],
          page: {
            number: pageNumber,
            size: 100,
            totalElements: 101,
            totalPages: 2,
          },
        });
      }),
    );

    await expect(
      getPolityOfficialRecord(uuid(1), { acceptedLanguage: "en" }),
    ).rejects.toThrow("Invalid official record page response.");
  });
});
