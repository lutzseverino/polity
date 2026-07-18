import { describe, expect, it } from "vitest";

import {
  parseGovernment,
  parseOfficialRecordPage,
} from "@/domains/polity/api/polity-contract";

const polityId = "11111111-1111-4111-8111-111111111111";

describe("polity transport contract", () => {
  it("rejects malformed government fields consumed by the government view", () => {
    expect(() =>
      parseGovernment({
        constitution: {
          body: "Rules",
          id: polityId,
          institutions: [],
          offices: [
            {
              description: "Coordinates the assembly.",
              id: "77777777-7777-4777-8777-777777777777",
              name: "Tribune",
              seatCount: 0,
              termLengthDays: 365,
            },
          ],
          procedures: [],
          ratifiedAt: "2026-01-01T00:00:00Z",
          title: "Constitution",
          version: 1,
        },
        formation: {
          activeMemberCount: 3,
          complete: true,
          minimumFullGovernmentMembers: 3,
          standingMemberCount: 3,
        },
        jurisdictions: [],
      }),
    ).toThrow("Invalid polity government response.");
  });

  it("rejects malformed evidence fields consumed by the record view", () => {
    expect(() =>
      parseOfficialRecordPage({
        content: [
          {
            actorName: "Member",
            body: "Recorded.",
            constitutionVersion: 1,
            entryNumber: 0,
            id: "dddddddd-dddd-4ddd-8ddd-ddddddddddd1",
            occurredAt: "2026-01-01T00:00:00Z",
            title: "Motion recorded",
            type: "motion_introduced",
          },
        ],
        page: {
          number: 0,
          size: 100,
          totalElements: 1,
          totalPages: 1,
        },
      }),
    ).toThrow("Invalid official record response.");
  });

  it("rejects inconsistent page totals before aggregate reads can truncate", () => {
    expect(() =>
      parseOfficialRecordPage({
        content: [],
        page: {
          number: 0,
          size: 100,
          totalElements: 101,
          totalPages: 1,
        },
      }),
    ).toThrow("Invalid official record page response.");
  });
});
