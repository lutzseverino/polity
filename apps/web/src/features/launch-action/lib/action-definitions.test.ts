import { describe, expect, it } from "vitest";

import { filterActionDefinitions } from "@/features/launch-action/lib/action-definitions";

const translate = (message: { message?: string }) => message.message ?? "";

describe("action definitions", () => {
  it("maps every backend-supported polity-level capability exactly once", () => {
    const actions = filterActionDefinitions("", translate);

    expect(actions.map((action) => action.availabilityKey).sort()).toEqual([
      "introduceAmendment",
      "introduceAppeal",
      "introduceConstitutionalReview",
      "introduceDisbandment",
      "introduceMotion",
      "introduceOfficeElection",
      "introduceOfficeTermReview",
      "introduceSanction",
      "inviteMembers",
      "requestCertification",
      "resignMembership",
    ]);
  });

  it.each([
    ["change our rules", "amend-constitution"],
    ["invite someone", "invite-member"],
    ["invite person", "invite-member"],
    ["leave this polity", "resign-membership"],
    ["remove an officer", "review-office-term"],
    ["someone broke the rules", "propose-sanction"],
    ["void a decision", "request-review"],
  ])("finds the formal action for the plain-language goal %s", (query, id) => {
    expect(
      filterActionDefinitions(query, translate).map((action) => action.id),
    ).toContain(id);
  });
});
