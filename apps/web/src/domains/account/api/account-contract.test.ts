import { describe, expect, it } from "vitest";

import { parsePolityAccount } from "@/domains/account/api/account-contract";

const receiptId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
const userId = "99999999-9999-4999-8999-999999999999";

describe("Polity account transport contract", () => {
  it.each([
    "pending",
    "applied",
  ] as const)("projects a valid %s convergence response", (status) => {
    expect(
      parsePolityAccount({
        grants: { failureCode: null, receiptId, status },
        userId,
      }),
    ).toEqual({
      grants: { receiptId, status },
      userId,
    });
  });

  it("requires a stable failure code for failed convergence", () => {
    expect(
      parsePolityAccount({
        grants: {
          failureCode: "grant_application_failed",
          receiptId,
          status: "failed",
        },
        userId,
      }),
    ).toEqual({
      grants: {
        failureCode: "grant_application_failed",
        receiptId,
        status: "failed",
      },
      userId,
    });

    expect(() =>
      parsePolityAccount({
        grants: { failureCode: null, receiptId, status: "failed" },
        userId,
      }),
    ).toThrow("Invalid Polity account response.");
  });

  it("rejects failure data on a non-failed receipt", () => {
    expect(() =>
      parsePolityAccount({
        grants: {
          failureCode: "unexpected_failure",
          receiptId,
          status: "applied",
        },
        userId,
      }),
    ).toThrow("Invalid Polity account response.");
  });
});
